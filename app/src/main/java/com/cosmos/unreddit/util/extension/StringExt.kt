package com.cosmos.unreddit.util.extension

import android.webkit.MimeTypeMap

val String.mimeType: String
    get() = MimeTypeMap.getSingleton().getMimeTypeFromUrl(this) ?: ""
