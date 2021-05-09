package com.cosmos.unreddit.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.model.preferences.MediaPreferences
import com.cosmos.unreddit.data.model.preferences.ProfilePreferences
import com.cosmos.unreddit.data.model.preferences.UiPreferences
import com.cosmos.unreddit.util.extension.getValue
import com.cosmos.unreddit.util.extension.setValue
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

    //region Ui

    suspend fun setNightMode(nightMode: Int) {
        preferencesDatastore.setValue(UiPreferences.PreferencesKeys.NIGHT_MODE, nightMode)
    }

    fun getNightMode(defaultValue: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM): Flow<Int> {
        return preferencesDatastore.getValue(UiPreferences.PreferencesKeys.NIGHT_MODE, defaultValue)
    }

    //endregion

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

    //region Profile

    fun getCurrentProfile(): Flow<Int> {
        return preferencesDatastore.getValue(
            ProfilePreferences.PreferencesKeys.CURRENT_PROFILE,
            -1
        )
    }

    suspend fun setCurrentProfile(profileId: Int) {
        preferencesDatastore.setValue(ProfilePreferences.PreferencesKeys.CURRENT_PROFILE, profileId)
    }

    //endregion

    //region Media

    fun getMuteVideo(defaultValue: Boolean): Flow<Boolean> {
        return preferencesDatastore.getValue(
            MediaPreferences.PreferencesKeys.MUTE_VIDEO,
            defaultValue
        )
    }

    suspend fun setMuteVideo(muteVideo: Boolean) {
        preferencesDatastore.setValue(MediaPreferences.PreferencesKeys.MUTE_VIDEO, muteVideo)
    }

    //endregion
}
