package io.github.filipembcruz.banca.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "banca_settings")

/** All settings live in one JSON value: simple to read, migrate, export and import. */
class SettingsStore(private val context: Context) {

    private val key = stringPreferencesKey("settings_json")

    val flow: Flow<AppSettings> = context.dataStore.data
        .map { SettingsJson.decode(it[key]) }
        .distinctUntilChanged()

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val cur = SettingsJson.decode(prefs[key])
            prefs[key] = SettingsJson.encode(transform(cur))
        }
    }

    suspend fun replace(json: String) {
        val parsed = SettingsJson.decode(json)
        context.dataStore.edit { it[key] = SettingsJson.encode(parsed) }
    }

    suspend fun export(): String = SettingsJson.encode(current())
}
