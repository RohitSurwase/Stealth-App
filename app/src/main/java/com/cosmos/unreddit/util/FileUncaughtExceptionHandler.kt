package com.cosmos.unreddit.util

import android.content.Context
import com.cosmos.unreddit.BuildConfig
import com.cosmos.unreddit.R
import java.io.File
import java.util.Date
import kotlin.system.exitProcess

class FileUncaughtExceptionHandler(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val filename: String
        get() = context.getString(R.string.app_name) +
                "_" +
                BuildConfig.VERSION_NAME +
                "_" +
                DateUtil.getFormattedDate(context.getString(R.string.file_date_format), Date()) +
                ".log"

    override fun uncaughtException(thrad: Thread, throwable: Throwable) {
        val uncaughtFolder = File(
            context.getExternalFilesDir(null),
            UNCAUGHT_FOLDER_NAME
        ).also { uncaughtFolder ->
            uncaughtFolder.mkdir()
        }

        throwable.printStackTrace()

        File(uncaughtFolder, filename).printWriter().use { pw ->
            throwable.printStackTrace(pw)
        }

        exitProcess(0)
    }

    companion object {
        private const val UNCAUGHT_FOLDER_NAME = "uncaught"
    }
}
