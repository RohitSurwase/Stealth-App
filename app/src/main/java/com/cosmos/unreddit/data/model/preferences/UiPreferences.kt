package com.cosmos.unreddit.data.model.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.intPreferencesKey

data class UiPreferences(
    val nightMode: Int
) {
    object PreferencesKeys {
        val NIGHT_MODE = intPreferencesKey("night_mode")
    }

    enum class NightMode(val index: Int, val mode: Int) {
        LIGHT(0, AppCompatDelegate.MODE_NIGHT_NO),
        DARK(1, AppCompatDelegate.MODE_NIGHT_YES),
        FOLLOW_SYSTEM(2, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        companion object {
            fun asIndex(nightMode: Int): Int? {
                return values().find { it.mode == nightMode }?.index
            }

            fun asMode(index: Int): Int? {
                return values().find { it.index == index }?.mode
            }
        }
    }
}
