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

        when (intent.action) {
            ACTION_DOWNLOAD_RETRY -> {
                val mediaType = intent.extras?.getInt(KEY_TYPE, -1)?.let {
                    GalleryMedia.Type.fromValue(it)
                } ?: return

                retry(context, url, mediaType)
            }
            ACTION_DOWNLOAD_CANCEL -> cancel(context)
        }
    }

    private fun retry(context: Context, url: String, type: GalleryMedia.Type) {
        MediaDownloadWorker.enqueueWork(context, url, type)
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

        fun getRetryPendingIntent(
            context: Context,
            url: String,
            type: GalleryMedia.Type
        ): PendingIntent {
            val intent = Intent(context, DownloadManagerReceiver::class.java).apply {
                action = ACTION_DOWNLOAD_RETRY
                putExtra(KEY_URL, url)
                putExtra(KEY_TYPE, type.value)
            }

            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                IntentUtil.getPendingIntentFlag(false)
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
                IntentUtil.getPendingIntentFlag(false)
            )
        }
    }
}
