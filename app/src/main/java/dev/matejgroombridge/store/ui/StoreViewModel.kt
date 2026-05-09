package dev.matejgroombridge.store.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import dev.matejgroombridge.store.BuildConfig
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.AppManifest
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.data.repository.InstalledAppsRepository
import dev.matejgroombridge.store.data.repository.ManifestRepository
import dev.matejgroombridge.store.data.repository.RemoteManifestRepository
import dev.matejgroombridge.store.data.settings.SettingsRepository
import dev.matejgroombridge.store.install.InstallCoordinator
import dev.matejgroombridge.store.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single ViewModel that owns the UI state. Small enough not to warrant splitting per-screen.
 *
 * Composes three sources:
 *  - manifest (network)
 *  - installed apps (PackageManager)
 *  - in-flight action state per package (download/verify/install)
 */
class StoreViewModel(
    app: Application,
    private val manifestRepo: ManifestRepository,
    private val installedRepo: InstalledAppsRepository,
    private val settings: SettingsRepository,
    val installs: InstallCoordinator,
) : AndroidViewModel(app) {

    data class UiState(
        val isLoading: Boolean = true,
        val manifest: AppManifest? = null,
        val error: String? = null,
        /** Per-package install state, refreshed alongside the manifest. */
        val installStates: Map<String, InstallState> = emptyMap(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** Per-package transient action (download/verify/install). */
    val actions: StateFlow<Map<String, ActionState>> = installs.state

    /**
     * Queue of packages waiting to be installed sequentially via the system
     * installer. Populated by [installAllUpdates]; the head is launched by the
     * activity, and as each install returns the activity calls [popInstallQueue]
     * to pull the next one.
     *
     * Sequential because Android only surfaces one install dialog at a time —
     * trying to launch a second while the first is open just gets dropped.
     */
    private val _installQueue = MutableStateFlow<List<String>>(emptyList())
    val installQueue: StateFlow<List<String>> = _installQueue.asStateFlow()

    /** User settings (theme, manifest URL, etc.) */
    val settingsFlow: StateFlow<SettingsRepository.Settings> = settings.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        SettingsRepository.Settings(
            themeMode = ThemeMode.System,
            manifestUrl = BuildConfig.MANIFEST_URL,
            checkIntervalHours = 6,
            hiddenPackages = emptySet(),
            developerOptionsEnabled = false,
            updateIdeas = emptyMap(),
        ),
    )

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            val url = settingsFlow.value.manifestUrl
            manifestRepo.fetchManifest(url)
                .onSuccess { manifest ->
                    val states = manifest.apps.associate { it.package_name to installedRepo.stateFor(it) }
                    _ui.update { it.copy(isLoading = false, manifest = manifest, installStates = states) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load manifest") }
                }
        }
    }

    /** Recompute install states only (cheap; useful after returning from system installer). */
    fun refreshInstalledStates() {
        val manifest = _ui.value.manifest ?: return
        val states = manifest.apps.associate { it.package_name to installedRepo.stateFor(it) }
        _ui.update { it.copy(installStates = states) }
    }

    fun entry(packageName: String): AppEntry? =
        _ui.value.manifest?.apps?.firstOrNull { it.package_name == packageName }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setManifestUrl(url: String) = viewModelScope.launch {
        settings.setManifestUrl(url); refresh()
    }
    fun setCheckIntervalHours(hours: Int) = viewModelScope.launch { settings.setCheckIntervalHours(hours) }
    fun setHidden(packageName: String, hidden: Boolean) = viewModelScope.launch {
        settings.setHidden(packageName, hidden)
    }

    fun setDeveloperOptionsEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setDeveloperOptionsEnabled(enabled)
    }

    fun addUpdateIdea(packageName: String, idea: String) = viewModelScope.launch {
        settings.addUpdateIdea(packageName, idea)
    }

    fun removeUpdateIdea(packageName: String, index: Int) = viewModelScope.launch {
        settings.removeUpdateIdea(packageName, index)
    }

    fun clearUpdateIdeas(packageName: String) = viewModelScope.launch {
        settings.clearUpdateIdeas(packageName)
    }

    /** Enqueue every app currently in [InstallState.UpdateAvailable]. */
    fun installAllUpdates() {
        val pending = _ui.value.installStates
            .filterValues { it is InstallState.UpdateAvailable }
            .keys
            .toList()
        _installQueue.value = pending
    }

    /** Pop the next package off the install queue (called by the activity once
     *  the previous install dialog has returned). Returns null when empty. */
    fun popInstallQueue(): String? {
        val current = _installQueue.value
        if (current.isEmpty()) return null
        val next = current.first()
        _installQueue.value = current.drop(1)
        return next
    }

    fun clearInstallQueue() { _installQueue.value = emptyList() }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                return StoreViewModel(
                    app = app,
                    manifestRepo = RemoteManifestRepository(),
                    installedRepo = InstalledAppsRepository(app),
                    settings = SettingsRepository(app),
                    installs = InstallCoordinator(app),
                ) as T
            }
        }
    }
}
