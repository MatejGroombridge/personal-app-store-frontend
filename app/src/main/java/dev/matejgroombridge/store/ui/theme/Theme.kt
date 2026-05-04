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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable light/dark mode. Persisted via DataStore. */
enum class ThemeMode { System, Light, Dark }

/** Whether the device supports Material You wallpaper-derived colours. */
private val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Neutral Material 3 baseline used on pre-Android-12 devices that can't generate
 * a Material You palette from the wallpaper. Picks a calm pink primary so the
 * accent doesn't look randomly assigned.
 */
private val FallbackDarkScheme: ColorScheme = darkColorScheme()
private val FallbackLightScheme: ColorScheme = lightColorScheme()

/**
 * Root theme. Always Material You — wallpaper-derived dynamic colour on
 * Android 12+, baseline Material 3 on older OS versions.
 */
@Composable
fun AppStoreTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light  -> false
        ThemeMode.Dark   -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        supportsDynamicColor && isDark  -> dynamicDarkColorScheme(context)
        supportsDynamicColor && !isDark -> dynamicLightColorScheme(context)
        isDark                          -> FallbackDarkScheme
        else                            -> FallbackLightScheme
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
