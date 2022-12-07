package com.cosmos.unreddit.data.model.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class DataPreferences(
    val redditSource: Int,
    val redditSourceInstance: String,
    val enablePrivacyEnhancer: Boolean
) {
    object PreferencesKeys {
        val REDDIT_SOURCE = intPreferencesKey("reddit_source")
        val REDDIT_SOURCE_INSTANCE = stringPreferencesKey("reddit_source_instance")
        val PRIVACY_ENHANCER = booleanPreferencesKey("privacy_enhancer")
    }

    enum class RedditSource(val value: Int) {
        REDDIT(0), TEDDIT(1);

        companion object {
            fun fromValue(value: Int): RedditSource = values().find { it.value == value } ?: REDDIT
        }
    }
}
