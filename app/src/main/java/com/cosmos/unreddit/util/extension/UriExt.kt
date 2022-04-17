package com.cosmos.unreddit.util.extension

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun Uri.getFilename(context: Context): String? {
    return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.run {
            moveToFirst()
            getString(nameIndex)
        }
    }
}
