package org.fossify.clock.ui.dialogs

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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.clock.R
import org.fossify.commons.R as CommonsR

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TimerEditorContent(
    duration: String,
    label: TextFieldState,
    soundTitle: String,
    vibrate: Boolean,
    saving: Boolean,
    onDurationClick: () -> Unit,
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
    val durationLabel = stringResource(CommonsR.string.duration)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .clickable(enabled = !saving, interactionSource = null, indication = null, onClick = onCancel)
                .clearAndSetSemantics { },
        )
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(margin),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = colors.surface,
            ) {
                Column {
                    Column(
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(start = padding, top = padding, end = padding, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = colors.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(enabled = !saving, role = Role.Button, onClickLabel = durationLabel) {
                                        focus.clearFocus()
                                        onDurationClick()
                                    }
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = duration,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 45.sp),
                                    style = MaterialTheme.typography.displayMediumEmphasized.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontFeatureSettings = "tnum",
                                    ),
                                )
                                Icon(painterResource(R.drawable.ic_hourglass_vector), contentDescription = null)
                            }
                        }
                        OutlinedTextField(
                            state = label,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving,
                            label = { Text(stringResource(CommonsR.string.label)) },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            shape = MaterialTheme.shapes.large,
                        )
                        ReminderOptions(
                            soundTitle = soundTitle,
                            vibrate = vibrate,
                            onSoundClick = onSoundClick,
                            onVibrateChange = onVibrateChange,
                            enabled = !saving,
                        )
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(start = padding, end = padding, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onCancel, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(CommonsR.string.cancel))
                        }
                        Button(
                            modifier = Modifier.heightIn(min = 48.dp),
                            enabled = !saving,
                            onClick = {
                                focus.clearFocus()
                                onConfirm()
                            },
                        ) {
                            Text(stringResource(CommonsR.string.ok))
                        }
                    }
                }
            }
        }
    }
}
