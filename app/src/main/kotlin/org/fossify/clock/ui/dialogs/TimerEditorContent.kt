package org.fossify.clock.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.fossify.clock.R
import org.fossify.commons.R as CommonsR

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
                        ReminderEditorTicket(
                            value = duration,
                            supportingText = durationLabel,
                            clickLabel = durationLabel,
                            icon = R.drawable.ic_hourglass_vector,
                            color = colors.tertiary,
                            contentColor = colors.onTertiary,
                            graphicColor = colors.tertiaryContainer,
                            graphicContentColor = colors.onTertiaryContainer,
                            fontFamily = FontFamily.Monospace,
                            enabled = !saving,
                        ) {
                            focus.clearFocus()
                            onDurationClick()
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
                    ReminderEditorActions(
                        modifier = Modifier.padding(start = padding, end = padding, bottom = 12.dp),
                        color = colors.tertiary,
                        contentColor = colors.onTertiary,
                        onCancel = onCancel,
                        onConfirm = {
                            focus.clearFocus()
                            onConfirm()
                        },
                        enabled = !saving,
                    )
                }
            }
        }
    }
}
