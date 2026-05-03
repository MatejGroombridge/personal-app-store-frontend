package dev.matejgroombridge.store.data.repository

import android.content.Context
import android.content.pm.PackageManager
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.InstallState

/**
 * Asks Android's PackageManager whether a given package is installed and at what version.
 *
 * Note: requires QUERY_ALL_PACKAGES permission on Android 11+ (declared in the manifest).
 */
class InstalledAppsRepository(private val context: Context) {

    /** Compute the install state for a single manifest entry. */
    fun stateFor(entry: AppEntry): InstallState {
        val pm = context.packageManager
        return try {
            val info = pm.getPackageInfo(entry.package_name, 0)
            val installedCode = pkgVersionCode(info)
            val installedName = info.versionName ?: ""
            when {
                installedCode < entry.version_code -> InstallState.UpdateAvailable(installedCode, installedName)
                else -> InstallState.Installed(installedCode, installedName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            InstallState.NotInstalled
        }
    }

    private fun pkgVersionCode(info: android.content.pm.PackageInfo): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }
}
