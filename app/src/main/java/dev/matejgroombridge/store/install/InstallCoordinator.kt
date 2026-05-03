package dev.matejgroombridge.store.install

import android.content.Context
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.AppEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * High-level orchestration layer.
 *
 * The UI calls [downloadAndInstall] with an [AppEntry]; this class coordinates the
 * downloader + verifier + installer and emits per-package [ActionState]s the UI
 * observes to render progress bars / spinners / errors.
 */
class InstallCoordinator(context: Context) {

    private val downloader = ApkDownloader(context)
    private val installer = ApkInstaller(context)

    private val _state = MutableStateFlow<Map<String, ActionState>>(emptyMap())
    val state: StateFlow<Map<String, ActionState>> = _state.asStateFlow()

    /** Snapshot the current [ActionState] for [packageName]. */
    fun stateFor(packageName: String): ActionState =
        _state.value[packageName] ?: ActionState.Idle

    /** Returns false if the user must first grant REQUEST_INSTALL_PACKAGES. */
    fun canInstallNow(): Boolean = installer.hasInstallPermission()

    fun installPermissionIntent() = installer.installPermissionSettingsIntent()
    fun launchAppIntent(pkg: String) = installer.launchIntent(pkg)
    fun openAppInfoIntent(pkg: String) = installer.openAppInfo(pkg)
    fun uninstallIntent(pkg: String) = installer.uninstallIntent(pkg)

    /**
     * Full pipeline: download → SHA-256 verify → return install Intent.
     *
     * Returns null on failure (state will already be marked Failed).
     */
    suspend fun downloadAndInstall(entry: AppEntry): android.content.Intent? {
        val pkg = entry.package_name
        val fileName = "${pkg}-${entry.version_name}.apk"

        update(pkg, ActionState.Downloading(0L, entry.apk_size_bytes))

        val file = runCatching {
            downloader.download(entry.apk_url, fileName) { downloaded, total ->
                val effectiveTotal = if (total > 0) total else entry.apk_size_bytes
                update(pkg, ActionState.Downloading(downloaded, effectiveTotal))
            }
        }.getOrElse { e ->
            update(pkg, ActionState.Failed(e.message ?: "Download failed"))
            return null
        }

        update(pkg, ActionState.Verifying)
        val actual = runCatching { downloader.sha256(file) }.getOrElse { e ->
            update(pkg, ActionState.Failed("Verification error: ${e.message}"))
            return null
        }
        if (!actual.equals(entry.apk_sha256, ignoreCase = true)) {
            file.delete()
            update(pkg, ActionState.Failed("Checksum mismatch — refusing to install"))
            return null
        }

        update(pkg, ActionState.AwaitingInstall)
        return installer.installIntent(file)
    }

    /** Called after the system installer returns, to reset our transient state. */
    fun reset(packageName: String) {
        _state.update { it - packageName }
    }

    private fun update(pkg: String, s: ActionState) {
        _state.update { it + (pkg to s) }
    }
}
