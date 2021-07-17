package com.cosmos.unreddit.util.extension

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

fun Context.enqueueUniqueWork(
    uniqueName: String,
    existingWorkPolicy: ExistingWorkPolicy,
    work: OneTimeWorkRequest
) {
    WorkManager
        .getInstance(this)
        .enqueueUniqueWork(uniqueName, existingWorkPolicy, work)
}

fun Context.cancelUniqueWork(uniqueWorkName: String) {
    WorkManager
        .getInstance(this)
        .cancelUniqueWork(uniqueWorkName)
}

fun Context.cancelAllWorkByTag(tag: String) {
    WorkManager
        .getInstance(this)
        .cancelAllWorkByTag(tag)
}
