package org.fossify.clock.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.commons.R as CommonsR

@Composable
internal fun ClockTopBar(@StringRes title: Int, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val titleStyle = MaterialTheme.typography.headlineSmallEmphasized
    val titleHeight = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
    TopAppBar(
        title = {
            Text(
                stringResource(title),
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(CommonsR.drawable.ic_arrow_left_vector),
                    contentDescription = stringResource(CommonsR.string.back),
                )
            }
        },
        expandedHeight = maxOf(64.dp, titleHeight + 16.dp),
        windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            navigationIconContentColor = colors.onSurface,
        ),
    )
}
