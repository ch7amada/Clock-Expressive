package org.fossify.clock.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.ClockAppearance
import org.fossify.clock.helpers.ClockPalette
import org.fossify.commons.extensions.isCredentialStorageAvailable

fun Context.resolveClockColorScheme(): ColorScheme {
    val appearance = if (isCredentialStorageAvailable) config.appearance else ClockAppearance()
    val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    return clockColorScheme(appearance.palette, appearance.mode.isDark(systemDark))
}

internal fun Context.clockColorScheme(palette: ClockPalette, darkTheme: Boolean): ColorScheme {
    if (palette == ClockPalette.System && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (darkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
    }
    val base = when {
        palette == ClockPalette.Plum -> plumSurfaces(darkTheme)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    return base.withPalette(palette.tones, darkTheme)
}

private data class AccentColors(val tone10: Long, val tone20: Long, val tone30: Long, val tone40: Long, val tone80: Long, val tone90: Long)
private data class PaletteTones(val primary: AccentColors, val secondary: AccentColors, val tertiary: AccentColors)

private val IndigoTones = PaletteTones(
    AccentColors(0xFF13174D, 0xFF292D63, 0xFF40447C, 0xFF575C96, 0xFFBFC2FF, 0xFFE0E0FF),
    AccentColors(0xFF1A1B2C, 0xFF2F3042, 0xFF46465A, 0xFF5E5D72, 0xFFC7C4DD, 0xFFE4E1FA),
    AccentColors(0xFF2E1121, 0xFF462638, 0xFF5F3C4F, 0xFF795367, 0xFFE8B9D0, 0xFFFFD8EB),
)
private val TealTones = PaletteTones(
    AccentColors(0xFF00201E, 0xFF003733, 0xFF00504A, 0xFF006A62, 0xFF78D7CB, 0xFF96F3E7),
    AccentColors(0xFF09201E, 0xFF1F3533, 0xFF354C49, 0xFF4D6361, 0xFFB4CCC8, 0xFFD0E8E4),
    AccentColors(0xFF081D34, 0xFF203249, 0xFF364962, 0xFF4E607B, 0xFFB6C8E9, 0xFFD4E3FF),
)
private val VioletTones = PaletteTones(
    AccentColors(0xFF24005A, 0xFF3D2172, 0xFF553989, 0xFF6E51A3, 0xFFD5BBFF, 0xFFEDDCFF),
    AccentColors(0xFF21182B, 0xFF372D41, 0xFF4E4359, 0xFF665A72, 0xFFD1C1DC, 0xFFEDDCF8),
    AccentColors(0xFF330D1C, 0xFF4B2231, 0xFF653849, 0xFF805062, 0xFFF1B7C9, 0xFFFFD9E3),
)
private val PlumTones = PaletteTones(
    AccentColors(0xFF3E0809, 0xFF60191B, 0xFF80302E, 0xFF9A4140, 0xFFFFB3AA, 0xFFFFDAD5),
    AccentColors(0xFF251331, 0xFF3D2948, 0xFF553F60, 0xFF6E5779, 0xFFDAC0E5, 0xFFF7DCFF),
    AccentColors(0xFF2F1227, 0xFF48283E, 0xFF624057, 0xFF7D5770, 0xFFEDBAD7, 0xFFFFD8EB),
)

private fun plumSurfaces(darkTheme: Boolean): ColorScheme = if (darkTheme) {
    darkColorScheme(
        background = Color(0xFF1B111E),
        onBackground = Color(0xFFF0DFEE),
        surface = Color(0xFF1B111E),
        onSurface = Color(0xFFF0DFEE),
        surfaceVariant = Color(0xFF4C3A4D),
        onSurfaceVariant = Color(0xFFE0C6DE),
        inverseSurface = Color(0xFFF0DFEE),
        inverseOnSurface = Color(0xFF382A3B),
        outline = Color(0xFFA08AA0),
        outlineVariant = Color(0xFF534054),
        surfaceBright = Color(0xFF443347),
        surfaceDim = Color(0xFF1B111E),
        surfaceContainerLowest = Color(0xFF150C18),
        surfaceContainerLow = Color(0xFF241A26),
        surfaceContainer = Color(0xFF291E2C),
        surfaceContainerHigh = Color(0xFF352638),
        surfaceContainerHighest = Color(0xFF403143),
    )
} else {
    lightColorScheme(
        background = Color(0xFFFFF7FD),
        onBackground = Color(0xFF251827),
        surface = Color(0xFFFFF7FD),
        onSurface = Color(0xFF251827),
        surfaceVariant = Color(0xFFF2E0EF),
        onSurfaceVariant = Color(0xFF514051),
        inverseSurface = Color(0xFF382A3B),
        inverseOnSurface = Color(0xFFF9EBF8),
        outline = Color(0xFF7E687D),
        outlineVariant = Color(0xFFD6BFD3),
        surfaceBright = Color(0xFFFFF7FD),
        surfaceDim = Color(0xFFE6D5E4),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFFEFFB),
        surfaceContainer = Color(0xFFF9E9F7),
        surfaceContainerHigh = Color(0xFFF3E3F1),
        surfaceContainerHighest = Color(0xFFEDDEEB),
    )
}

private val ClockPalette.tones: PaletteTones
    get() = when (this) {
        ClockPalette.System, ClockPalette.Indigo -> IndigoTones
        ClockPalette.Teal -> TealTones
        ClockPalette.Violet -> VioletTones
        ClockPalette.Plum -> PlumTones
    }

private fun ColorScheme.withPalette(tones: PaletteTones, darkTheme: Boolean): ColorScheme {
    val (primary, secondary, tertiary) = tones
    return copy(
        primary = Color(if (darkTheme) primary.tone80 else primary.tone40),
        onPrimary = if (darkTheme) Color(primary.tone20) else Color.White,
        primaryContainer = Color(if (darkTheme) primary.tone30 else primary.tone90),
        onPrimaryContainer = Color(if (darkTheme) primary.tone90 else primary.tone10),
        secondary = Color(if (darkTheme) secondary.tone80 else secondary.tone40),
        onSecondary = if (darkTheme) Color(secondary.tone20) else Color.White,
        secondaryContainer = Color(if (darkTheme) secondary.tone30 else secondary.tone90),
        onSecondaryContainer = Color(if (darkTheme) secondary.tone90 else secondary.tone10),
        tertiary = Color(if (darkTheme) tertiary.tone80 else tertiary.tone40),
        onTertiary = if (darkTheme) Color(tertiary.tone20) else Color.White,
        tertiaryContainer = Color(if (darkTheme) tertiary.tone30 else tertiary.tone90),
        onTertiaryContainer = Color(if (darkTheme) tertiary.tone90 else tertiary.tone10),
        inversePrimary = Color(if (darkTheme) primary.tone40 else primary.tone80),
        surfaceTint = Color(if (darkTheme) primary.tone80 else primary.tone40),
        primaryFixed = Color(primary.tone90),
        primaryFixedDim = Color(primary.tone80),
        onPrimaryFixed = Color(primary.tone10),
        onPrimaryFixedVariant = Color(primary.tone30),
        secondaryFixed = Color(secondary.tone90),
        secondaryFixedDim = Color(secondary.tone80),
        onSecondaryFixed = Color(secondary.tone10),
        onSecondaryFixedVariant = Color(secondary.tone30),
        tertiaryFixed = Color(tertiary.tone90),
        tertiaryFixedDim = Color(tertiary.tone80),
        onTertiaryFixed = Color(tertiary.tone10),
        onTertiaryFixedVariant = Color(tertiary.tone30),
    )
}
