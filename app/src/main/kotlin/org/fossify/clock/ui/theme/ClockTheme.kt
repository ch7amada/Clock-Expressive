package org.fossify.clock.ui.theme

import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.ClockAppearance
import org.fossify.commons.extensions.isCredentialStorageAvailable

internal val LocalClockAppearance = compositionLocalOf { ClockAppearance() }

private val ClockShapes = Shapes(
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ClockTheme(window: Window? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    val configuration = LocalConfiguration.current
    val systemDark = isSystemInDarkTheme()
    val config = remember(context, preview) {
        if (!preview && context.isCredentialStorageAvailable) context.config else null
    }
    var appearance by remember(config) { mutableStateOf(config?.appearance ?: ClockAppearance()) }
    var resumeCount by remember { mutableStateOf(0) }

    if (config != null) {
        DisposableEffect(config) {
            val stopObserving = config.observeAppearance { appearance = it }
            onDispose(stopObserving)
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            appearance = config.appearance
            resumeCount++
        }
    }

    val darkTheme = appearance.mode.isDark(systemDark)
    val scheme = remember(context, configuration, darkTheme, appearance.palette, resumeCount) {
        val base = context.clockColorScheme(appearance.palette, darkTheme)
        base.copy(surface = lerp(base.surface, base.primary, 0.04f))
    }
    if (window != null) {
        SideEffect {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ClockShapes,
    ) {
        CompositionLocalProvider(LocalClockAppearance provides appearance, content = content)
    }
}
