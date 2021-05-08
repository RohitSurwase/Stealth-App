package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

data class MediaPreferences(
    val muteVideo: Boolean
) {
    object PreferencesKeys {
        val MUTE_VIDEO = booleanPreferencesKey("mute_video")
    }
}
