package org.fossify.clock.helpers

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import org.fossify.clock.extensions.config
import org.fossify.clock.ui.theme.resolveClockColorScheme
import org.fossify.commons.R as CommonsR

fun Context.resolveWidgetBackgroundColor(): Int {
    val storedColor = config.widgetBgColor
    return if (!config.widgetBackgroundColorSet && storedColor == getColor(CommonsR.color.default_widget_bg_color)) {
        resolveClockColorScheme().primaryContainer.toArgb()
    } else {
        storedColor
    }
}

fun Context.resolveWidgetTextColor(): Int {
    val storedColor = config.widgetTextColor
    return if (!config.widgetTextColorSet && storedColor == getColor(CommonsR.color.default_widget_text_color)) {
        resolveClockColorScheme().onPrimaryContainer.toArgb()
    } else {
        storedColor
    }
}
