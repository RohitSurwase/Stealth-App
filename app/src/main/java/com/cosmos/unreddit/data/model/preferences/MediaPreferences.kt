package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

data class MediaPreferences(
    val muteVideo: Boolean,

    val downloadFilenameAuthor: Boolean
) {
    object PreferencesKeys {
        val MUTE_VIDEO = booleanPreferencesKey("mute_video")
        val DOWNLOAD_FILENAME_AUTHOR = booleanPreferencesKey("download_filename_author")
    }
}
