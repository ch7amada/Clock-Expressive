package org.fossify.clock.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import org.fossify.commons.R as CommonsR

@Composable
internal fun SelectionBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val countText = remember(count, locale) { NumberFormat.getIntegerInstance(locale).format(count) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(CommonsR.drawable.ic_cross_vector), stringResource(CommonsR.string.close))
            }
            Text(
                text = "${stringResource(CommonsR.string.items_selected)}: $countText",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onDelete) {
                Icon(painterResource(CommonsR.drawable.ic_delete_vector), stringResource(CommonsR.string.delete))
            }
        }
    }
}
