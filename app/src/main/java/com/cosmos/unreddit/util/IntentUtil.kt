package com.cosmos.unreddit.util

import android.app.PendingIntent
import android.os.Build

object IntentUtil {

    fun getPendingIntentFlag(mutable: Boolean): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (mutable) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_IMMUTABLE
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PendingIntent.FLAG_IMMUTABLE
            else -> 0
        }
    }
}
