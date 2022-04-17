package com.cosmos.unreddit.data.model.backup

import androidx.annotation.DrawableRes
import com.cosmos.unreddit.R

enum class BackupType(
    val displayName: String,
    @DrawableRes val icon: Int,
    val mime: Array<String>
) {
    STEALTH("Stealth", R.drawable.ic_stealth, arrayOf("application/json")),
    REDDIT("Reddit", R.drawable.ic_reddit, arrayOf("application/json"))
}
