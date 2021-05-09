package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.intPreferencesKey

data class ProfilePreferences(
    val currentProfile: Int
) {
    object PreferencesKeys {
        val CURRENT_PROFILE = intPreferencesKey("current_profile")
    }
}
