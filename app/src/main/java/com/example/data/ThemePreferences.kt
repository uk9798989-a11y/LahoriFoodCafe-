package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "lahori_theme_prefs")

object ThemePreferences {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")

    fun observeDarkMode(context: Context): Flow<Boolean> =
        context.applicationContext.themeDataStore.data.map { prefs ->
            prefs[DARK_MODE_KEY] ?: false
        }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.applicationContext.themeDataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }
}
