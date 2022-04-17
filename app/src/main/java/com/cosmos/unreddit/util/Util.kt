package com.cosmos.unreddit.util

object Util {

    fun <T1, T2> let(p1: T1?, p2: T2?, block: (T1, T2) -> Unit) {
        if (p1 != null && p2 != null) {
            block.invoke(p1, p2)
        }
    }
}
