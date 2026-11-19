package org.fossify.clock.ui.stopwatch

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import org.fossify.clock.R
import org.fossify.clock.extensions.formatStopwatchTime
import org.fossify.clock.helpers.SORT_BY_LAP
import org.fossify.clock.helpers.SORT_BY_LAP_TIME
import org.fossify.clock.helpers.SORT_BY_TOTAL_TIME
import org.fossify.clock.helpers.STOPWATCH_LIVE_LAP_ID
import org.fossify.clock.helpers.Stopwatch
import org.fossify.commons.R as CommonsR
import org.fossify.commons.helpers.SORT_DESCENDING
import java.text.NumberFormat
import java.util.Locale

internal data class StopwatchUiState(
    val status: Stopwatch.State = Stopwatch.State.STOPPED,
    val totalTime: Long = 0,
    val useLongerMSFormat: Boolean = false,
    val sorting: Int = 0,
    val laps: List<StopwatchLap> = emptyList(),
)

internal data class StopwatchLap(
    val id: Int,
    val number: Int,
    val lapTime: Long,
    val totalTime: Long,
)

@Composable
internal fun StopwatchScreen(
    state: StopwatchUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit,
    onSort: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val recordLap = {
        onLap()
        listState.requestScrollToItem(0)
    }
    val changeSorting: (Int) -> Unit = { column ->
        onSort(column)
        listState.requestScrollToItem(0)
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        val gutter = if (maxWidth < 360.dp) 12.dp else 24.dp
        val wideLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE && maxWidth >= 552.dp
        val sideBySide = wideLandscape && maxHeight >= 128.dp && state.laps.isNotEmpty()
        val compactHeight = maxHeight < (240 + 152 * LocalDensity.current.fontScale).dp
        val controls: @Composable () -> Unit = {
            StopwatchControls(state, wideLandscape, onToggle, onReset, recordLap)
        }
        val table: @Composable (Modifier) -> Unit = { modifier ->
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    LapHeader(state.sorting, changeSorting)
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        lapRows(state.laps, changeSorting)
                    }
                }
            }
        }

        if (sideBySide) {
            Row(
                modifier = Modifier.widthIn(max = 960.dp).fillMaxSize().padding(horizontal = gutter, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
                table(Modifier.weight(1.15f))
            }
        } else if (compactHeight) {
            val colors = MaterialTheme.colorScheme
            val shape = MaterialTheme.shapes.large
            LazyColumn(
                modifier = Modifier.widthIn(max = 536.dp).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = gutter, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "controls") { controls() }
                if (state.laps.isNotEmpty()) {
                    item(key = "lap_header") {
                        Surface(shape = shape, color = colors.surfaceContainerLow) {
                            LapHeader(state.sorting, changeSorting)
                        }
                    }
                    lapRows(state.laps, changeSorting)
                }
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = 536.dp).fillMaxWidth().padding(horizontal = gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                controls()
                if (state.laps.isNotEmpty()) {
                    table(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StopwatchControls(
    state: StopwatchUiState,
    horizontalActions: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit,
) {
    val running = state.status == Stopwatch.State.RUNNING
    val stopped = state.status == Stopwatch.State.STOPPED
    val toggleLabel = stringResource(
        when (state.status) {
            Stopwatch.State.STOPPED -> R.string.stopwatch_start
            Stopwatch.State.PAUSED -> R.string.stopwatch_resume
            Stopwatch.State.RUNNING -> R.string.stopwatch_pause
        }
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val style = MaterialTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum")
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle)
                    .heightIn(min = if (horizontalActions) 88.dp else 112.dp)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.totalTime.formatStopwatchTime(state.useLongerMSFormat),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = style,
                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = style.fontSize),
                )
            }
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val toggle: @Composable (Modifier) -> Unit = { modifier ->
                Button(
                    onClick = onToggle,
                    shapes = ButtonDefaults.shapes(),
                    modifier = modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    if (!horizontalActions) {
                        Icon(
                            painter = painterResource(if (running) CommonsR.drawable.ic_pause_vector else CommonsR.drawable.ic_play_vector),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text(toggleLabel, textAlign = TextAlign.Center)
                }
            }
            val relatedActions: @Composable RowScope.() -> Unit = {
                TonalAction(stringResource(R.string.stopwatch_reset), onReset, Modifier.weight(1f))
                if (running) {
                    TonalAction(stringResource(R.string.lap), onLap, Modifier.weight(1f))
                }
            }
            val groupModifier = Modifier.fillMaxWidth().padding(8.dp)
            if (horizontalActions && !stopped) {
                Row(groupModifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    toggle(Modifier.weight(1.35f))
                    relatedActions()
                }
            } else {
                Column(groupModifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    toggle(Modifier.fillMaxWidth())
                    if (!stopped) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = relatedActions)
                    }
                }
            }
        }
    }
}

@Composable
private fun TonalAction(label: String, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LapHeader(sorting: Int, onSort: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SortHeader(stringResource(R.string.lap), SORT_BY_LAP, sorting, 1f, onSort)
        SortHeader(stringResource(R.string.stopwatch_lap_time), SORT_BY_LAP_TIME, sorting, 2f, onSort)
        SortHeader(stringResource(R.string.stopwatch_total_time), SORT_BY_TOTAL_TIME, sorting, 2f, onSort)
    }
}

@Composable
private fun RowScope.SortHeader(
    label: String,
    column: Int,
    sorting: Int,
    weight: Float,
    onSort: (Int) -> Unit,
) {
    val active = sorting and column != 0
    val descending = sorting and SORT_DESCENDING != 0
    val direction = stringResource(if (descending) CommonsR.string.descending else CommonsR.string.ascending)
    val text = if (active) label + if (descending) " ↓" else " ↑" else label
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.weight(weight)
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (active) colors.secondaryContainer else colors.surfaceContainerLow)
            .clickable(role = Role.Button) { onSort(column) }
            .semantics { if (active) stateDescription = direction }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun LazyListScope.lapRows(laps: List<StopwatchLap>, onSort: (Int) -> Unit) {
    items(laps, key = { it.id }) { lap -> LapRow(lap, onSort) }
}

@Composable
private fun LapRow(lap: StopwatchLap, onSort: (Int) -> Unit) {
    val live = lap.id == STOPWATCH_LIVE_LAP_ID
    val colors = MaterialTheme.colorScheme
    val locales = ConfigurationCompat.getLocales(LocalConfiguration.current)
    val numberFormat = remember(locales) {
        NumberFormat.getIntegerInstance(locales[0] ?: Locale.getDefault()).apply {
            isGroupingUsed = false
        }
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (live) colors.tertiaryContainer else colors.surfaceContainer,
        contentColor = if (live) colors.onTertiaryContainer else colors.onSurface,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LapCell(numberFormat.format(lap.number), SORT_BY_LAP, 1f, textStyle, onSort)
            LapCell(lap.lapTime.formatStopwatchTime(false), SORT_BY_LAP_TIME, 2f, textStyle, onSort)
            LapCell(lap.totalTime.formatStopwatchTime(false), SORT_BY_TOTAL_TIME, 2f, textStyle, onSort)
        }
    }
}

@Composable
private fun RowScope.LapCell(
    value: String,
    column: Int,
    weight: Float,
    style: TextStyle,
    onSort: (Int) -> Unit,
) {
    Box(
        modifier = Modifier.weight(weight)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { onSort(column) }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            style = style,
            maxLines = 1,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = style.fontSize),
        )
    }
}
