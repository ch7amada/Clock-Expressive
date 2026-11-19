package org.fossify.clock.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

internal data class SettingsChoice(val value: Int, @StringRes val label: Int)

@Composable
internal fun SettingsChoiceDialog(
    title: String,
    options: List<SettingsChoice>,
    selectedValue: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().semantics { paneTitle = title },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
                )
                options.forEach { option ->
                    val selected = option.value == selectedValue
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .selectable(selected = selected, role = Role.RadioButton) {
                                    onDismiss()
                                    onSelect(option.value)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
