package com.cosmos.unreddit.util.extension

val Long.isPast: Boolean
    get() = System.currentTimeMillis() >= this
