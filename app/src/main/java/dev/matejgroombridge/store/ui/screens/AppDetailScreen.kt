package dev.matejgroombridge.store.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import dev.matejgroombridge.store.data.model.ActionState
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.InstallState
import dev.matejgroombridge.store.ui.StoreViewModel
import dev.matejgroombridge.store.ui.components.ActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    vm: StoreViewModel,
    packageName: String,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
    onUninstall: () -> Unit,
) {
    val state by vm.ui.collectAsState()
    val actions by vm.actions.collectAsState()
    val settings by vm.settingsFlow.collectAsState()

    val entry = vm.entry(packageName)
    val install = state.installStates[packageName] ?: InstallState.NotInstalled
    val action = actions[packageName] ?: ActionState.Idle
    val isHidden = packageName in settings.hiddenPackages
    val isInstalled = install is InstallState.Installed || install is InstallState.UpdateAvailable

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
                actions = {
                    if (entry != null) {
                        // Open eye = visible (currently shown), tap to hide.
                        // Closed eye = hidden, tap to unhide.
                        IconButton(onClick = { vm.setHidden(packageName, !isHidden) }) {
                            Icon(
                                if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (isHidden) "Unhide app" else "Hide app",
                            )
                        }
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
            isInstalled = isInstalled,
            onPrimaryAction = onPrimaryAction,
            onUninstall = onUninstall,
            padding = padding,
        )
    }
}

@Composable
private fun DetailContent(
    entry: AppEntry,
    install: InstallState,
    action: ActionState,
    isInstalled: Boolean,
    onPrimaryAction: () -> Unit,
    onUninstall: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Secondary action: only meaningful for apps that are actually
            // installed, and only sensible while no install/update is in flight.
            if (isInstalled && action is ActionState.Idle) {
                androidx.compose.material3.TextButton(onClick = onUninstall) {
                    Text("Uninstall")
                }
            }
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
            ChangelogText(entry.changelog.ifBlank { "No changelog provided." })
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Details") {
            DetailRow("Package", entry.package_name)
            DetailRow("Version", "${entry.version_name} (${entry.version_code})")
            DetailRow("Size", formatBytes(entry.apk_size_bytes))
            DetailRow("Min Android SDK", entry.min_sdk.toString())
            if (entry.released_at.isNotBlank()) {
                DetailRow("Published", formatPublishedAt(entry.released_at))
            }
            entry.source_url?.let { LinkRow(label = "Source", url = it) }
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

/**
 * Same layout as [DetailRow] but the value is a tappable URL that opens the
 * system browser via ACTION_VIEW. Visually identical to DetailRow — no special
 * link styling — so the row feels like the rest of the section. Discoverability
 * comes from the ripple on tap rather than from a coloured/underlined label.
 */
@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, url.toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            .padding(vertical = 6.dp),
    ) {
        Text(
            label, modifier = Modifier.width(140.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

/**
 * Lightweight markdown renderer for changelog bodies. Supports the small subset
 * the changeset script produces:
 *   - "# " / "## " / "### " headings (rendered as styled headings)
 *   - "- " / "* " bullet lines (rendered with a bullet glyph)
 *   - blank lines as vertical spacing
 *   - everything else as a normal paragraph
 *
 * Avoids pulling in a full markdown library for a few characters of formatting.
 */
@Composable
private fun ChangelogText(text: String) {
    val lines = text.lines()
    Column {
        lines.forEachIndexed { index, raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> {
                    // Don't add a trailing gap after the last line.
                    if (index != lines.lastIndex) Spacer(Modifier.height(8.dp))
                }
                line.startsWith("### ") -> Text(
                    text = line.removePrefix("### ").trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp, bottom = 4.dp),
                )
                line.startsWith("## ") -> Text(
                    text = line.removePrefix("## ").trim(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 4.dp),
                )
                line.startsWith("# ") -> Text(
                    text = line.removePrefix("# ").trim(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp, bottom = 4.dp),
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                ) {
                    Text(
                        "•  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        line.drop(2).trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                else -> Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0L            -> "—"
    bytes < 1024           -> "$bytes B"
    bytes < 1024 * 1024    -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else                   -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

/**
 * Renders an ISO-8601 timestamp as e.g. "Mar 16 2026, 5:24pm" in the user's
 * local timezone. Falls back to the raw string if parsing fails so we never
 * show nothing.
 *
 * Accepts both "2026-05-03T02:53:26Z" and the fractional-seconds form
 * "2026-05-03T02:53:26.472617Z" that the release pipeline emits.
 */
private fun formatPublishedAt(iso: String): String {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    for (pattern in patterns) {
        val parsed: Date? = runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(iso)
        }.getOrNull()
        if (parsed != null) {
            // "MMM d yyyy, h:mma" gives "Mar 16 2026, 5:24PM" — we lowercase the AM/PM.
            val out = SimpleDateFormat("MMM d yyyy, h:mma", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }.format(parsed)
            return out.replace("AM", "am").replace("PM", "pm")
        }
    }
    return iso
}
