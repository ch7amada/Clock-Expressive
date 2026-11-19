package org.fossify.clock.ui.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.models.MyTimeZone
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.R as CommonsR

internal fun showTimeZonePicker(
    activity: SimpleActivity,
    timeZones: List<MyTimeZone>,
    selectedIds: Set<String>,
    onConfirm: (Set<String>) -> Unit,
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
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.setCanceledOnTouchOutside(true)
    dialog.setContentView(ComposeView(dialog.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                TimeZonePicker(
                    timeZones = timeZones,
                    initialSelection = selectedIds,
                    onCancel = dialog::dismiss,
                    onConfirm = { selection ->
                        if (dialog.isShowing) {
                            dialog.dismiss()
                            onConfirm(selection)
                        }
                    },
                )
            }
        }
    })
    dialog.show()
}

@Composable
private fun TimeZonePicker(
    timeZones: List<MyTimeZone>,
    initialSelection: Set<String>,
    onCancel: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var selectedIds by remember { mutableStateOf(initialSelection) }
    val title = stringResource(R.string.time_zone)
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
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(timeZones, key = { it.id }) { timeZone ->
                    val id = timeZone.id.toString()
                    val selected = id in selectedIds
                    val rowShape = MaterialTheme.shapes.medium
                    Surface(
                        shape = rowShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(rowShape)
                                .toggleable(
                                    value = selected,
                                    role = Role.Checkbox,
                                    onValueChange = { checked ->
                                        selectedIds = if (checked) selectedIds + id else selectedIds - id
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = timeZone.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Checkbox(checked = selected, onCheckedChange = null)
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(CommonsR.string.cancel)) }
                Button(onClick = { onConfirm(selectedIds) }) { Text(stringResource(CommonsR.string.ok)) }
            }
        }
    }
}
