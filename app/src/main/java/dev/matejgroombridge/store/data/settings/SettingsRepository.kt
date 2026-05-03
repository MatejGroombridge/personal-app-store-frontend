package dev.matejgroombridge.store.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.matejgroombridge.store.BuildConfig
import dev.matejgroombridge.store.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "matej_store_settings")

/** All user-tweakable preferences live here. Reactive via Flow, persisted via DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ThemeMode      = stringPreferencesKey("theme_mode")
        val DynamicColor   = booleanPreferencesKey("dynamic_color")
        val ManifestUrl    = stringPreferencesKey("manifest_url")
        val CheckIntervalH = intPreferencesKey("check_interval_hours")
    }

    data class Settings(
        val themeMode: ThemeMode,
        val dynamicColor: Boolean,
        val manifestUrl: String,
        val checkIntervalHours: Int,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.System,
            dynamicColor = prefs[Keys.DynamicColor] ?: true,
            manifestUrl = prefs[Keys.ManifestUrl] ?: BuildConfig.MANIFEST_URL,
            checkIntervalHours = prefs[Keys.CheckIntervalH] ?: 6,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.ThemeMode] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DynamicColor] = enabled }

    suspend fun setManifestUrl(url: String) =
        context.dataStore.edit { it[Keys.ManifestUrl] = url }

    suspend fun setCheckIntervalHours(hours: Int) =
        context.dataStore.edit { it[Keys.CheckIntervalH] = hours }
}
