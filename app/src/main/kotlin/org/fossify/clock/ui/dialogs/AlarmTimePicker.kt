package org.fossify.clock.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.isInputValid
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.R as MaterialR
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.R as CommonsR

@OptIn(ExperimentalMaterial3Api::class)
internal fun showAlarmTimePicker(
    activity: ComponentActivity,
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onTimePicked: (Int, Int) -> Unit,
) {
    val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
    val dismissOnDestroy = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dialog.dismiss()
        }
    }
    activity.lifecycle.addObserver(dismissOnDestroy)
    dialog.setOnDismissListener { activity.lifecycle.removeObserver(dismissOnDestroy) }
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        WindowCompat.setDecorFitsSystemWindows(this, false)
        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setDimAmount(0.32f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            attributes = attributes.apply { setFitInsetsTypes(0) }
        }
        @Suppress("DEPRECATION")
        setSoftInputMode(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            },
        )
    }
    dialog.setContentView(ComposeView(dialog.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                val state = rememberTimePickerState(initialHour, initialMinute, is24Hour)
                AlarmTimePicker(
                    state = state,
                    onCancel = dialog::dismiss,
                    onConfirm = {
                        if (dialog.isShowing && state.isInputValid) {
                            dialog.dismiss()
                            onTimePicked(state.hour, state.minute)
                        }
                    },
                )
            }
        }
    })

    dialog.show()
    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmTimePicker(
    state: TimePickerState,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    var useKeyboard by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val margin = if (configuration.screenWidthDp < 360) 8.dp else 16.dp
    val maxWidth = if (configuration.screenWidthDp > configuration.screenHeightDp) 648.dp else 400.dp

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .clickable(interactionSource = null, indication = null, onClick = onCancel)
                .clearAndSetSemantics { },
        )
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(margin),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
                color = colors.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                BoxWithConstraints {
                    val horizontalPadding = if (maxWidth < 336.dp) 8.dp else 16.dp
                    val layout = if (maxWidth >= 628.dp) {
                        TimePickerLayoutType.Horizontal
                    } else {
                        TimePickerLayoutType.Vertical
                    }

                    Column(Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp)) {
                        Column(
                            modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(MaterialR.string.material_timepicker_select_time),
                                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp).semantics { heading() },
                                style = MaterialTheme.typography.titleMediumEmphasized,
                            )
                            if (useKeyboard) {
                                TimeInput(state)
                            } else {
                                TimePicker(
                                    state = state,
                                    shapes = TimePickerDefaults.shapes(timeFieldShape = MaterialTheme.shapes.extraLarge),
                                    colors = TimePickerDefaults.colors(
                                        clockDialColor = colors.surfaceContainerLowest,
                                        timeSelectorSelectedContainerColor = colors.primaryContainer,
                                        timeSelectorSelectedContentColor = colors.onPrimaryContainer,
                                        timeSelectorContainerColor = colors.surfaceContainer,
                                        timeSelectorContentColor = colors.onSurface,
                                    ),
                                    layoutType = layout,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                if (useKeyboard) {
                                    // Discard incomplete keypad entries when returning to the dial.
                                    state.hourInput = state.hour
                                    state.minuteInput = state.minute
                                }
                                useKeyboard = !useKeyboard
                            }) {
                                val icon = if (useKeyboard) {
                                    MaterialR.drawable.ic_clock_black_24dp
                                } else {
                                    MaterialR.drawable.ic_keyboard_black_24dp
                                }
                                val description = if (useKeyboard) {
                                    MaterialR.string.material_timepicker_clock_mode_description
                                } else {
                                    MaterialR.string.material_timepicker_text_input_mode_description
                                }
                                Icon(painterResource(icon), contentDescription = stringResource(description))
                            }
                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                TextButton(onClick = onCancel) {
                                    Text(stringResource(CommonsR.string.cancel))
                                }
                                Button(onClick = onConfirm, enabled = state.isInputValid) {
                                    Text(stringResource(CommonsR.string.ok))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
