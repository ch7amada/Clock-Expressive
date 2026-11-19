package org.fossify.clock.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import org.fossify.clock.ui.theme.ClockTheme
import com.google.android.material.R as MaterialR
import org.fossify.commons.R as CommonsR

internal fun showTimerDurationPicker(
    activity: ComponentActivity,
    initialSeconds: Int,
    onConfirm: (Int) -> Unit,
) {
    val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
    val dismissOnDestroy = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dialog.dismiss()
        }
    }
    activity.lifecycle.addObserver(dismissOnDestroy)
    dialog.setOnDismissListener { activity.lifecycle.removeObserver(dismissOnDestroy) }
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        @Suppress("DEPRECATION")
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    dialog.setCanceledOnTouchOutside(true)
    dialog.setContentView(ComposeView(dialog.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                TimerDurationPicker(
                    initialSeconds = initialSeconds,
                    onCancel = dialog::dismiss,
                    onConfirm = { seconds ->
                        if (dialog.isShowing) {
                            dialog.dismiss()
                            onConfirm(seconds)
                        }
                    },
                )
            }
        }
    })
    dialog.show()
}

private enum class DurationUnit(val label: Int, val seconds: Int, val maximum: Int) {
    Hours(CommonsR.string.hours_raw, 3600, 23),
    Minutes(CommonsR.string.minutes_raw, 60, 59),
    Seconds(CommonsR.string.seconds_raw, 1, 59);

    fun initialValue(initialSeconds: Int): Int {
        val value = initialSeconds / seconds
        return (if (this == Hours) value else value % 60).coerceIn(0, maximum)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerDurationPicker(initialSeconds: Int, onCancel: () -> Unit, onConfirm: (Int) -> Unit) {
    val title = stringResource(CommonsR.string.duration)
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ROOT
    val gutter = if (configuration.screenWidthDp < 360) 8.dp else 0.dp
    val formatter = remember(locale) {
        NumberFormat.getIntegerInstance(locale).apply {
            minimumIntegerDigits = 2
            isGroupingUsed = false
        }
    }
    val focusManager = LocalFocusManager.current
    val units = DurationUnit.entries
    val wheels = units.map { unit -> rememberPagerState(unit.initialValue(initialSeconds)) { unit.maximum + 1 } }
    val fields = units.map { rememberTextFieldState() }
    val fieldValues = fields.map { it.text.toString().toIntOrNull() }
    val fieldsValid = fieldValues.zip(units).all { (value, unit) -> value != null && value in 0..unit.maximum }
    var useKeyboard by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.padding(horizontal = gutter).widthIn(max = 440.dp).fillMaxWidth()
            .semantics { paneTitle = title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Column(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                if (useKeyboard) {
                    units.forEachIndexed { index, unit ->
                        OutlinedTextField(
                            state = fields[index],
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(unit.label)) },
                            isError = fieldValues[index]?.let { it !in 0..unit.maximum } == true,
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            inputTransformation = InputTransformation {
                                if (length > 2 || asCharSequence().any { !it.isDigit() }) {
                                    revertAllChanges()
                                }
                            },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        units.forEach { unit ->
                            Text(
                                text = stringResource(unit.label),
                                modifier = Modifier.weight(1f).clearAndSetSemantics { },
                                textAlign = TextAlign.Center,
                                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        units.forEachIndexed { index, unit ->
                            DurationWheel(unit, wheels[index], formatter, Modifier.weight(1f))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = !useKeyboard || fieldsValid,
                    onClick = {
                        focusManager.clearFocus()
                        if (useKeyboard) {
                            wheels.forEachIndexed { index, wheel ->
                                wheel.requestScrollToPage(checkNotNull(fieldValues[index]))
                            }
                        } else {
                            fields.forEachIndexed { index, field ->
                                val value = formatter.format(wheels[index].currentPage)
                                field.edit {
                                    replace(0, length, value)
                                    selection = TextRange(0, length)
                                }
                            }
                        }
                        useKeyboard = !useKeyboard
                    },
                ) {
                    Icon(
                        painterResource(if (useKeyboard) MaterialR.drawable.ic_clock_black_24dp else MaterialR.drawable.ic_keyboard_black_24dp),
                        contentDescription = stringResource(
                            if (useKeyboard) MaterialR.string.material_timepicker_clock_mode_description
                            else MaterialR.string.material_timepicker_text_input_mode_description,
                        ),
                    )
                }
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(CommonsR.string.cancel))
                    }
                    Button(
                        modifier = Modifier.heightIn(min = 48.dp),
                        enabled = !useKeyboard || fieldsValid,
                        onClick = {
                            onConfirm(units.indices.sumOf { index ->
                                val value = if (useKeyboard) checkNotNull(fieldValues[index]) else wheels[index].currentPage
                                value * units[index].seconds
                            })
                        },
                    ) {
                        Text(stringResource(CommonsR.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DurationWheel(unit: DurationUnit, state: PagerState, formatter: NumberFormat, modifier: Modifier) {
    val label = stringResource(unit.label)
    val value = formatter.format(state.currentPage)
    val rowHeight = with(LocalDensity.current) { maxOf(48.dp, 40.sp.toDp()) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = label
            stateDescription = value
            progressBarRangeInfo = ProgressBarRangeInfo(state.currentPage.toFloat(), 0f..unit.maximum.toFloat(), unit.maximum - 1)
            setProgress { target ->
                val page = target.toInt().coerceIn(0, unit.maximum)
                if (page == state.currentPage) {
                    false
                } else {
                    scope.launch { state.animateScrollToPage(page) }
                    true
                }
            }
        },
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
    ) {
        VerticalPager(
            state = state,
            modifier = Modifier.fillMaxWidth().height(rowHeight * 3),
            pageSize = PageSize.Fixed(rowHeight),
            contentPadding = PaddingValues(vertical = rowHeight),
        ) { page ->
            val selected = page == state.currentPage
            val numberStyle = if (selected) MaterialTheme.typography.headlineMediumEmphasized else MaterialTheme.typography.titleLarge
            Surface(
                modifier = Modifier.fillMaxSize()
                    .clickable { scope.launch { state.animateScrollToPage(page) } }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) colors.primaryContainer else colors.surfaceContainer,
                contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatter.format(page),
                        style = numberStyle.copy(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
