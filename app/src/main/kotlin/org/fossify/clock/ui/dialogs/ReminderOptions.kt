package org.fossify.clock.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.fossify.clock.R
import org.fossify.commons.R as CommonsR

@Composable
internal fun ReminderOptions(
    soundTitle: String,
    vibrate: Boolean,
    onSoundClick: () -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val focus = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(enabled = enabled, role = Role.Button) {
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
                    .toggleable(
                    value = vibrate,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onVibrateChange,
                )
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
                Switch(checked = vibrate, onCheckedChange = null, enabled = enabled)
            }
        }
    }
}
