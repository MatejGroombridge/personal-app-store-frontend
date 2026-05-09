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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "matej_store_settings")

/** All user-tweakable preferences live here. Reactive via Flow, persisted via DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ThemeMode      = stringPreferencesKey("theme_mode")
        val ManifestUrl    = stringPreferencesKey("manifest_url")
        val CheckIntervalH = intPreferencesKey("check_interval_hours")
        val HiddenPackages = stringSetPreferencesKey("hidden_packages")
        val DeveloperOptionsEnabled = booleanPreferencesKey("developer_options_enabled")
        val UpdateIdeasJson = stringPreferencesKey("update_ideas_json")
    }

    data class Settings(
        val themeMode: ThemeMode,
        val manifestUrl: String,
        val checkIntervalHours: Int,
        /** Package names the user has chosen to hide from the main list. */
        val hiddenPackages: Set<String>,
        /** Enables private developer-only affordances in the UI. */
        val developerOptionsEnabled: Boolean,
        /** Update ideas grouped by app package name. */
        val updateIdeas: Map<String, List<String>>,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.System,
            manifestUrl = prefs[Keys.ManifestUrl] ?: BuildConfig.MANIFEST_URL,
            checkIntervalHours = prefs[Keys.CheckIntervalH] ?: 6,
            hiddenPackages = prefs[Keys.HiddenPackages] ?: emptySet(),
            developerOptionsEnabled = prefs[Keys.DeveloperOptionsEnabled] ?: false,
            updateIdeas = prefs[Keys.UpdateIdeasJson]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, List<String>>>(json) }.getOrNull()
            } ?: emptyMap(),
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

    suspend fun setDeveloperOptionsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DeveloperOptionsEnabled] = enabled }

    suspend fun addUpdateIdea(packageName: String, idea: String) =
        context.dataStore.edit { prefs ->
            val trimmed = idea.trim()
            if (trimmed.isBlank()) return@edit
            val current = prefs[Keys.UpdateIdeasJson]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, List<String>>>(json) }.getOrNull()
            } ?: emptyMap()
            val updated = current + (packageName to (current[packageName].orEmpty() + trimmed))
            prefs[Keys.UpdateIdeasJson] = Json.encodeToString(updated)
        }

    suspend fun removeUpdateIdea(packageName: String, index: Int) =
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UpdateIdeasJson]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, List<String>>>(json) }.getOrNull()
            } ?: emptyMap()
            val ideas = current[packageName].orEmpty()
            if (index !in ideas.indices) return@edit
            val remaining = ideas.filterIndexed { i, _ -> i != index }
            val updated = if (remaining.isEmpty()) current - packageName else current + (packageName to remaining)
            prefs[Keys.UpdateIdeasJson] = Json.encodeToString(updated)
        }

    suspend fun clearUpdateIdeas(packageName: String) =
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UpdateIdeasJson]?.let { json ->
                runCatching { Json.decodeFromString<Map<String, List<String>>>(json) }.getOrNull()
            } ?: emptyMap()
            prefs[Keys.UpdateIdeasJson] = Json.encodeToString(current - packageName)
        }
}
