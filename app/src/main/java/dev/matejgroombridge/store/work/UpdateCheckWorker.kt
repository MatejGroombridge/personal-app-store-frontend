package dev.matejgroombridge.store.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.matejgroombridge.store.MainActivity
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.data.repository.InstalledAppsRepository
import dev.matejgroombridge.store.data.repository.RemoteManifestRepository
import dev.matejgroombridge.store.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically polls the manifest and posts a notification listing apps that have
 * updates available. Runs in the background under WorkManager constraints (network only).
 *
 * Schedule from anywhere with [schedule] — typically called from
 * [dev.matejgroombridge.store.StoreApplication.onCreate] or after the user
 * tweaks the check interval in Settings.
 */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val settings = SettingsRepository(ctx).settings.first()
        val manifest = RemoteManifestRepository().fetchManifest(settings.manifestUrl)
            .getOrElse { return Result.retry() }

        val installed = InstalledAppsRepository(ctx)
        val updates = manifest.apps.mapNotNull { entry ->
            (installed.stateFor(entry) as? InstallState.UpdateAvailable)?.let { entry to it }
        }

        if (updates.isNotEmpty()) {
            notifyUpdates(ctx, updates.size, updates.joinToString(", ") { it.first.display_name })
        }
        return Result.success()
    }

    private fun notifyUpdates(ctx: Context, count: Int, names: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "App updates",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifies when your apps have new versions available."
            }
            nm.createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = if (count == 1) "1 app update available" else "$count app updates available"
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(names)
            .setStyle(NotificationCompat.BigTextStyle().bigText(names))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "update_checks"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "update_check_periodic"

        /** Idempotent: replaces any existing schedule with the supplied interval. */
        fun schedule(context: Context, intervalHours: Int = 6) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                repeatInterval = intervalHours.toLong().coerceAtLeast(1L),
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
