package com.cosmos.unreddit.data.model.db

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.room.Entity
import com.cosmos.unreddit.R
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "redirect",
    primaryKeys = ["service"]
)
data class Redirect(
    var pattern: String,

    var redirect: String,

    var service: String,

    var mode: RedirectMode
) : Parcelable {
    enum class RedirectMode(val mode: Int, @StringRes val label: Int) {
        ON(0, R.string.redirect_mode_on),
        OFF(1, R.string.redirect_mode_off),
        ALWAYS_ASK(2, R.string.redirect_mode_ask);

        val isEnabled: Boolean
            get() = this != OFF

        companion object {
            fun toMode(mode: Int): RedirectMode = values().find { it.mode == mode } ?: OFF
        }
    }
}
