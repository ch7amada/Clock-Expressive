package org.fossify.clock.ui.timer

import android.animation.ValueAnimator
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    selectedIds: Set<Int>,
    timerIdToScrollTo: Int?,
    animatePlacement: Boolean,
    onAdd: () -> Unit,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onReset: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    onScrollComplete: (Int) -> Unit,
) {
    val selecting = selectedIds.isNotEmpty()
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val bottomPadding = if (selecting) 16.dp else 88.dp
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
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
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
                        EmptyTimers(Modifier.align(Alignment.Center).padding(horizontal = gutter))
                    }
                    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = gutter,
                            top = 12.dp,
                            end = gutter,
                            bottom = bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                    selected = timer.id in selectedIds,
                                    selecting = selecting,
                                    isDragging = isDragging,
                                    onClick = { onClick(timer.id) },
                                    onLongClick = { onLongClick(timer.id) },
                                    onToggle = { onToggle(timer.id) },
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
                        val addDescription = stringResource(R.string.new_timer)
                        ExtendedFloatingActionButton(
                            text = { Text(addDescription, Modifier.clearAndSetSemantics {}) },
                            icon = { Icon(painterResource(CommonsR.drawable.ic_plus_vector), contentDescription = null) },
                            onClick = onAdd,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = gutter, bottom = 16.dp)
                                .semantics {
                                    contentDescription = addDescription
                                    role = Role.Button
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerRow(
    timer: TimerListItem,
    selected: Boolean,
    selecting: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val active = timer.running || timer.paused
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (active) colors.primaryContainer else colors.surfaceContainerLow,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurface,
        border = if (selected) BorderStroke(2.dp, if (active) colors.onPrimaryContainer else colors.primary) else null,
        shadowElevation = if (isDragging) 6.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = 100.dp)
                .padding(start = 20.dp, top = 12.dp, end = if (selecting) 4.dp else 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = timer.time,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 36.sp),
                    style = MaterialTheme.typography.displaySmallEmphasized.copy(fontFeatureSettings = "tnum"),
                )
                if (timer.label.isNotEmpty()) {
                    Text(
                        text = timer.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (timer.canReset) {
                val resetDescription = stringResource(R.string.stopwatch_reset)
                Surface(
                    onClick = onReset,
                    modifier = Modifier.size(48.dp).semantics {
                        contentDescription = resetDescription
                        role = Role.Button
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) colors.surface else colors.surfaceContainerHigh,
                    contentColor = colors.onSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(CommonsR.drawable.ic_restart_alt_vector),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            TimerPlayPause(timer, onToggle)
            if (selecting) dragHandle()
        }
    }
}

@Composable
private fun TimerPlayPause(timer: TimerListItem, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = timer.running || timer.paused
    val toggleDescription = stringResource(
        when {
            timer.running -> R.string.stopwatch_pause
            timer.paused -> R.string.stopwatch_resume
            else -> R.string.stopwatch_start
        },
    )
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        timer.progress?.let { progress ->
            val stroke = Stroke(width = with(LocalDensity.current) { 2.dp.toPx() }, cap = StrokeCap.Round)
            val color = if (active) colors.onPrimaryContainer else colors.primary
            CircularWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .size(64.dp)
                    .clearAndSetSemantics { hideFromAccessibility() },
                color = color,
                trackColor = color.copy(alpha = 0.25f),
                stroke = stroke,
                trackStroke = stroke,
                gapSize = 2.dp,
                amplitude = { 0.5f },
                wavelength = 15.dp,
                waveSpeed = if (timer.running && ValueAnimator.areAnimatorsEnabled()) 12.dp else 0.dp,
            )
        }
        Surface(
            onClick = onToggle,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = toggleDescription
                role = Role.Button
            },
            shape = if (timer.running) MaterialTheme.shapes.large else CircleShape,
            color = colors.primary,
            contentColor = colors.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(
                        if (timer.running) CommonsR.drawable.ic_pause_vector else CommonsR.drawable.ic_play_vector,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyTimers(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_hourglass_vector),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.no_timers_found),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
