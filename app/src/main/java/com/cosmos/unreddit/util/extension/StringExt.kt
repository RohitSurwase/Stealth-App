package com.cosmos.unreddit.util.extension

import android.webkit.MimeTypeMap
import java.util.Locale

val String.extension: String
    get() = MimeTypeMap.getFileExtensionFromUrl(this)

val String.mimeType: String
    get() = MimeTypeMap.getSingleton().getMimeTypeFromUrl(this) ?: ""

val String.sum: Int
    get() = this.sumOf { it.code }

val String.titlecase: String
    get() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
