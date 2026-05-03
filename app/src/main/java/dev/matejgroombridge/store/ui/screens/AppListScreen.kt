package dev.matejgroombridge.store.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.ui.StoreViewModel
import dev.matejgroombridge.store.ui.components.AppRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    vm: StoreViewModel,
    onAppClick: (String) -> Unit,
    onPrimaryAction: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHidden: () -> Unit,
    onInstallAllUpdates: () -> Unit,
) {
    val state by vm.ui.collectAsState()
    val actions by vm.actions.collectAsState()
    val settings by vm.settingsFlow.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Hidden apps drop out before any other processing happens.
    val nonHidden = state.manifest?.apps
        ?.filter { it.package_name !in settings.hiddenPackages }
        ?: emptyList()

    fun stateOf(e: AppEntry): InstallState =
        state.installStates[e.package_name] ?: InstallState.NotInstalled

    val updateCount = nonHidden.count { stateOf(it) is InstallState.UpdateAvailable }

    // Sort: updates first (most actionable), then not-installed (new), then installed.
    // Within each group, alphabetical by display name (case-insensitive).
    val visibleApps = nonHidden.sortedWith(
        compareBy<AppEntry>(
            { when (stateOf(it)) {
                is InstallState.UpdateAvailable -> 0
                is InstallState.NotInstalled    -> 1
                is InstallState.Installed       -> 2
            } },
            { it.display_name.lowercase() },
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("My Apps") },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenHidden) {
                        Icon(Icons.Outlined.VisibilityOff, contentDescription = "Hidden apps")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.manifest == null -> CenteredSpinner(padding)
            state.error != null && state.manifest == null -> CenteredMessage(
                title = "Couldn't load apps",
                detail = state.error!!,
                padding = padding,
            )
            state.manifest?.apps.isNullOrEmpty() -> CenteredMessage(
                title = "No apps yet",
                detail = "Push a tag to one of your repos to publish your first app.",
                padding = padding,
            )
            nonHidden.isEmpty() -> CenteredMessage(
                title = "Nothing to show",
                detail = "All apps in the manifest are hidden. Tap the eye icon above to manage them.",
                padding = padding,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // "Install all updates" sticky banner — only when ≥2 are
                // available (single-update is fine via the row's button).
                if (updateCount >= 2) {
                    item("install_all") {
                        InstallAllBanner(
                            count = updateCount,
                            onInstallAll = onInstallAllUpdates,
                        )
                    }
                }

                items(visibleApps, key = { it.package_name }) { entry ->
                    AppRow(
                        entry = entry,
                        installState = state.installStates[entry.package_name] ?: InstallState.NotInstalled,
                        actionState = actions[entry.package_name] ?: ActionState.Idle,
                        onPrimaryAction = { onPrimaryAction(entry.package_name) },
                        onClick = { onAppClick(entry.package_name) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/**
 * Promotional banner that surfaces when ≥2 updates are available, providing a
 * single-tap way to chain through them all via the system installer queue.
 */
@Composable
private fun InstallAllBanner(count: Int, onInstallAll: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.SystemUpdateAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$count updates available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Install them one after another.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Button(onClick = onInstallAll) { Text("Install all") }
        }
    }
}

@Composable
private fun CenteredSpinner(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CenteredMessage(title: String, detail: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
