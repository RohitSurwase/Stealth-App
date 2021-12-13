package com.cosmos.unreddit.data.worker

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cosmos.unreddit.BuildConfig
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.receiver.DownloadManagerReceiver
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.util.DateUtil
import com.cosmos.unreddit.util.IntentUtil
import com.cosmos.unreddit.util.extension.cancelAllWorkByTag
import com.cosmos.unreddit.util.extension.cancelNotification
import com.cosmos.unreddit.util.extension.createNotificationChannel
import com.cosmos.unreddit.util.extension.enqueueUniqueWork
import com.cosmos.unreddit.util.extension.showNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.util.Date

@HiltWorker
class MediaDownloadWorker @AssistedInject constructor (
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(appContext, params) {

    private val filename: String
        get() = applicationContext.getString(R.string.app_name) +
            "_" +
            DateUtil.getFormattedDate(
                applicationContext.getString(R.string.file_date_format),
                Date()
            )

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val type = inputData.getInt(KEY_TYPE, -1).let {
            GalleryMedia.Type.fromValue(it)
        } ?: return Result.failure()

        val builder = createDownloadManagerBuilder()
            .setProgress(0, 0, true)
            .addAction(getCancelAction(url))

        applicationContext.showNotification(NOTIFICATION_ID, builder.build())

        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
        val name = "$filename.$extension"

        val uri = withContext(NonCancellable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadMedia(url, type, name, mimeType)
            } else {
                downloadMediaLegacy(url, type, name, mimeType)
            }
        }

        builder
            .setProgress(0, 0, false)
            .clearActions()

        return when {
            isStopped -> {
                applicationContext.cancelNotification(NOTIFICATION_ID)

                Result.success()
            }
            uri != null -> {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    IntentUtil.getPendingIntentFlag(false)
                )

                builder
                    .setContentText(
                        applicationContext.getString(R.string.notification_download_content_success)
                    )
                    .setContentIntent(pendingIntent)

                if (type == GalleryMedia.Type.IMAGE) {
                    val bitmap = getBitmap(uri)
                    builder
                        .setLargeIcon(bitmap)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null)
                        )
                }

                applicationContext.showNotification(NOTIFICATION_ID, builder.build())

                Result.success()
            }
            else -> {
                builder
                    .setContentText(
                        applicationContext.getString(R.string.notification_download_content_failed)
                    )
                    .addAction(getRetryAction(url, type))

                applicationContext.showNotification(NOTIFICATION_ID, builder.build())

                Result.failure()
            }
        }
    }

    /**
     * @see <a href="https://commonsware.com/blog/2019/12/21/scoped-storage-stories-storing-mediastore.html">Scoped Storage Stories: Storing via MediaStore </a>
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadMedia(
        url: String,
        type: GalleryMedia.Type,
        name: String,
        mimeType: String
    ): Uri? {
        var uri: Uri? = null

        val collection = when (type) {
            GalleryMedia.Type.IMAGE -> {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            GalleryMedia.Type.VIDEO -> {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
        }

        withContext(ioDispatcher) {
            runCatching {
                val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()

                if (response.isSuccessful) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val resolver = applicationContext.contentResolver
                    uri = resolver.insert(collection, values)

                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            val sink = outputStream.sink().buffer()

                            response.body()?.source()?.let { source ->
                                sink.writeAllWhileActive(source)
                            }

                            sink.close()
                        }

                        if (!isStopped) {
                            values.clear()
                            values.put(MediaStore.Video.Media.IS_PENDING, 0)

                            resolver.update(it, values, null, null)
                        } else {
                            resolver.delete(it, null, null)
                        }
                    }
                }
            }.onFailure {
                uri = null
            }
        }

        return uri
    }

    /**
     * @see <a href="https://commonsware.com/blog/2019/12/21/scoped-storage-stories-storing-mediastore.html">Scoped Storage Stories: Storing via MediaStore </a>
     */
    @Suppress("deprecation")
    private suspend fun downloadMediaLegacy(
        url: String,
        type: GalleryMedia.Type,
        name: String,
        mimeType: String
    ): Uri? {
        var uri: Uri? = null

        val publicDirectory = when (type) {
            GalleryMedia.Type.IMAGE -> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            GalleryMedia.Type.VIDEO -> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            }
        }

        val file = File(publicDirectory, name)

        withContext(ioDispatcher) {
            runCatching {
                val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()

                if (response.isSuccessful) {
                    val sink = file.sink().buffer()

                    response.body()?.source()?.let { source ->
                        sink.writeAllWhileActive(source)
                    }

                    sink.close()

                    if (!isStopped) {
                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(file.absolutePath),
                            arrayOf(mimeType),
                            null
                        )

                        uri = Uri.fromFile(file)
                    } else {
                        file.delete()
                    }
                }
            }.onFailure {
                uri = null
            }
        }

        return uri
    }

    /**
     * @see [BufferedSink.writeAll]
     */
    private fun BufferedSink.writeAllWhileActive(source: BufferedSource): Long {
        var totalBytesRead = 0L
        while (!isStopped) {
            val readCount = source.read(buffer, 8192) // Segment.SIZE
            if (readCount == -1L) break
            totalBytesRead += readCount
            emitCompleteSegments()
        }
        return totalBytesRead
    }

    @Suppress("deprecation")
    private fun getBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(applicationContext.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri)
        }
    }

    private fun createDownloadManagerBuilder(): NotificationCompat.Builder {
        createDownloadManagerChannel()
        return NotificationCompat.Builder(applicationContext, DOWNLOAD_MANAGER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stealth)
            .setContentTitle(applicationContext.getString(R.string.notification_download_title))
            .setContentText(
                applicationContext.getString(R.string.notification_download_content_pending)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

    private fun createDownloadManagerChannel() {
        applicationContext.createNotificationChannel(
            DOWNLOAD_MANAGER_CHANNEL_ID,
            R.string.notification_download_channel_name,
            R.string.notification_download_channel_description,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
    }

    private fun getRetryAction(url: String, type: GalleryMedia.Type): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            applicationContext.getString(R.string.notification_download_action_retry),
            DownloadManagerReceiver.getRetryPendingIntent(applicationContext, url, type)
        ).build()
    }

    private fun getCancelAction(url: String): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            null,
            applicationContext.getString(R.string.notification_download_action_cancel),
            DownloadManagerReceiver.getCancelPendingIntent(applicationContext, url)
        ).build()
    }

    companion object {
        private const val DOWNLOAD_MANAGER_CHANNEL_ID =
            "${BuildConfig.APPLICATION_ID}.DOWNLOAD_MANAGER_CHANNEL"

        private const val WORK_TAG = "MediaDownloadWorker"

        private const val NOTIFICATION_ID = 856

        private const val KEY_URL = "KEY_URL"
        private const val KEY_TYPE = "KEY_TYPE"

        fun enqueueWork(context: Context, url: String, type: GalleryMedia.Type) {
            val downloadRequest = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
                .addTag(WORK_TAG)
                .setInputData(
                    workDataOf(
                        KEY_URL to url,
                        KEY_TYPE to type.value
                    )
                )
                .build()

            context.enqueueUniqueWork(url, ExistingWorkPolicy.APPEND_OR_REPLACE, downloadRequest)
        }

        fun cancelWork(context: Context) {
            context.cancelAllWorkByTag(WORK_TAG)
        }
    }
}
