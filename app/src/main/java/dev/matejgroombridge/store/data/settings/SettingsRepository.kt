package dev.matejgroombridge.store.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val ManifestUrl    = stringPreferencesKey("manifest_url")
        val CheckIntervalH = intPreferencesKey("check_interval_hours")
        val HiddenPackages = stringSetPreferencesKey("hidden_packages")
    }

    data class Settings(
        val themeMode: ThemeMode,
        val manifestUrl: String,
        val checkIntervalHours: Int,
        /** Package names the user has chosen to hide from the main list. */
        val hiddenPackages: Set<String>,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.System,
            manifestUrl = prefs[Keys.ManifestUrl] ?: BuildConfig.MANIFEST_URL,
            checkIntervalHours = prefs[Keys.CheckIntervalH] ?: 6,
            hiddenPackages = prefs[Keys.HiddenPackages] ?: emptySet(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.ThemeMode] = mode.name }

    suspend fun setManifestUrl(url: String) =
        context.dataStore.edit { it[Keys.ManifestUrl] = url }

    suspend fun setCheckIntervalHours(hours: Int) =
        context.dataStore.edit { it[Keys.CheckIntervalH] = hours }

    suspend fun setHidden(packageName: String, hidden: Boolean) =
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HiddenPackages] ?: emptySet()
            prefs[Keys.HiddenPackages] = if (hidden) current + packageName else current - packageName
        }
}
