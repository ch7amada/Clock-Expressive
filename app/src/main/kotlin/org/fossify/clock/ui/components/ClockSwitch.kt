package org.fossify.clock.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.fossify.clock.ui.theme.LocalShowSwitchCheckmarks
import org.fossify.commons.R as CommonsR

@Composable
internal fun ClockSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val showCheckmark = LocalShowSwitchCheckmarks.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = if (checked && showCheckmark) {
            {
                Icon(
                    painter = painterResource(CommonsR.drawable.ic_check_vector),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}
