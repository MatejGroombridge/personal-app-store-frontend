package dev.matejgroombridge.store

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.ui.StoreViewModel
import dev.matejgroombridge.store.ui.screens.AppDetailScreen
import dev.matejgroombridge.store.ui.screens.AppListScreen
import dev.matejgroombridge.store.ui.screens.HiddenAppsScreen
import dev.matejgroombridge.store.ui.screens.SettingsScreen
import dev.matejgroombridge.store.ui.theme.AppStoreTheme
import kotlinx.coroutines.launch

/**
 * Single-Activity host. Compose Navigation handles the four screens.
 *
 * Responsibilities not handled inside @Composables:
 *  - Splash screen
 *  - Edge-to-edge
 *  - Launching system Intents (install, app info, "unknown sources" permission)
 *  - Re-checking install state when we come back from those Intents
 *  - Sequencing the "Install all updates" queue across multiple installer dialogs
 *  - Asking for POST_NOTIFICATIONS on Android 13+ so background update checks
 *    can actually surface a notification
 *  - Honouring the deep-link intent extra written by [UpdateCheckWorker] so a
 *    tap on the update notification jumps straight to the right detail screen
 */
class MainActivity : ComponentActivity() {

    private val vm: StoreViewModel by viewModels { StoreViewModel.Factory }

    /** Package the user most recently kicked off an install for, so we can clear
     *  its transient ActionState (the spinner) when the system installer returns. */
    private var pendingInstallPackage: String? = null

    private val installLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // System installer returned (success/cancel/fail). Clear the pending action
        // state so the spinner stops, then re-check what's installed.
        pendingInstallPackage?.let(vm.installs::reset)
        pendingInstallPackage = null
        vm.refreshInstalledStates()

        // If the user kicked off "Install all updates", continue the chain.
        installNextFromQueue()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> /* user came back from "unknown sources" settings */ }

    /** Result handler for the runtime POST_NOTIFICATIONS request on Android 13+. */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — silent either way; the worker will still fire, just no notif. */ }

    /** Compose-side handle so the notification deep-link can navigate. */
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for POST_NOTIFICATIONS on Android 13+ exactly once per launch
        // (the system handles "don't ask again" internally).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by vm.settingsFlow.collectAsState()
            AppStoreTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(
                        vm = vm,
                        onPrimaryAction = ::handlePrimaryAction,
                        onUninstall = ::handleUninstall,
                        onInstallAllUpdates = ::handleInstallAllUpdates,
                        onNavReady = { navController = it },
                    )
                }
            }
        }

        // If launched via the update notification, jump straight to that detail screen.
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Activity is reused (singleTop / from notification PendingIntent.FLAG_UPDATE_CURRENT).
        handleDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        // The user may have installed/uninstalled an app outside our flow.
        vm.refreshInstalledStates()
    }

    /**
     * Decides what the row's primary button does depending on current state.
     * - Not installed / Update available → download + verify + launch system installer
     * - Installed                       → launch the app
     */
    private fun handlePrimaryAction(packageName: String) {
        val entry = vm.entry(packageName) ?: return
        val state = vm.ui.value.installStates[packageName]

        if (state is InstallState.Installed) {
            vm.installs.launchAppIntent(packageName)?.let(::startActivity)
            return
        }

        if (!vm.installs.canInstallNow()) {
            permissionLauncher.launch(vm.installs.installPermissionIntent())
            return
        }

        lifecycleScope.launch {
            vm.installs.downloadAndInstall(entry)?.let { intent ->
                pendingInstallPackage = packageName
                installLauncher.launch(intent)
            }
        }
    }

    /** Pops the system "Uninstall this app?" confirmation. */
    private fun handleUninstall(packageName: String) {
        vm.installs.uninstallIntent(packageName)?.let(::startActivity)
    }

    /** Kicks off the "Install all updates" chain — picks up the head of the queue. */
    private fun handleInstallAllUpdates() {
        if (!vm.installs.canInstallNow()) {
            permissionLauncher.launch(vm.installs.installPermissionIntent())
            return
        }
        vm.installAllUpdates()
        installNextFromQueue()
    }

    /** Pulls the next pending install off the queue and launches its installer. */
    private fun installNextFromQueue() {
        val next = vm.popInstallQueue() ?: return
        val entry = vm.entry(next) ?: return installNextFromQueue() // skip stale entries
        lifecycleScope.launch {
            vm.installs.downloadAndInstall(entry)?.let { intent ->
                pendingInstallPackage = next
                installLauncher.launch(intent)
            } ?: run {
                // Failure on this one — keep going with the rest of the queue
                // rather than abandoning everything that was lined up.
                installNextFromQueue()
            }
        }
    }

    /**
     * Honour the optional `EXTRA_DEEP_LINK_PACKAGE` written by [UpdateCheckWorker]
     * when the user taps the update notification.
     */
    private fun handleDeepLink(intent: Intent?) {
        val pkg = intent?.getStringExtra(EXTRA_DEEP_LINK_PACKAGE) ?: return
        // Defer one frame so the NavHost has actually been composed.
        navController?.let { nav ->
            // popBackStack to root first to avoid stacking detail screens on
            // each notification tap.
            nav.popBackStack(route = "list", inclusive = false)
            nav.navigate("detail/$pkg")
        }
    }

    companion object {
        /** Notification → deep-link intent extra. Read in [handleDeepLink]. */
        const val EXTRA_DEEP_LINK_PACKAGE = "deep_link_package"
    }
}

@androidx.compose.runtime.Composable
private fun AppNavigation(
    vm: StoreViewModel,
    onPrimaryAction: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onInstallAllUpdates: () -> Unit,
    onNavReady: (NavHostController) -> Unit,
) {
    val nav = rememberNavController()
    LaunchedEffect(nav) { onNavReady(nav) }

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            AppListScreen(
                vm = vm,
                onAppClick = { pkg -> nav.navigate("detail/$pkg") },
                onPrimaryAction = onPrimaryAction,
                onOpenSettings = { nav.navigate("settings") },
                onOpenHidden = { nav.navigate("hidden") },
                onInstallAllUpdates = onInstallAllUpdates,
            )
        }
        composable("detail/{pkg}") { backStack ->
            val pkg = backStack.arguments?.getString("pkg").orEmpty()
            AppDetailScreen(
                vm = vm,
                packageName = pkg,
                onBack = { nav.popBackStack() },
                onPrimaryAction = { onPrimaryAction(pkg) },
                onUninstall = { onUninstall(pkg) },
            )
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable("hidden") {
            HiddenAppsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
