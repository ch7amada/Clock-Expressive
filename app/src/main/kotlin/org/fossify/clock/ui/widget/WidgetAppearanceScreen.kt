package org.fossify.clock.ui.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.clock.R
import org.fossify.commons.compose.alert_dialog.rememberAlertDialogState
import org.fossify.commons.dialogs.ColorPickerAlertDialog
import java.text.NumberFormat
import kotlin.math.roundToInt
import org.fossify.commons.R as CommonsR

@Composable
internal fun WidgetAppearanceScreen(
    backgroundColor: Int,
    opaqueBackgroundColor: Int,
    opacity: Int,
    onBackgroundColorChange: (Int) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    textColor: Int? = null,
    onTextColorChange: (Int) -> Unit = {},
    preview: @Composable () -> Unit,
) {
    val backgroundPicker = rememberAlertDialogState()
    val textPicker = rememberAlertDialogState()
    val colors = MaterialTheme.colorScheme
    val cutoutSides = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gutter = if (maxWidth < 360.dp) 16.dp else 24.dp
        Scaffold(
            modifier = Modifier.windowInsetsPadding(cutoutSides),
            topBar = { WidgetAppearanceTopBar(onBack) },
        ) { insets ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .consumeWindowInsets(insets)
                    .padding(horizontal = gutter),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color(backgroundColor),
                    ) {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            preview()
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = colors.surfaceContainerHigh,
                        contentColor = colors.onSurface,
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ColorSwatchRow(CommonsR.string.background_color, backgroundColor, backgroundPicker::show)
                            OpacityControl(opacity, onOpacityChange)
                            if (textColor != null) {
                                ColorSwatchRow(CommonsR.string.text_color, textColor, textPicker::show)
                            }
                        }
                    }
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(CommonsR.string.save))
                }
            }
        }
    }

    backgroundPicker.DialogMember {
        ColorPickerAlertDialog(backgroundPicker, opaqueBackgroundColor, onActiveColorChange = {}) { confirmed, color ->
            if (confirmed) onBackgroundColorChange(color)
        }
    }
    if (textColor != null) {
        textPicker.DialogMember {
            ColorPickerAlertDialog(textPicker, textColor, onActiveColorChange = {}) { confirmed, color ->
                if (confirmed) onTextColorChange(color)
            }
        }
    }
}

@Composable
private fun WidgetAppearanceTopBar(onBack: () -> Unit) {
    val titleStyle = MaterialTheme.typography.headlineSmallEmphasized
    val lineHeight = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
    TopAppBar(
        title = {
            Text(
                stringResource(CommonsR.string.customize_widget_colors),
                modifier = Modifier.semantics { heading() },
                style = titleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(CommonsR.drawable.ic_arrow_left_vector),
                    contentDescription = stringResource(CommonsR.string.back),
                )
            }
        },
        expandedHeight = maxOf(64.dp, lineHeight * 2 + 12.dp),
    )
}

@Composable
private fun ColorSwatchRow(@StringRes label: Int, color: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        Box(
            Modifier
                .size(32.dp)
                .background(Color(color), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

@Composable
private fun OpacityControl(opacity: Int, onOpacityChange: (Int) -> Unit) {
    val label = stringResource(R.string.widget_background_opacity)
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { NumberFormat.getPercentInstance(locale) }
    val percentage = remember(format, opacity) { format.format(opacity / 100.0) }
    val sliderState = remember { SliderState(steps = 99, trackRange = 0f..100f) }
    sliderState.value = opacity.toFloat()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(percentage, style = MaterialTheme.typography.labelLarge)
    }
    Slider(
        state = sliderState,
        onValueChange = { onOpacityChange(it.roundToInt()) },
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = label
            stateDescription = percentage
        },
        track = { SliderDefaults.Track(sliderState = it, drawTick = { _, _ -> }) },
    )
}
