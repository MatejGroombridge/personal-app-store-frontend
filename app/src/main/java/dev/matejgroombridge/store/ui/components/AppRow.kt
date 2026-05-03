package dev.matejgroombridge.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.InstallState

/**
 * One row of the app list. Spacious, Material 3, iconography to the left, status + action right.
 */
@Composable
fun AppRow(
    entry: AppEntry,
    installState: InstallState,
    actionState: ActionState,
    onPrimaryAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(entry)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.display_name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    StatusLine(entry, installState)
                }
                Spacer(Modifier.width(12.dp))
                ActionButton(
                    installState = installState,
                    actionState = actionState,
                    onClick = onPrimaryAction,
                )
            }

            // Inline progress bar while a download is in flight
            if (actionState is ActionState.Downloading && actionState.totalBytes > 0) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { actionState.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
        }
    }
}

@Composable
private fun AppIcon(entry: AppEntry) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (!entry.icon_url.isNullOrBlank()) {
            AsyncImage(
                model = entry.icon_url,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(shape),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun StatusLine(entry: AppEntry, state: InstallState) {
    val text = when (state) {
        is InstallState.NotInstalled    -> "v${entry.version_name}"
        is InstallState.Installed       -> "v${entry.version_name} • Installed"
        is InstallState.UpdateAvailable -> "v${state.installedVersionName} → v${entry.version_name}"
    }
    val color = when (state) {
        is InstallState.UpdateAvailable -> MaterialTheme.colorScheme.tertiary
        is InstallState.Installed       -> MaterialTheme.colorScheme.onSurfaceVariant
        is InstallState.NotInstalled    -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
