package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

data class ContentPreferences(
    val showNsfw: Boolean,

    val showNsfwPreview: Boolean,

    val showSpoilerPreview: Boolean
) {
    object PreferencesKeys {
        val SHOW_NSFW = booleanPreferencesKey("show_nsfw")
        val SHOW_NSFW_PREVIEW = booleanPreferencesKey("show_nsfw_preview")
        val SHOW_SPOILER_PREVIEW = booleanPreferencesKey("show_spoiler_preview")
    }
}
