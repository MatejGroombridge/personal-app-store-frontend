package dev.matejgroombridge.store.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Monokai-inspired palette.
 *
 * Background tones are slightly desaturated to keep large surfaces calm,
 * while accents are pulled straight from the classic Monokai scheme:
 *   - Pink/Magenta  #F92672  → primary accent
 *   - Green         #A6E22E  → secondary
 *   - Cyan          #66D9EF  → tertiary
 *   - Orange        #FD971F  → warnings
 *   - Yellow        #E6DB74  → strings/highlights
 *   - Purple        #AE81FF  → numerics
 */
object Monokai {
    // Background scaffolding
    val Bg          = Color(0xFF272822)
    val BgElevated  = Color(0xFF2D2E27)
    val BgSurface   = Color(0xFF34352F)
    val BgHigh      = Color(0xFF3E3D32)
    val Border      = Color(0xFF49483E)

    // Foreground / text
    val Fg          = Color(0xFFF8F8F2)
    val FgMuted     = Color(0xFFC2C2BB)
    val FgSubtle    = Color(0xFF75715E)

    // Accents
    val Pink        = Color(0xFFF92672)
    val Green       = Color(0xFFA6E22E)
    val Cyan        = Color(0xFF66D9EF)
    val Orange      = Color(0xFFFD971F)
    val Yellow      = Color(0xFFE6DB74)
    val Purple      = Color(0xFFAE81FF)

    // Light-mode counterparts (Monokai Light is rare; we synthesize a clean version)
    val LightBg         = Color(0xFFFAFAF5)
    val LightBgElevated = Color(0xFFFFFFFF)
    val LightBgSurface  = Color(0xFFF1F1EA)
    val LightBgHigh     = Color(0xFFE8E8DF)
    val LightBorder     = Color(0xFFD8D8CE)
    val LightFg         = Color(0xFF272822)
    val LightFgMuted    = Color(0xFF49483E)
    val LightFgSubtle   = Color(0xFF75715E)
}
