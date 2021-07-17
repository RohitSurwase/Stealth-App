package com.cosmos.unreddit.util.extension

import android.app.Notification
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

fun Context.createNotificationChannel(
    channelId: String,
    @StringRes name: Int,
    @StringRes description: Int,
    importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT
) {
    createNotificationChannel(
        channelId,
        getString(name),
        getString(description),
        importance
    )
}

fun Context.createNotificationChannel(
    channelId: String,
    name: String,
    description: String,
    importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT
) {
    val channel = NotificationChannelCompat.Builder(channelId, importance)
        .setName(name)
        .setDescription(description)
        .build()
    NotificationManagerCompat.from(this).createNotificationChannel(channel)
}

fun Context.showNotification(notificationId: Int, notification: Notification) {
    NotificationManagerCompat.from(this).notify(notificationId, notification)
}

fun Context.cancelNotification(notificationId: Int) {
    NotificationManagerCompat.from(this).cancel(notificationId)
}
