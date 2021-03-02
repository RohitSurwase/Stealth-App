package com.cosmos.unreddit.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.cosmos.unreddit.preferences.ContentPreferences
import com.cosmos.unreddit.util.getValue
import com.cosmos.unreddit.util.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesDatastore: DataStore<Preferences>
) {

    //region Content

    suspend fun setShowNsfw(showNsfw: Boolean) {
        preferencesDatastore.setValue(ContentPreferences.PreferencesKeys.SHOW_NSFW, showNsfw)
    }

    fun getShowNsfw(defaultValue: Boolean = false): Flow<Boolean> {
        return preferencesDatastore.getValue(
            ContentPreferences.PreferencesKeys.SHOW_NSFW,
            defaultValue
        )
    }

    suspend fun setShowNsfwPreview(showNsfwPreview: Boolean) {
        preferencesDatastore.setValue(
            ContentPreferences.PreferencesKeys.SHOW_NSFW_PREVIEW,
            showNsfwPreview
        )
    }

    fun getShowNsfwPreview(defaultValue: Boolean = false): Flow<Boolean> {
        return preferencesDatastore.getValue(
            ContentPreferences.PreferencesKeys.SHOW_NSFW_PREVIEW,
            defaultValue
        )
    }

    suspend fun setShowSpoilerPreview(showSpoilerPreview: Boolean) {
        preferencesDatastore.setValue(
            ContentPreferences.PreferencesKeys.SHOW_SPOILER_PREVIEW,
            showSpoilerPreview
        )
    }

    fun getShowSpoilerPreview(defaultValue: Boolean = false): Flow<Boolean> {
        return preferencesDatastore.getValue(
            ContentPreferences.PreferencesKeys.SHOW_SPOILER_PREVIEW,
            defaultValue
        )
    }

    fun getContentPreferences(): Flow<ContentPreferences> {
        return preferencesDatastore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val showNsfw = preferences[ContentPreferences.PreferencesKeys.SHOW_NSFW] ?: false
            val showNsfwPreview =
                preferences[ContentPreferences.PreferencesKeys.SHOW_NSFW_PREVIEW] ?: false
            val showSpoilerPreview =
                preferences[ContentPreferences.PreferencesKeys.SHOW_SPOILER_PREVIEW] ?: false
            ContentPreferences(showNsfw, showNsfwPreview, showSpoilerPreview)
        }
    }

    //endregion
}
