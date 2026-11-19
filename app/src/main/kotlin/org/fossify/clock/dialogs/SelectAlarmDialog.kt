package org.fossify.clock.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.models.Alarm
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.R as CommonsR

class SelectAlarmDialog(
    val activity: BaseSimpleActivity,
    val alarms: List<Alarm>,
    val titleResId: Int,
    val onAlarmPicked: (alarm: Alarm?) -> Unit,
) {
    init {
        val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
        var pickedAlarm: Alarm? = null
        val dismissOnDestroy = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialog.dismiss()
            }
        }
        activity.lifecycle.addObserver(dismissOnDestroy)
        dialog.setOnDismissListener {
            activity.lifecycle.removeObserver(dismissOnDestroy)
            onAlarmPicked(pickedAlarm)
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(ComposeView(dialog.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ClockTheme {
                    AlarmSelector(
                        alarms = alarms,
                        title = stringResource(titleResId),
                        onCancel = dialog::dismiss,
                        onConfirm = { alarm ->
                            if (dialog.isShowing) {
                                pickedAlarm = alarm
                                dialog.dismiss()
                            }
                        },
                    )
                }
            }
        })
        dialog.show()
    }
}

@Composable
private fun AlarmSelector(
    alarms: List<Alarm>,
    title: String,
    onCancel: () -> Unit,
    onConfirm: (Alarm?) -> Unit,
) {
    var selectedId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .semantics { paneTitle = title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                alarms.forEach { alarm ->
                    key(alarm.id) {
                        AlarmOption(
                            alarm = alarm,
                            time = context.getFormattedTime(alarm.timeInMinutes * 60, false, false).toString(),
                            selected = alarm.id == selectedId,
                            onSelect = { selectedId = alarm.id },
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(CommonsR.string.cancel)) }
                Button(onClick = { onConfirm(alarms.firstOrNull { it.id == selectedId }) }) {
                    Text(stringResource(CommonsR.string.ok))
                }
            }
        }
    }
}

@Composable
private fun AlarmOption(alarm: Alarm, time: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(CircleShape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column {
                Text(text = time, style = MaterialTheme.typography.titleMedium)
                if (alarm.label.isNotBlank()) {
                    Text(text = alarm.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
