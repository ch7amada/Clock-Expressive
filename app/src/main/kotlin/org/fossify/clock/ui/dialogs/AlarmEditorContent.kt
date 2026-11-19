package org.fossify.clock.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.google.android.material.R as MaterialR
import org.fossify.clock.R
import org.fossify.commons.R as CommonsR

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AlarmEditorContent(
    time: String,
    @StringRes daylessLabel: Int?,
    dayIndexes: List<Int>,
    days: Int,
    label: TextFieldState,
    soundTitle: String,
    vibrate: Boolean,
    onTimeClick: () -> Unit,
    onDayClick: (Int) -> Unit,
    onSoundClick: () -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val narrow = LocalConfiguration.current.screenWidthDp < 360
    val margin = if (narrow) 8.dp else 16.dp
    val padding = if (narrow) 16.dp else 24.dp
    val dayNames = stringArrayResource(CommonsR.array.week_days)
    val dayShortNames = stringArrayResource(CommonsR.array.week_days_short)
    val selectTime = stringResource(MaterialR.string.material_timepicker_select_time)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .clickable(interactionSource = null, indication = null, onClick = onCancel)
                .clearAndSetSemantics { },
        )
        Box(
            Modifier.fillMaxSize().safeDrawingPadding().padding(margin),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                color = colors.surface,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column {
                    Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(start = padding, top = padding, end = padding, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = colors.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable(role = Role.Button, onClickLabel = selectTime) {
                                        focus.clearFocus()
                                        onTimeClick()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.displaySmallEmphasized
                                            .copy(fontFeatureSettings = "tnum"),
                                    )
                                    daylessLabel?.let {
                                        Text("(${stringResource(it)})", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Icon(painterResource(R.drawable.ic_clock_vector), contentDescription = null)
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            dayIndexes.forEach { dayIndex ->
                                FilterChip(
                                    selected = days > 0 && (days and (1 shl dayIndex)) != 0,
                                    onClick = { onDayClick(dayIndex) },
                                    label = {
                                        Text(
                                            text = dayShortNames[dayIndex],
                                            modifier = Modifier.clearAndSetSemantics {
                                                contentDescription = dayNames[dayIndex]
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        OutlinedTextField(
                            state = label,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(CommonsR.string.label)) },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            shape = MaterialTheme.shapes.large,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = colors.surfaceContainerHigh,
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(role = Role.Button) {
                                            focus.clearFocus()
                                            onSoundClick()
                                        }
                                        .heightIn(min = 56.dp)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(painterResource(CommonsR.drawable.ic_bell_vector), contentDescription = null)
                                    Text(soundTitle, style = MaterialTheme.typography.bodyLarge)
                                }
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .toggleable(value = vibrate, role = Role.Switch, onValueChange = onVibrateChange)
                                        .heightIn(min = 56.dp)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(painterResource(CommonsR.drawable.ic_vibrate_vector), contentDescription = null)
                                    Text(
                                        text = stringResource(R.string.vibrate),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Switch(checked = vibrate, onCheckedChange = null)
                                }
                            }
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(start = padding, end = padding, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(CommonsR.string.cancel))
                        }
                        Button(onClick = {
                            focus.clearFocus()
                            onConfirm()
                        }) {
                            Text(stringResource(CommonsR.string.ok))
                        }
                    }
                }
            }
        }
    }
}
