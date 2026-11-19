package org.fossify.clock.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.google.android.material.R as MaterialR
import org.fossify.clock.R
import org.fossify.commons.R as CommonsR

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
                        ReminderEditorTicket(
                            value = time,
                            supportingText = daylessLabel?.let { stringResource(it) },
                            clickLabel = selectTime,
                            icon = R.drawable.ic_clock_vector,
                            color = colors.primary,
                            contentColor = colors.onPrimary,
                            graphicColor = colors.primaryContainer,
                            graphicContentColor = colors.onPrimaryContainer,
                        ) {
                            focus.clearFocus()
                            onTimeClick()
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
                        ReminderOptions(
                            soundTitle = soundTitle,
                            vibrate = vibrate,
                            onSoundClick = onSoundClick,
                            onVibrateChange = onVibrateChange,
                        )
                    }
                    ReminderEditorActions(
                        modifier = Modifier.padding(start = padding, end = padding, bottom = 12.dp),
                        color = colors.primary,
                        contentColor = colors.onPrimary,
                        onCancel = onCancel,
                        onConfirm = {
                            focus.clearFocus()
                            onConfirm()
                        },
                    )
                }
            }
        }
    }
}
