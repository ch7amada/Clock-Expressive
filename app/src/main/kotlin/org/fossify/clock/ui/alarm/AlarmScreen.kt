package org.fossify.clock.ui.alarm

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.clock.R
import org.fossify.clock.ui.components.ClockSwitch
import org.fossify.clock.ui.components.SelectionBar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import org.fossify.commons.R as CommonsR

@Immutable
internal data class AlarmListItem(
    val id: Int,
    val time: String,
    val days: String,
    val label: String,
    val enabled: Boolean,
    val isNext: Boolean,
)

@Composable
internal fun AlarmScreen(
    alarms: List<AlarmListItem>?,
    selectedIds: Set<Int>,
    onAdd: () -> Unit,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
            val compactHeight = maxHeight < 320.dp
            val gutter = if (maxWidth < 400.dp) 16.dp else 24.dp
            val wideCompact = compactHeight && maxWidth >= 600.dp
            val useSideAction = wideCompact && !selecting
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
                    if (alarms?.isEmpty() == true) {
                        EmptyAlarms(Modifier.align(Alignment.Center).padding(horizontal = gutter))
                    }
                    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = gutter,
                            top = 12.dp,
                            end = if (useSideAction) gutter + 72.dp else gutter,
                            bottom = if (useSideAction) 16.dp else bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 10.dp),
                    ) {
                        items(alarms.orEmpty(), key = { it.id }) { alarm ->
                            ReorderableItem(
                                state = reorderState,
                                key = alarm.id,
                                animateItemModifier = Modifier.animateItem(placementSpec = placement),
                            ) { isDragging ->
                                AlarmRow(
                                    alarm = alarm,
                                    selected = alarm.id in selectedIds,
                                    selecting = selecting,
                                    isDragging = isDragging,
                                    compactHeight = compactHeight,
                                    showSideHeading = wideCompact,
                                    onClick = { onClick(alarm.id) },
                                    onLongClick = { onLongClick(alarm.id) },
                                    onToggle = { onToggle(alarm.id, it) },
                                    dragHandle = {
                                        val actions = buildList {
                                            alarms?.firstOrNull()?.id?.takeIf { it != alarm.id }?.let { firstId ->
                                                add(CustomAccessibilityAction(stringResource(CommonsR.string.move_to_top)) {
                                                    onMove(alarm.id, firstId)
                                                    true
                                                })
                                            }
                                            alarms?.lastOrNull()?.id?.takeIf { it != alarm.id }?.let { lastId ->
                                                add(CustomAccessibilityAction(stringResource(CommonsR.string.move_to_bottom)) {
                                                    onMove(alarm.id, lastId)
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

                    if (useSideAction) {
                        FloatingActionButton(
                            onClick = onAdd,
                            modifier = Modifier.align(Alignment.TopEnd).padding(end = gutter, top = 12.dp),
                        ) {
                            Icon(
                                painter = painterResource(CommonsR.drawable.ic_plus_vector),
                                contentDescription = stringResource(R.string.add_alarm),
                            )
                        }
                    } else if (!selecting) {
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(R.string.add_alarm)) },
                            icon = { Icon(painterResource(CommonsR.drawable.ic_plus_vector), contentDescription = null) },
                            onClick = onAdd,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = gutter, bottom = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmListItem,
    selected: Boolean,
    selecting: Boolean,
    isDragging: Boolean,
    compactHeight: Boolean,
    showSideHeading: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val color by animateColorAsState(
        targetValue = when {
            alarm.isNext -> colors.primaryContainer
            selected -> colors.secondaryContainer
            alarm.enabled -> colors.surfaceContainerLow
            else -> colors.surface
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "Alarm row color",
    )
    val controlColor by animateColorAsState(
        targetValue = when {
            alarm.isNext -> colors.surface
            selected -> colors.surfaceContainerHighest
            alarm.enabled -> colors.surfaceContainerHigh
            else -> Color.Transparent
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "Alarm switch background",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> colors.primary
            !alarm.enabled -> colors.outlineVariant
            else -> Color.Transparent
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "Alarm row outline",
    )
    val leadingCorner by animateDpAsState(
        targetValue = when {
            alarm.isNext -> 48.dp
            selected -> 24.dp
            else -> 12.dp
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "Alarm row leading corner",
    )
    val trailingCorner by animateDpAsState(
        targetValue = when {
            alarm.isNext -> 14.dp
            selected -> 12.dp
            else -> 30.dp
        },
        animationSpec = motion.fastSpatialSpec(),
        label = "Alarm row trailing corner",
    )
    val contentColor = when {
        alarm.isNext -> colors.onPrimaryContainer
        selected -> colors.onSecondaryContainer
        alarm.enabled -> colors.onSurface
        else -> colors.onSurfaceVariant
    }
    val alarmDescription = stringResource(CommonsR.string.alarm)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (alarm.isNext) 0.dp else 12.dp),
        shape = RoundedCornerShape(
            topStart = leadingCorner,
            topEnd = trailingCorner,
            bottomEnd = if (alarm.isNext) leadingCorner else trailingCorner,
            bottomStart = if (alarm.isNext) trailingCorner else leadingCorner,
        ),
        color = color,
        contentColor = contentColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isDragging) 6.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            verticalAlignment = Alignment.Top,
        ) {
            if (alarm.isNext && showSideHeading) {
                UpcomingAlarmHeading(Modifier.widthIn(max = 184.dp))
            }
            Column(Modifier.weight(1f)) {
                if (alarm.isNext && !showSideHeading) {
                    UpcomingAlarmHeading(Modifier.fillMaxWidth())
                }
                Row(
                    modifier = Modifier
                        .heightIn(min = if (alarm.isNext && !compactHeight) 124.dp else 64.dp)
                        .padding(
                            start = if (alarm.isNext) 20.dp else 14.dp,
                            end = if (selecting) 6.dp else 12.dp,
                            top = if (alarm.isNext && !compactHeight) 10.dp else 6.dp,
                            bottom = if (alarm.isNext && !compactHeight) 14.dp else 6.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = if (alarm.isNext && showSideHeading) Alignment.Top else Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val maxTimeSize = when {
                            alarm.isNext && !compactHeight -> 68.sp
                            alarm.isNext -> 50.sp
                            else -> 32.sp
                        }
                        Text(
                            text = alarm.time,
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = maxTimeSize),
                            style = if (alarm.isNext) {
                                MaterialTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum")
                            } else {
                                MaterialTheme.typography.headlineLargeEmphasized.copy(fontFeatureSettings = "tnum")
                            },
                        )
                        if (alarm.label.isNotEmpty()) {
                            Text(text = alarm.label, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = alarm.days,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (alarm.isNext || selected) contentColor else colors.onSurfaceVariant,
                        )
                    }

                    Surface(color = controlColor, shape = CircleShape) {
                        ClockSwitch(
                            checked = alarm.enabled,
                            onCheckedChange = onToggle,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { contentDescription = "$alarmDescription ${alarm.time}" },
                        )
                    }
                    if (selecting) {
                        dragHandle()
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingAlarmHeading(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, color = colors.primary, contentColor = colors.onPrimary) {
        Text(
            text = stringResource(R.string.upcoming_alarm),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 32.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
        )
    }
}

@Composable
private fun EmptyAlarms(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alarm_vector),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.no_alarms_found),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
