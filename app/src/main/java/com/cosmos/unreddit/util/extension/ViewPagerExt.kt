package com.cosmos.unreddit.util.extension

import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.previousPage() {
    currentItem -= 1
}

fun ViewPager2.nextPage() {
    currentItem += 1
}
