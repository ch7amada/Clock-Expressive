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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.fossify.clock.R
import org.fossify.clock.ui.components.CardBounceState
import org.fossify.clock.ui.components.SelectionBar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.text.DecimalFormatSymbols
import java.util.Locale
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
    val gridState = rememberLazyGridState()
    val cardBounce = remember { CardBounceState() }
    val haptics = LocalHapticFeedback.current
    val bottomPadding = if (selecting) 16.dp else 104.dp
    val reorderState = rememberReorderableLazyGridState(
        lazyGridState = gridState,
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
            val columns = if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f) 1 else 2
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = gutter,
                            top = 12.dp,
                            end = if (useSideAction) gutter + 96.dp else gutter,
                            bottom = if (useSideAction) 16.dp else bottomPadding,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(alarms.orEmpty(), key = { _, alarm -> alarm.id }) { index, alarm ->
                            ReorderableItem(
                                state = reorderState,
                                key = alarm.id,
                                animateItemModifier = Modifier.animateItem(placementSpec = placement),
                            ) { isDragging ->
                                AlarmRow(
                                    alarm = alarm,
                                    modifier = cardBounce.modifier(alarm.enabled, index, columns),
                                    selected = alarm.id in selectedIds,
                                    selecting = selecting,
                                    isDragging = isDragging,
                                    compactHeight = compactHeight,
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

                    if (!selecting) {
                        MediumFloatingActionButton(
                            onClick = onAdd,
                            modifier = if (useSideAction) {
                                Modifier.align(Alignment.TopEnd).padding(end = gutter, top = 12.dp)
                            } else {
                                Modifier.align(Alignment.BottomEnd).padding(end = gutter, bottom = 16.dp)
                            },
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ) {
                            Icon(
                                painter = painterResource(CommonsR.drawable.ic_plus_vector),
                                contentDescription = stringResource(R.string.add_alarm),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmListItem,
    modifier: Modifier,
    selected: Boolean,
    selecting: Boolean,
    isDragging: Boolean,
    compactHeight: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val color by animateColorAsState(
        targetValue = when {
            selected -> colors.secondaryContainer
            alarm.enabled -> colors.primary
            else -> colors.surfaceContainerHigh
        },
        animationSpec = motion.fastEffectsSpec(),
        label = "Alarm row color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else Color.Transparent,
        animationSpec = motion.fastEffectsSpec(),
        label = "Alarm row outline",
    )
    val corner by animateDpAsState(
        targetValue = if (selected) 18.dp else 32.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "Alarm row corner",
    )
    val contentColor = when {
        selected -> colors.onSecondaryContainer
        alarm.enabled -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }
    val alarmDescription = stringResource(CommonsR.string.alarm)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = corner.coerceAtLeast(0.dp),
            topEnd = 14.dp,
            bottomEnd = corner.coerceAtLeast(0.dp),
            bottomStart = 14.dp,
        ),
        color = color,
        contentColor = contentColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isDragging) 6.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = if (compactHeight) 132.dp else 246.dp)
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (alarm.isNext) {
                    Icon(
                        painterResource(R.drawable.ic_alarm_filled_vector),
                        contentDescription = stringResource(R.string.upcoming_alarm),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(text = alarm.days, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            AlarmTimeReadout(alarm.time, alarm.enabled, compactHeight)
            if (alarm.label.isNotEmpty()) {
                Text(text = alarm.label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "$alarmDescription ${alarm.time}" },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primary,
                        checkedTrackColor = colors.onPrimary,
                        checkedBorderColor = colors.primary,
                    ),
                )
                if (selecting) dragHandle()
            }
        }
    }
}

@Composable
private fun AlarmTimeReadout(formattedTime: String, enabled: Boolean, compactHeight: Boolean) {
    val time = formattedTime.substringBefore(' ')
    val hours = time.substringBefore(':').padStart(2, DecimalFormatSymbols.getInstance().zeroDigit)
    val minutes = time.substringAfter(':')
    val period = formattedTime.substringAfter(' ', missingDelimiterValue = "").uppercase(Locale.getDefault())
    val numerals = MaterialTheme.typography.displayLargeEmphasized.copy(
        fontFeatureSettings = "tnum",
        fontWeight = if (enabled) FontWeight.ExtraBold else FontWeight.Light,
        lineHeight = 0.87.em,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
            mode = LineHeightStyle.Mode.Tight,
        ),
        letterSpacing = 0.sp,
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = formattedTime },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = hours,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = if (compactHeight) 80.sp else 190.sp),
                style = numerals.copy(textGeometricTransform = TextGeometricTransform(scaleX = 0.28f)),
            )
            Column(
                modifier = Modifier.weight(1f, fill = false).padding(top = if (compactHeight) 2.dp else 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = minutes,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = if (compactHeight) 50.sp else 116.sp),
                    style = numerals.copy(textGeometricTransform = TextGeometricTransform(scaleX = 0.44f)),
                )
                if (period.isNotEmpty()) {
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelLarge.copy(lineHeight = TextUnit.Unspecified),
                        fontSize = if (compactHeight) 14.sp else 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarms(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = stringResource(R.string.no_alarms_found),
            modifier = Modifier.padding(32.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
