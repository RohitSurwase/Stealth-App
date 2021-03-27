package com.cosmos.unreddit.util.extension

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

suspend fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T) {
    edit { preferences ->
        preferences[key] = value
    }
}

fun <T> DataStore<Preferences>.getValue(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
    return data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        preferences[key] ?: defaultValue
    }
}
