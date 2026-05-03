package dev.matejgroombridge.store.data.model

/**
 * Computed, per-app status combining the manifest entry with what's actually installed
 * on the device. Drives the action button label/behavior.
 */
sealed interface InstallState {
    data object NotInstalled : InstallState
    data class Installed(val installedVersionCode: Int, val installedVersionName: String) : InstallState
    data class UpdateAvailable(
        val installedVersionCode: Int,
        val installedVersionName: String,
    ) : InstallState
}

/**
 * Transient in-progress states tracked per package while the user is acting on it.
 * Kept separate from [InstallState] so we don't lose the underlying install info
 * mid-download.
 */
sealed interface ActionState {
    data object Idle : ActionState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ActionState {
        val progress: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    }
    data object Verifying : ActionState
    data object AwaitingInstall : ActionState
    data class Failed(val message: String) : ActionState
}
