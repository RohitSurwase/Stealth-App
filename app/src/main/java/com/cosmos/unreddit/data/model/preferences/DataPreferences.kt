package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.intPreferencesKey

data class DataPreferences(
    val redditSource: Int
) {
    object PreferencesKeys {
        val REDDIT_SOURCE = intPreferencesKey("reddit_source")
    }

    enum class RedditSource(val value: Int) {
        REDDIT(0), TEDDIT(1);

        companion object {
            fun fromValue(value: Int): RedditSource = values().find { it.value == value } ?: REDDIT
        }
    }
}
