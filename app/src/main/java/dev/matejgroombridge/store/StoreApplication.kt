package dev.matejgroombridge.store

import android.app.Application
import dev.matejgroombridge.store.data.settings.SettingsRepository
import dev.matejgroombridge.store.work.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application entry point. Schedules the periodic update-check WorkManager job
 * using the user-configured interval from DataStore.
 */
class StoreApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val settings = SettingsRepository(this@StoreApplication).settings.first()
            UpdateCheckWorker.schedule(this@StoreApplication, settings.checkIntervalHours)
        }
    }
}
