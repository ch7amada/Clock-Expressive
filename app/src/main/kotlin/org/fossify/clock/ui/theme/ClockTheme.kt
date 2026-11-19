package org.fossify.clock.ui.theme

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isCredentialStorageAvailable
import org.fossify.commons.helpers.FONT_TYPE_CUSTOM
import org.fossify.commons.helpers.FONT_TYPE_MONOSPACE
import org.fossify.commons.helpers.FontHelper

internal val LocalShowSwitchCheckmarks = compositionLocalOf { false }

private val ClockShapes = Shapes(
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
// At this luminance black and white have the same contrast ratio against the fill.
private const val BlackWhiteContrastThreshold = 0.179f

private fun Color.contrastingContentColor(): Color =
    if (luminance() > BlackWhiteContrastThreshold) Color.Black else Color.White

/** Applies the current Fossify appearance preferences to a Clock Compose surface. */
@Composable
fun ClockTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val preview = LocalInspectionMode.current
    var colorScheme by remember(context, darkTheme, preview) {
        mutableStateOf(if (preview) fixedColorScheme(darkTheme) else context.clockColorScheme(darkTheme))
    }
    var fontFamily by remember(context, preview) {
        mutableStateOf(if (preview) null else context.clockFontFamily())
    }
    var showSwitchCheckmarks by remember(context, preview) {
        mutableStateOf(!preview && context.isCredentialStorageAvailable && context.baseConfig.showCheckmarksOnSwitches)
    }
    val typography = remember(fontFamily) { fontFamily?.let { Typography().withFontFamily(it) } }

    if (!preview) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            colorScheme = context.clockColorScheme(darkTheme)
            fontFamily = context.clockFontFamily()
            showSwitchCheckmarks = context.isCredentialStorageAvailable && context.baseConfig.showCheckmarksOnSwitches
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ClockShapes,
        typography = typography,
    ) {
        CompositionLocalProvider(LocalShowSwitchCheckmarks provides showSwitchCheckmarks, content = content)
    }
}

private fun Context.clockFontFamily(): FontFamily? {
    if (!isCredentialStorageAvailable) return null

    return when (baseConfig.fontType) {
        FONT_TYPE_MONOSPACE -> FontFamily.Monospace
        FONT_TYPE_CUSTOM -> FontHelper.getTypeface(this).takeUnless { it == AndroidTypeface.DEFAULT }?.let {
            FontFamily(Typeface(it))
        }
        else -> null
    }
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = fontFamily),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = fontFamily),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = fontFamily),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily),
)

private fun fixedColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) darkColorScheme() else expressiveLightColorScheme()

private fun Context.clockColorScheme(darkTheme: Boolean): ColorScheme {
    if (baseConfig.isSystemThemeEnabled) {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> fixedColorScheme(darkTheme)
            darkTheme -> dynamicDarkColorScheme(this)
            else -> dynamicLightColorScheme(this)
        }
    }

    val surface = Color(getProperBackgroundColor())
    val onSurface = Color(getProperTextColor())
    val primary = Color(getProperPrimaryColor())
    val primaryContainer = lerp(surface, primary, 0.24f)

    return fixedColorScheme(darkTheme = surface.luminance() < BlackWhiteContrastThreshold).copy(
        primary = primary,
        onPrimary = primary.contrastingContentColor(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contrastingContentColor(),
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = lerp(surface, onSurface, 0.12f),
        onSurfaceVariant = onSurface,
        surfaceTint = primary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = lerp(surface, onSurface, 0.04f),
        surfaceContainer = lerp(surface, onSurface, 0.07f),
        surfaceContainerHigh = lerp(surface, onSurface, 0.10f),
        surfaceContainerHighest = lerp(surface, onSurface, 0.12f),
    )
}
