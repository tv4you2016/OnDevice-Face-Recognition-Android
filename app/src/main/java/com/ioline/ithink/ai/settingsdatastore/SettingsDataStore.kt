package com.ioline.ithink.ai.settingsdatastore

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ioline.ithink.ai.AutoDismissDialog
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

val Context.settingsDataStore by preferencesDataStore("full_settings")


class SettingsDataStore(private val context: Context) {

    private val SETTINGS_KEY = stringPreferencesKey("settings_json")
    private val jsonFormatter = Json { ignoreUnknownKeys = true }

    val settingsFlow: Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->
            val json = prefs[SETTINGS_KEY]
            if (json == null) AppSettings()
            else jsonFormatter.decodeFromString<AppSettings>(json)
        }

    suspend fun saveSettings(settings: AppSettings) {
        val json = jsonFormatter.encodeToString(settings)

        context.settingsDataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = json
        }


    }
}
