package dev.matejgroombridge.store.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable theme mode. Persisted via DataStore. */
enum class ThemeMode { System, Light, Dark, Monokai }

/** Whether to draw colors from the device wallpaper (Material You, Android 12+). */
private val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val MonokaiDarkScheme: ColorScheme = darkColorScheme(
    primary            = Monokai.Pink,
    onPrimary          = Monokai.Bg,
    primaryContainer   = Monokai.Pink.copy(alpha = 0.25f),
    onPrimaryContainer = Monokai.Fg,

    secondary            = Monokai.Green,
    onSecondary          = Monokai.Bg,
    secondaryContainer   = Monokai.Green.copy(alpha = 0.18f),
    onSecondaryContainer = Monokai.Fg,

    tertiary            = Monokai.Cyan,
    onTertiary          = Monokai.Bg,
    tertiaryContainer   = Monokai.Cyan.copy(alpha = 0.18f),
    onTertiaryContainer = Monokai.Fg,

    background     = Monokai.Bg,
    onBackground   = Monokai.Fg,
    surface        = Monokai.Bg,
    onSurface      = Monokai.Fg,
    surfaceVariant = Monokai.BgElevated,
    onSurfaceVariant = Monokai.FgMuted,

    surfaceContainerLowest = Monokai.Bg,
    surfaceContainerLow    = Monokai.BgElevated,
    surfaceContainer       = Monokai.BgSurface,
    surfaceContainerHigh   = Monokai.BgHigh,
    surfaceContainerHighest = Monokai.BgHigh,

    outline        = Monokai.Border,
    outlineVariant = Monokai.Border.copy(alpha = 0.5f),

    error          = Monokai.Orange,
    onError        = Monokai.Bg,
    errorContainer = Monokai.Orange.copy(alpha = 0.2f),
    onErrorContainer = Monokai.Fg,
)

private val MonokaiLightScheme: ColorScheme = lightColorScheme(
    primary            = Monokai.Pink,
    onPrimary          = Color.White,
    primaryContainer   = Monokai.Pink.copy(alpha = 0.15f),
    onPrimaryContainer = Monokai.LightFg,

    secondary            = Color(0xFF4D8B0F),
    onSecondary          = Color.White,
    secondaryContainer   = Monokai.Green.copy(alpha = 0.18f),
    onSecondaryContainer = Monokai.LightFg,

    tertiary            = Color(0xFF1E8FA8),
    onTertiary          = Color.White,
    tertiaryContainer   = Monokai.Cyan.copy(alpha = 0.18f),
    onTertiaryContainer = Monokai.LightFg,

    background     = Monokai.LightBg,
    onBackground   = Monokai.LightFg,
    surface        = Monokai.LightBg,
    onSurface      = Monokai.LightFg,
    surfaceVariant = Monokai.LightBgSurface,
    onSurfaceVariant = Monokai.LightFgMuted,

    surfaceContainerLowest = Monokai.LightBgElevated,
    surfaceContainerLow    = Monokai.LightBg,
    surfaceContainer       = Monokai.LightBgSurface,
    surfaceContainerHigh   = Monokai.LightBgHigh,
    surfaceContainerHighest = Monokai.LightBgHigh,

    outline        = Monokai.LightBorder,
    outlineVariant = Monokai.LightBorder.copy(alpha = 0.5f),

    error          = Color(0xFFB3261E),
    onError        = Color.White,
)

/**
 * Root theme. Resolves [themeMode] + [useDynamicColor] into a concrete [ColorScheme].
 *
 * Resolution rules:
 *  - Monokai mode → always Monokai dark palette (it's the "vibe" mode).
 *  - Otherwise:
 *      - useDynamicColor && Android 12+  → wallpaper colors
 *      - else → Material default light/dark
 *  - System/Light/Dark control whether we render the dark or light variant.
 */
@Composable
fun MatejStoreTheme(
    themeMode: ThemeMode = ThemeMode.System,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.System  -> systemDark
        ThemeMode.Light   -> false
        ThemeMode.Dark    -> true
        ThemeMode.Monokai -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        themeMode == ThemeMode.Monokai -> MonokaiDarkScheme
        useDynamicColor && supportsDynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        isDark -> MonokaiDarkScheme
        else   -> MonokaiLightScheme
    }

    // Sync system bar icon colors with the chosen scheme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            val barsAreLight = colorScheme.background.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = barsAreLight
            controller.isAppearanceLightNavigationBars = barsAreLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
