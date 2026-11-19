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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.MyDigitalTimeWidgetProvider
import org.fossify.clock.helpers.SIMPLE_PHONE
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.clock.ui.widget.DigitalWidgetPreview
import org.fossify.clock.ui.widget.WidgetAppearanceScreen
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.R as CommonsR

class WidgetDigitalConfigureActivity : SimpleActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val storedBackground = config.widgetBgColor
        val initialBackground = Color.rgb(
            Color.red(storedBackground),
            Color.green(storedBackground),
            Color.blue(storedBackground),
        )
        val storedAlpha = Color.alpha(storedBackground) / 255f
        val initialOpacity = (storedAlpha * 100).toInt()
        // The previous SeekBar only quantized alpha when its progress changed from zero.
        val initialAlpha = if (initialOpacity == 0) storedAlpha else initialOpacity / 100f
        val initialTextColor = resolveTextColor()

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
            val backgroundColor = background.adjustAlpha(alpha)

            ClockTheme {
                WidgetAppearanceScreen(
                    backgroundColor = backgroundColor,
                    opaqueBackgroundColor = background,
                    textColor = textColor,
                    opacity = opacity,
                    onBackgroundColorChange = { background = it },
                    onTextColorChange = { textColor = it },
                    onOpacityChange = {
                        if (opacity != it) {
                            opacity = it
                            alpha = it / 100f
                        }
                    },
                    onBack = ::finish,
                    onSave = { saveConfig(widgetId, backgroundColor, textColor) },
                    preview = { DigitalWidgetPreview(use24HourFormat, textColor) },
                )
            }
        }
    }

    private fun resolveTextColor(): Int {
        val textColor = config.widgetTextColor
        return if (textColor == getColor(CommonsR.color.default_widget_text_color) && isDynamicTheme()) {
            getColor(CommonsR.color.you_primary_color)
        } else {
            textColor
        }
    }

    private fun saveConfig(widgetId: Int, backgroundColor: Int, textColor: Int) {
        config.apply {
            widgetBgColor = backgroundColor
            widgetTextColor = textColor
        }

        val update = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyDigitalTimeWidgetProvider::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        sendBroadcast(update)

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
