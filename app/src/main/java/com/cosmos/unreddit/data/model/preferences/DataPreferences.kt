package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

data class DataPreferences(
    val redditSource: Int,
    val enablePrivacyEnhancer: Boolean
) {
    object PreferencesKeys {
        val REDDIT_SOURCE = intPreferencesKey("reddit_source")
        val PRIVACY_ENHANCER = booleanPreferencesKey("privacy_enhancer")
    }

    enum class RedditSource(val value: Int) {
        REDDIT(0), TEDDIT(1);

        companion object {
            fun fromValue(value: Int): RedditSource = values().find { it.value == value } ?: REDDIT
        }
    }
}
