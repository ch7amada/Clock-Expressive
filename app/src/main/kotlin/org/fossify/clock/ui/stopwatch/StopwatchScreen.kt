package org.fossify.clock.ui.stopwatch

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
    val hasLiveLap = state.laps.any { it.id == STOPWATCH_LIVE_LAP_ID }
    val liveLapAlpha = animateFloatAsState(
        targetValue = if (hasLiveLap) 1f else 0f,
        animationSpec = if (hasLiveLap) MaterialTheme.motionScheme.defaultEffectsSpec() else snap(),
        label = "Current lap insertion",
    )
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
        val compactHeight = maxHeight < (328 + 152 * LocalDensity.current.fontScale).dp
        val controls: @Composable () -> Unit = {
            StopwatchControls(state, wideLandscape || compactHeight, onToggle, onReset, recordLap)
        }
        val table: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier) {
                LapHeader(state.sorting, changeSorting)
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
                ) {
                    lapRows(state.laps, changeSorting, liveLapAlpha)
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
            LazyColumn(
                modifier = Modifier.widthIn(max = 536.dp).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = gutter, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "controls") { controls() }
                if (state.laps.isNotEmpty()) {
                    item(key = "lap_header") {
                        LapHeader(state.sorting, changeSorting)
                    }
                    lapRows(state.laps, changeSorting, liveLapAlpha)
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
    compactFace: Boolean,
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
    val motion = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val broadTarget = when (state.status) {
        Stopwatch.State.STOPPED -> if (compactFace) 30.dp else 60.dp
        Stopwatch.State.RUNNING -> if (compactFace) 12.dp else 22.dp
        Stopwatch.State.PAUSED -> if (compactFace) 24.dp else 44.dp
    }
    val tightTarget = when (state.status) {
        Stopwatch.State.STOPPED -> if (compactFace) 12.dp else 22.dp
        Stopwatch.State.RUNNING -> if (compactFace) 30.dp else 60.dp
        Stopwatch.State.PAUSED -> if (compactFace) 10.dp else 16.dp
    }
    val broadCorner by animateDpAsState(broadTarget, motion, label = "Stopwatch top-start and bottom-end corners")
    val tightCorner by animateDpAsState(tightTarget, motion, label = "Stopwatch top-end and bottom-start corners")
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val style = MaterialTheme.typography.displayLargeEmphasized.copy(
            fontSize = if (compactFace) 60.sp else 88.sp,
            fontFeatureSettings = "tnum",
            letterSpacing = (-0.045).em,
        )
        Surface(
            shape = RoundedCornerShape(
                topStart = broadCorner.coerceAtLeast(0.dp),
                topEnd = tightCorner.coerceAtLeast(0.dp),
                bottomEnd = broadCorner.coerceAtLeast(0.dp),
                bottomStart = tightCorner.coerceAtLeast(0.dp),
            ),
            color = if (running) colors.tertiaryContainer else colors.primaryContainer,
            contentColor = if (running) colors.onTertiaryContainer else colors.onPrimaryContainer,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle)
                    .heightIn(min = if (compactFace) 100.dp else 196.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val formattedTime = state.totalTime.formatStopwatchTime(state.useLongerMSFormat)
                val time = remember(formattedTime) {
                    buildAnnotatedString {
                        append(formattedTime)
                        val fractionStart = formattedTime.lastIndexOf('.')
                        if (fractionStart >= 0) {
                            addStyle(SpanStyle(fontSize = 0.54.em), fractionStart, formattedTime.length)
                        }
                    }
                }
                Text(
                    text = time,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = style,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize),
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val connected = !stopped && maxWidth >= 312.dp && LocalDensity.current.fontScale <= 1.3f
            val toggle: @Composable (Modifier, CornerBasedShape) -> Unit = { modifier, shape ->
                Button(
                    onClick = onToggle,
                    shapes = ButtonDefaults.shapes(shape = shape, pressedShape = RoundedCornerShape(14.dp)),
                    modifier = modifier.heightIn(min = 56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(if (running) CommonsR.drawable.ic_pause_vector else CommonsR.drawable.ic_play_vector),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(toggleLabel, textAlign = TextAlign.Center)
                }
            }
            val relatedActions: @Composable RowScope.() -> Unit = {
                val resetShape = when {
                    connected && running -> RoundedCornerShape(8.dp)
                    connected -> RoundedCornerShape(topStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 8.dp)
                    else -> RoundedCornerShape(28.dp)
                }
                TonalAction(stringResource(R.string.stopwatch_reset), onReset, Modifier.weight(1f), resetShape)
                if (running) {
                    val lapShape = if (connected) {
                        RoundedCornerShape(topStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 8.dp)
                    } else {
                        RoundedCornerShape(28.dp)
                    }
                    TonalAction(stringResource(R.string.lap), onLap, Modifier.weight(1f), lapShape)
                }
            }
            if (connected) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    toggle(
                        Modifier.weight(1.55f),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 28.dp),
                    )
                    relatedActions()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    toggle(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp))
                    if (!stopped) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = relatedActions)
                    }
                }
            }
        }
    }
}

@Composable
private fun TonalAction(label: String, onClick: () -> Unit, modifier: Modifier, shape: CornerBasedShape) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(shape = shape, pressedShape = RoundedCornerShape(14.dp)),
        modifier = modifier.heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LapHeader(sorting: Int, onSort: (Int) -> Unit) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 6.dp, bottomStart = 6.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            .background(if (active) colors.secondaryContainer else colors.surfaceContainerHigh)
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

private fun LazyListScope.lapRows(laps: List<StopwatchLap>, onSort: (Int) -> Unit, liveLapAlpha: State<Float>) {
    items(laps, key = { it.id }) { lap ->
        val modifier = if (lap.id == STOPWATCH_LIVE_LAP_ID) {
            Modifier.graphicsLayer { alpha = liveLapAlpha.value }
        } else {
            Modifier
        }
        LapRow(lap, onSort, modifier)
    }
}

@Composable
private fun LapRow(lap: StopwatchLap, onSort: (Int) -> Unit, modifier: Modifier) {
    val live = lap.id == STOPWATCH_LIVE_LAP_ID
    val colors = MaterialTheme.colorScheme
    val locales = ConfigurationCompat.getLocales(LocalConfiguration.current)
    val numberFormat = remember(locales) {
        NumberFormat.getIntegerInstance(locales[0] ?: Locale.getDefault()).apply {
            isGroupingUsed = false
        }
    }
    val lapStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    val totalStyle = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum")
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
    Surface(
        modifier = modifier,
        shape = if (live) shape else RoundedCornerShape(0.dp),
        color = if (live) colors.tertiaryContainer else colors.surface,
        contentColor = if (live) colors.onTertiaryContainer else colors.onSurface,
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LapCell(
                    value = numberFormat.format(lap.number),
                    column = SORT_BY_LAP,
                    weight = 1f,
                    style = totalStyle,
                    onSort = onSort,
                    textModifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (live) colors.tertiary else colors.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    color = if (live) colors.onTertiary else colors.onSurface,
                )
                LapCell(lap.lapTime.formatStopwatchTime(false), SORT_BY_LAP_TIME, 2f, lapStyle, onSort)
                LapCell(lap.totalTime.formatStopwatchTime(false), SORT_BY_TOTAL_TIME, 2f, totalStyle, onSort)
            }
            if (!live) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
            }
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
    textModifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
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
            modifier = textModifier,
            color = color,
            style = style,
            maxLines = 1,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = style.fontSize),
        )
    }
}
