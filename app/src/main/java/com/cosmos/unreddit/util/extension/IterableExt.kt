package com.cosmos.unreddit.util.extension

fun <T> Iterable<Iterable<T>>.interlace(): List<T> {
    val result = ArrayList<T>()

    val max = this.maxOf { it.count() }

    for (i in 0..max) {
        this
            .mapNotNull { it.elementAtOrNull(i) }
            .let { result.addAll(it) }
    }

    return result
}
