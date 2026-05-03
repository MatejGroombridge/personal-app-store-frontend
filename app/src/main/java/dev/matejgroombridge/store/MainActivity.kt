package dev.matejgroombridge.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.ui.StoreViewModel
import dev.matejgroombridge.store.ui.screens.AppDetailScreen
import dev.matejgroombridge.store.ui.screens.AppListScreen
import dev.matejgroombridge.store.ui.screens.SettingsScreen
import dev.matejgroombridge.store.ui.theme.MatejStoreTheme
import kotlinx.coroutines.launch

/**
 * Single-Activity host. Compose Navigation handles the three screens.
 *
 * Responsibilities not handled inside @Composables:
 *  - Splash screen
 *  - Edge-to-edge
 *  - Launching system Intents (install, app info, "unknown sources" permission)
 *  - Re-checking install state when we come back from those Intents
 */
class MainActivity : ComponentActivity() {

    private val vm: StoreViewModel by viewModels { StoreViewModel.Factory }

    private val installLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // System installer returned (success/cancel/fail). Re-check what's installed.
        vm.refreshInstalledStates()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> /* user came back from "unknown sources" settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settingsFlow.collectAsState()
            MatejStoreTheme(
                themeMode = settings.themeMode,
                useDynamicColor = settings.dynamicColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(
                        vm = vm,
                        onPrimaryAction = ::handlePrimaryAction,
                    )
                }
            }
        }
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
                installLauncher.launch(intent)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNavigation(
    vm: StoreViewModel,
    onPrimaryAction: (String) -> Unit,
) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            AppListScreen(
                vm = vm,
                onAppClick = { pkg -> nav.navigate("detail/$pkg") },
                onPrimaryAction = onPrimaryAction,
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("detail/{pkg}") { backStack ->
            val pkg = backStack.arguments?.getString("pkg").orEmpty()
            AppDetailScreen(
                vm = vm,
                packageName = pkg,
                onBack = { nav.popBackStack() },
                onPrimaryAction = { onPrimaryAction(pkg) },
            )
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}

/** Suppress unused-import lint when uri-related helpers aren't used. */
@Suppress("unused")
private val keepUri: Uri? = null
@Suppress("unused")
private val keepIntent: Intent? = null
