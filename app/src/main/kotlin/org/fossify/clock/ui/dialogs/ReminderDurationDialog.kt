package org.fossify.clock.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.extensions.getFormattedSeconds
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.helpers.DAY_SECONDS
import org.fossify.commons.helpers.HOUR_SECONDS
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.R as CommonsR

internal fun ComponentActivity.showReminderDurationDialog(
    curSeconds: Int,
    showSecondsForCustom: Boolean = false,
    cancelCallback: (() -> Unit)? = null,
    callback: (Int) -> Unit,
) {
    hideKeyboard()
    val presets = sortedSetOf(MINUTE_SECONDS, 5 * MINUTE_SECONDS, 10 * MINUTE_SECONDS, 30 * MINUTE_SECONDS, HOUR_SECONDS, curSeconds)
    var showCustom by mutableStateOf(false)
    val dialog = object : ComponentDialog(this, android.R.style.Theme_DeviceDefault_Dialog) {
        override fun cancel() {
            if (showCustom) {
                window?.let { WindowCompat.getInsetsController(it, it.decorView).hide(WindowInsetsCompat.Type.ime()) }
                showCustom = false
            } else {
                super.cancel()
            }
        }
    }
    val dismissOnDestroy = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dialog.dismiss()
        }
    }
    lifecycle.addObserver(dismissOnDestroy)
    dialog.setOnDismissListener { lifecycle.removeObserver(dismissOnDestroy) }
    dialog.setOnCancelListener { cancelCallback?.invoke() }
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        @Suppress("DEPRECATION")
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    dialog.setCanceledOnTouchOutside(true)

    fun confirm(seconds: Int) {
        if (dialog.isShowing) {
            dialog.dismiss()
            callback(seconds)
        }
    }

    dialog.setContentView(ComposeView(dialog.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                if (showCustom) {
                    CustomDurationPicker(showSecondsForCustom, dialog::cancel, ::confirm)
                } else {
                    DurationPopup(
                        title = stringResource(CommonsR.string.duration),
                        onCancel = dialog::cancel,
                        onConfirm = { confirm(curSeconds) },
                    ) {
                        Column(
                            modifier = Modifier.selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            presets.forEach { seconds ->
                                DurationOption(
                                    label = getFormattedSeconds(seconds, false),
                                    selected = seconds == curSeconds,
                                    onClick = { confirm(seconds) },
                                )
                            }
                            DurationOption(stringResource(CommonsR.string.custom), selected = false) { showCustom = true }
                        }
                    }
                }
            }
        }
    })
    dialog.show()
}

private enum class ReminderUnit(val label: Int, val seconds: Int) {
    Seconds(CommonsR.string.seconds_raw, 1),
    Minutes(CommonsR.string.minutes_raw, MINUTE_SECONDS),
    Hours(CommonsR.string.hours_raw, HOUR_SECONDS),
    Days(CommonsR.string.days_raw, DAY_SECONDS),
}

@Composable
private fun CustomDurationPicker(
    showSeconds: Boolean,
    onCancel: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var unit by remember { mutableStateOf(ReminderUnit.Minutes) }
    val value = rememberTextFieldState()
    val input = value.text.toString()
    val seconds = (if (input.isEmpty()) 0 else input.toIntOrNull())
        ?.takeIf { it in 0..Int.MAX_VALUE / unit.seconds }
        ?.times(unit.seconds)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val confirm = { seconds?.let(onConfirm) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    DurationPopup(
        title = stringResource(CommonsR.string.custom),
        onCancel = onCancel,
        onConfirm = { confirm() },
        confirmEnabled = seconds != null,
    ) {
        OutlinedTextField(
            state = value,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            label = { Text(stringResource(CommonsR.string.value)) },
            isError = seconds == null,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = InputTransformation {
                if (asCharSequence().any { !it.isDigit() }) revertAllChanges()
            },
            onKeyboardAction = { confirm() },
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ReminderUnit.entries.drop(if (showSeconds) 0 else 1).forEach { option ->
                DurationOption(stringResource(option.label), selected = unit == option) { unit = option }
            }
        }
    }
}

@Composable
private fun DurationPopup(
    title: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().semantics { paneTitle = title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Column(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMediumEmphasized)
                content()
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(CommonsR.string.cancel))
                }
                Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(CommonsR.string.ok))
                }
            }
        }
    }
}

@Composable
private fun DurationOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .selectable(selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
