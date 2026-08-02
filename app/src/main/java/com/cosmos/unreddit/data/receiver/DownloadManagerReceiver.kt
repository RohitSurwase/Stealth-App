package com.cosmos.unreddit.data.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cosmos.unreddit.BuildConfig
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.worker.MediaDownloadWorker
import com.cosmos.unreddit.util.IntentUtil

class DownloadManagerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.extras?.getString(KEY_URL) ?: return
        // Only v.reddit videos carry a separate audio track, so this is expected to be null
        val sound = intent.extras?.getString(KEY_SOUND)
        val author = intent.extras?.getString(KEY_AUTHOR)

        when (intent.action) {
            ACTION_DOWNLOAD_RETRY -> {
                val mediaType = intent.extras?.getInt(KEY_TYPE, -1)?.let {
                    GalleryMedia.Type.fromValue(it)
                } ?: return

                retry(context, url, mediaType, sound, author)
            }
            ACTION_DOWNLOAD_CANCEL -> cancel(context)
        }
    }

    private fun retry(
        context: Context,
        url: String,
        type: GalleryMedia.Type,
        sound: String?,
        author: String?
    ) {
        MediaDownloadWorker.enqueueWork(context, url, type, sound, author)
    }

    private fun cancel(context: Context) {
        // TODO: Cancel unique work instead of by tag
        MediaDownloadWorker.cancelWork(context)
    }

    companion object {
        private const val ACTION_DOWNLOAD_RETRY =
            "${BuildConfig.APPLICATION_ID}.ACTION_DOWNLOAD_RETRY"
        private const val ACTION_DOWNLOAD_CANCEL =
            "${BuildConfig.APPLICATION_ID}.ACTION_DOWNLOAD_CANCEL"

        private const val KEY_URL = "KEY_URL"
        private const val KEY_TYPE = "KEY_TYPE"
        private const val KEY_SOUND = "KEY_SOUND"
        private const val KEY_AUTHOR = "KEY_AUTHOR"

        /**
         * [Intent.filterEquals] ignores the extras, so every download produces an intent that
         * matches the previous one. Without [PendingIntent.FLAG_UPDATE_CURRENT], the system would
         * hand back the pending intent of an earlier download and the button would act on a stale
         * URL.
         */
        private fun getPendingIntentFlag(): Int {
            return IntentUtil.getPendingIntentFlag(false) or PendingIntent.FLAG_UPDATE_CURRENT
        }

        fun getRetryPendingIntent(
            context: Context,
            url: String,
            type: GalleryMedia.Type,
            sound: String?,
            author: String?
        ): PendingIntent {
            val intent = Intent(context, DownloadManagerReceiver::class.java).apply {
                action = ACTION_DOWNLOAD_RETRY
                putExtra(KEY_URL, url)
                putExtra(KEY_TYPE, type.value)
                putExtra(KEY_SOUND, sound)
                putExtra(KEY_AUTHOR, author)
            }

            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                getPendingIntentFlag()
            )
        }

        fun getCancelPendingIntent(
            context: Context,
            url: String
        ): PendingIntent {
            val intent = Intent(context, DownloadManagerReceiver::class.java).apply {
                action = ACTION_DOWNLOAD_CANCEL
                putExtra(KEY_URL, url)
            }

            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                getPendingIntentFlag()
            )
        }
    }
}
