package dev.matejgroombridge.store.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands an APK file to the system package installer via an Intent.
 *
 * On modern Android (8.0+), the user must have granted REQUEST_INSTALL_PACKAGES
 * to *this* app once. If not, calling [hasInstallPermission] returns false and the
 * UI should route the user to [installPermissionSettingsIntent] first.
 */
class ApkInstaller(private val context: Context) {

    fun hasInstallPermission(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installPermissionSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Launches the system installer for [apk]. Returns the Intent so callers can startActivity it. */
    fun installIntent(apk: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Convenience: open the system "App info" page so the user can uninstall. */
    fun openAppInfo(packageName: String): Intent =
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Launch the installed app, if it's actually installed. */
    fun launchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
