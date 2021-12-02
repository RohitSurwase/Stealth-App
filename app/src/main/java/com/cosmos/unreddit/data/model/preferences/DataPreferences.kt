package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.intPreferencesKey

data class DataPreferences(
    val redditSource: Int
) {
    object PreferencesKeys {
        val REDDIT_SOURCE = intPreferencesKey("reddit_source")
    }
}
