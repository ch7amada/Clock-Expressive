package org.fossify.clock.activities

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.MyDigitalTimeWidgetProvider
import org.fossify.clock.helpers.resolveWidgetBackgroundColor
import org.fossify.clock.helpers.resolveWidgetTextColor
import org.fossify.clock.helpers.SIMPLE_PHONE
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.clock.ui.widget.DigitalWidgetPreview
import org.fossify.clock.ui.widget.WidgetAppearanceScreen
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS

class WidgetDigitalConfigureActivity : SimpleActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val storedBackground = resolveWidgetBackgroundColor()
        val initialBackground = Color.rgb(
            Color.red(storedBackground),
            Color.green(storedBackground),
            Color.blue(storedBackground),
        )
        val storedAlpha = Color.alpha(storedBackground) / 255f
        val initialOpacity = (storedAlpha * 100).toInt()
        // The previous SeekBar only quantized alpha when its progress changed from zero.
        val initialAlpha = if (initialOpacity == 0) storedAlpha else initialOpacity / 100f
        val initialTextColor = resolveWidgetTextColor()

        if (!config.wasInitialWidgetSetUp && Build.BRAND.equals(SIMPLE_PHONE, ignoreCase = true)) {
            saveConfig(widgetId, initialBackground.adjustAlpha(initialAlpha), initialTextColor)
            config.wasInitialWidgetSetUp = true
            return
        }

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !intent.getBooleanExtra(IS_CUSTOMIZING_COLORS, false)) {
            finish()
            return
        }

        val use24HourFormat = config.use24HourFormat
        setContent {
            var background by rememberSaveable { mutableIntStateOf(initialBackground) }
            var alpha by rememberSaveable { mutableFloatStateOf(initialAlpha) }
            var opacity by rememberSaveable { mutableIntStateOf(initialOpacity) }
            var textColor by rememberSaveable { mutableIntStateOf(initialTextColor) }
            var backgroundChanged by rememberSaveable { mutableStateOf(false) }
            var textColorChanged by rememberSaveable { mutableStateOf(false) }
            val backgroundColor = background.adjustAlpha(alpha)

            ClockTheme(window = window) {
                WidgetAppearanceScreen(
                    backgroundColor = backgroundColor,
                    opaqueBackgroundColor = background,
                    textColor = textColor,
                    opacity = opacity,
                    onBackgroundColorChange = { background = it; backgroundChanged = true },
                    onTextColorChange = { textColor = it; textColorChanged = true },
                    onOpacityChange = {
                        if (opacity != it) {
                            opacity = it
                            alpha = it / 100f
                            backgroundChanged = true
                        }
                    },
                    onBack = ::finish,
                    onSave = { saveConfig(widgetId, backgroundColor, textColor, backgroundChanged, textColorChanged) },
                    preview = { DigitalWidgetPreview(use24HourFormat, textColor) },
                )
            }
        }
    }

    private fun saveConfig(widgetId: Int, backgroundColor: Int, textColor: Int, backgroundChanged: Boolean = false, textColorChanged: Boolean = false) {
        config.apply {
            if (backgroundChanged) {
                widgetBgColor = backgroundColor
                widgetBackgroundColorSet = true
            }
            if (textColorChanged) {
                widgetTextColor = textColor
                widgetTextColorSet = true
            }
        }

        val update = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyDigitalTimeWidgetProvider::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        sendBroadcast(update)

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
