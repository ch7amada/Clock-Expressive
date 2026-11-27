package org.fossify.clock.ui.timer

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.clock.R
import org.fossify.clock.ui.components.SelectionBar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import org.fossify.commons.R as CommonsR

@Immutable
internal data class TimerListItem(
    val id: Int,
    val time: String,
    val label: String,
    val running: Boolean,
    val paused: Boolean,
    val canReset: Boolean,
    val progress: Float?,
)

@Composable
internal fun TimerScreen(
    timers: List<TimerListItem>?,
    pendingResetIds: Set<Int>,
    selectedIds: Set<Int>,
    timerIdToScrollTo: Int?,
    animatePlacement: Boolean,
    onAdd: () -> Unit,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onToggle: (TimerListItem) -> Unit,
    onReset: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    onScrollComplete: (Int) -> Unit,
) {
    val selecting = selectedIds.isNotEmpty()
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val bottomPadding = if (selecting) 16.dp else 104.dp
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = PaddingValues(bottom = bottomPadding),
    ) { from, to ->
        onMove(from.key as Int, to.key as Int)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    val targetIndex = timers.orEmpty().indexOfFirst { it.id == timerIdToScrollTo }
    LaunchedEffect(timerIdToScrollTo, targetIndex) {
        if (timerIdToScrollTo != null && targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
            onScrollComplete(timerIdToScrollTo)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
            val gutter = if (maxWidth < 400.dp) 16.dp else 24.dp
            val compactLandscape = maxWidth >= 480.dp && maxHeight < 280.dp
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.timer),
                    modifier = Modifier.padding(
                        start = gutter,
                        top = if (compactLandscape) 8.dp else 32.dp,
                        end = gutter,
                        bottom = if (compactLandscape) 4.dp else 16.dp,
                    ).semantics { heading() },
                    style = if (compactLandscape) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.displaySmallEmphasized,
                )
                if (selecting) {
                    SelectionBar(
                        count = selectedIds.size,
                        onClose = onClearSelection,
                        onDelete = onDelete,
                        modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp),
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    if (timers?.isEmpty() == true) {
                        EmptyTimers(
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = gutter)
                                .padding(bottom = if (compactLandscape) 0.dp else 56.dp),
                        )
                    }
                    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = gutter,
                            top = if (compactLandscape) 4.dp else 12.dp,
                            end = if (compactLandscape && !selecting) gutter + 88.dp else gutter,
                            bottom = if (compactLandscape && !selecting) 12.dp else bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(timers.orEmpty(), key = { it.id }) { timer ->
                            ReorderableItem(
                                state = reorderState,
                                key = timer.id,
                                animateItemModifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    placementSpec = placement.takeIf { animatePlacement },
                                    fadeOutSpec = null,
                                ),
                            ) { isDragging ->
                                TimerRow(
                                    timer = timer,
                                    compactLandscape = compactLandscape,
                                    selected = timer.id in selectedIds,
                                    selecting = selecting,
                                    isDragging = isDragging,
                                    controlsEnabled = timer.id !in pendingResetIds,
                                    onClick = { onClick(timer.id) },
                                    onLongClick = { onLongClick(timer.id) },
                                    onToggle = { onToggle(timer) },
                                    onReset = { onReset(timer.id) },
                                    dragHandle = {
                                        val actions = buildList {
                                            timers?.firstOrNull()?.id?.takeIf { it != timer.id }?.let { firstId ->
                                                add(CustomAccessibilityAction(stringResource(CommonsR.string.move_to_top)) {
                                                    onMove(timer.id, firstId)
                                                    true
                                                })
                                            }
                                            timers?.lastOrNull()?.id?.takeIf { it != timer.id }?.let { lastId ->
                                                add(CustomAccessibilityAction(stringResource(CommonsR.string.move_to_bottom)) {
                                                    onMove(timer.id, lastId)
                                                    true
                                                })
                                            }
                                        }
                                        val description = stringResource(CommonsR.string.reorder_items_by_dragging)
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .draggableHandle()
                                                .semantics {
                                                    contentDescription = description
                                                    customActions = actions
                                                }
                                                .focusable(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(CommonsR.drawable.ic_drag_handle_vector),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (!selecting) {
                        MediumFloatingActionButton(
                            onClick = onAdd,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = gutter, bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ) {
                            Icon(
                                painter = painterResource(CommonsR.drawable.ic_plus_vector),
                                contentDescription = stringResource(R.string.new_timer),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerRow(
    timer: TimerListItem,
    compactLandscape: Boolean,
    selected: Boolean,
    selecting: Boolean,
    isDragging: Boolean,
    controlsEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val active = timer.canReset
    val motion = MaterialTheme.motionScheme
    val stationCorner by animateDpAsState(
        targetValue = if (timer.running) 48.dp else 20.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "Timer station corner",
    )
    val stationColor = when {
        timer.running -> colors.tertiary
        active -> colors.tertiaryContainer
        selected -> colors.secondaryContainer
        else -> colors.surfaceContainerLow
    }
    val controlBandOpacity by animateFloatAsState(
        targetValue = if (timer.running) 0.18f else 0.10f,
        animationSpec = motion.fastEffectsSpec(),
        label = "Timer control band",
    )
    val stationContent = when {
        timer.running -> colors.onTertiary
        active -> colors.onTertiaryContainer
        selected -> colors.onSecondaryContainer
        else -> colors.onSurface
    }
    val toggleDescription = stringResource(
        when {
            timer.running -> R.string.stopwatch_pause
            timer.paused -> R.string.stopwatch_resume
            else -> R.string.stopwatch_start
        },
    )
    val toggleIcon = if (timer.running) CommonsR.drawable.ic_pause_vector else CommonsR.drawable.ic_play_vector
    val rowModifier = Modifier
        .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = if (active) {
            RoundedCornerShape(topStart = stationCorner, topEnd = 12.dp, bottomEnd = stationCorner, bottomStart = 12.dp)
        } else {
            RoundedCornerShape(8.dp)
        },
        color = stationColor,
        contentColor = stationContent,
        border = when {
            selected -> BorderStroke(2.dp, stationContent)
            !active -> BorderStroke(1.dp, colors.outlineVariant)
            else -> null
        },
        shadowElevation = if (isDragging) 6.dp else 0.dp,
    ) {
        if (active) {
            val controls: @Composable () -> Unit = {
                TimerControl(
                    icon = CommonsR.drawable.ic_restart_alt_vector,
                    description = stringResource(R.string.stopwatch_reset),
                    enabled = controlsEnabled,
                    color = stationColor,
                    contentColor = stationContent,
                    shape = RoundedCornerShape(12.dp),
                    onClick = onReset,
                )
                TimerControl(
                    icon = toggleIcon,
                    description = toggleDescription,
                    enabled = controlsEnabled,
                    color = stationContent,
                    contentColor = stationColor,
                    shape = if (timer.running) RoundedCornerShape(16.dp) else CircleShape,
                    width = 72.dp,
                    onClick = onToggle,
                )
                if (selecting) dragHandle()
            }
            val bandColor = stationContent.copy(alpha = controlBandOpacity)
            if (compactLandscape) {
                Column(rowModifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            TimerReadout(timer, active = true, compact = true)
                        }
                        Row(
                            modifier = Modifier.background(bandColor, RoundedCornerShape(16.dp)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            controls()
                        }
                    }
                    timer.progress?.let { TimerProgress(it, timer.running, height = 18.dp) }
                }
            } else {
                Column(rowModifier) {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 156.dp)
                            .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    ) {
                        TimerReadout(timer, active = true)
                        timer.progress?.let { TimerProgress(it, timer.running) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().background(bandColor).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        controls()
                    }
                }
            }
        } else {
            Row(
                modifier = rowModifier.heightIn(min = 80.dp).padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    TimerReadout(timer, active = false)
                }
                TimerControl(
                    icon = toggleIcon,
                    description = toggleDescription,
                    enabled = controlsEnabled,
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    shape = CircleShape,
                    onClick = onToggle,
                )
                if (selecting) dragHandle()
            }
        }
    }
}

@Composable
private fun TimerReadout(timer: TimerListItem, active: Boolean, compact: Boolean = false) {
    val typography = MaterialTheme.typography
    if (timer.label.isNotEmpty()) {
        Text(
            text = timer.label,
            style = if (active) typography.titleMedium else typography.bodyMedium,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val maxFontSize = when {
        compact -> 36.sp
        active -> 88.sp
        else -> 30.sp
    }
    val timeStyle = when {
        compact -> typography.displaySmallEmphasized
        active -> typography.displayLargeEmphasized
        else -> typography.headlineSmallEmphasized
    }
    Text(
        text = timer.time,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = maxFontSize),
        style = timeStyle.copy(fontFeatureSettings = "tnum"),
    )
}

@Composable
private fun TimerProgress(progress: Float, running: Boolean, height: Dp = 22.dp) {
    val color = LocalContentColor.current
    val stroke = with(LocalDensity.current) { Stroke(6.dp.toPx(), cap = StrokeCap.Round) }
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(height).clearAndSetSemantics { hideFromAccessibility() },
        color = color,
        trackColor = color.copy(alpha = 0.32f),
        stroke = stroke,
        trackStroke = stroke,
        gapSize = 4.dp,
        stopSize = 4.dp,
        amplitude = { 0.65f },
        wavelength = 32.dp,
        waveSpeed = if (running && ValueAnimator.areAnimatorsEnabled()) 32.dp else 0.dp,
    )
}

@Composable
private fun TimerControl(
    icon: Int,
    description: String,
    enabled: Boolean,
    color: Color,
    contentColor: Color,
    shape: Shape,
    onClick: () -> Unit,
    width: Dp = 48.dp,
) {
    val disabledColor = LocalContentColor.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width, 48.dp).semantics { contentDescription = description },
        shapes = ButtonDefaults.shapes(shape = shape, pressedShape = RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = disabledColor.copy(alpha = 0.12f),
            disabledContentColor = disabledColor.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun EmptyTimers(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_hourglass_vector),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(R.string.no_timers_found),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
