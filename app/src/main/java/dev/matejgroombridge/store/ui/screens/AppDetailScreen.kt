package dev.matejgroombridge.store.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.ui.StoreViewModel
import dev.matejgroombridge.store.ui.components.ActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    vm: StoreViewModel,
    packageName: String,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    val state by vm.ui.collectAsState()
    val actions by vm.actions.collectAsState()

    val entry = vm.entry(packageName)
    val install = state.installStates[packageName] ?: InstallState.NotInstalled
    val action = actions[packageName] ?: ActionState.Idle

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(entry?.display_name ?: "App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (entry == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("App not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        DetailContent(
            entry = entry,
            install = install,
            action = action,
            onPrimaryAction = onPrimaryAction,
            padding = padding,
        )
    }
}

@Composable
private fun DetailContent(
    entry: AppEntry,
    install: InstallState,
    action: ActionState,
    onPrimaryAction: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        // Hero
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (!entry.icon_url.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.icon_url,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Apps, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.display_name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Version ${entry.version_name}" + (entry.category?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(install, action, onPrimaryAction)
        }

        if (action is ActionState.Downloading) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { action.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (action is ActionState.Failed) {
            Spacer(Modifier.height(12.dp))
            Text(
                action.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
        if (entry.description.isNotBlank()) {
            Text(
                entry.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(24.dp))
        }

        SectionCard(title = "What's new") {
            Text(
                entry.changelog.ifBlank { "No changelog provided." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Details") {
            DetailRow("Package", entry.package_name)
            DetailRow("Version", "${entry.version_name} (${entry.version_code})")
            DetailRow("Size", formatBytes(entry.apk_size_bytes))
            DetailRow("Min Android SDK", entry.min_sdk.toString())
            if (entry.released_at.isNotBlank()) DetailRow("Published", entry.released_at)
            entry.source_url?.let { DetailRow("Source", it) }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, modifier = Modifier.width(140.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0L            -> "—"
    bytes < 1024           -> "$bytes B"
    bytes < 1024 * 1024    -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else                   -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
