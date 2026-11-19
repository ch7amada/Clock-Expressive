package org.fossify.clock.ui.clock

import android.icu.text.TimeZoneFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.fossify.clock.R
import org.fossify.clock.ui.components.SelectionBar
import org.fossify.clock.extensions.getFormattedDate
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.helpers.FORMAT_12H_WITH_SECONDS
import org.fossify.clock.helpers.FORMAT_24H_WITH_SECONDS
import org.fossify.clock.models.MyTimeZone
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.fossify.commons.R as CommonsR

@Composable
internal fun ClockScreen(
    clockTime: LongState,
    clockMinute: Long,
    localTimeZone: TimeZone,
    use24HourFormat: Boolean,
    nextAlarm: String,
    timeZones: List<MyTimeZone>,
    selectedIds: Set<Int>,
    onAdd: () -> Unit,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val listState = rememberLazyListState()
    val selecting = selectedIds.isNotEmpty()
    val locale = LocalConfiguration.current.locales[0]
    val minuteTime = clockMinute * 60_000L
    val localDate = remember(localTimeZone, clockMinute) {
        Calendar.getInstance(localTimeZone).apply { timeInMillis = minuteTime }
    }
    val dateFormat = remember(locale, localTimeZone) {
        SimpleDateFormat("EEE, d MMM", locale).apply { timeZone = localTimeZone }
    }
    val date = remember(dateFormat, clockMinute) { dateFormat.format(Date(minuteTime)) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
            val gutter = if (maxWidth < 400.dp) 16.dp else 24.dp
            val compactHeight = maxHeight < 320.dp
            val sideBySide = maxWidth >= 700.dp && maxWidth > maxHeight && (timeZones.isNotEmpty() || nextAlarm.isNotEmpty())
            val hero: @Composable () -> Unit = {
                LocalClock(
                    clockTime, localDate, date, localTimeZone, use24HourFormat,
                    nextAlarm = if (sideBySide) "" else nextAlarm,
                    compactHeight = compactHeight,
                )
            }
            Column(Modifier.widthIn(max = if (sideBySide) 1080.dp else 680.dp).fillMaxSize()) {
                if (selecting) {
                    SelectionBar(
                        count = selectedIds.size,
                        onClose = onClearSelection,
                        onDelete = onDelete,
                        modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp),
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (sideBySide) {
                            Column(
                                Modifier.weight(1f).fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 12.dp, bottom = 16.dp),
                            ) {
                                hero()
                            }
                        }
                        ZoneList(
                            state = listState,
                            timeZones = timeZones,
                            selectedIds = selectedIds,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .padding(end = if (sideBySide && !selecting) 72.dp else 0.dp),
                            hero = if (sideBySide) null else hero,
                            nextAlarm = if (sideBySide) nextAlarm else "",
                            row = { zone ->
                                val readout = zoneReadout(zone.zoneName, minuteTime, localDate, use24HourFormat)
                                ZoneRow(
                                    title = zone.title,
                                    offset = readout.offset,
                                    time = readout.time,
                                    date = readout.date,
                                    selected = zone.id in selectedIds,
                                    selecting = selecting,
                                    onClick = { onClick(zone.id) },
                                    onLongClick = { onLongClick(zone.id) },
                                )
                            },
                        )
                    }
                    if (!selecting) {
                        FloatingActionButton(
                            onClick = onAdd,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = gutter, bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Icon(painterResource(CommonsR.drawable.ic_plus_vector), stringResource(R.string.time_zone))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneList(
    state: LazyListState,
    timeZones: List<MyTimeZone>,
    selectedIds: Set<Int>,
    modifier: Modifier,
    hero: (@Composable () -> Unit)?,
    nextAlarm: String,
    row: @Composable (MyTimeZone) -> Unit,
) {
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = if (selectedIds.isEmpty()) 88.dp else 16.dp),
    ) {
        if (hero != null) {
            item(key = "local_clock", contentType = "local_clock") {
                Box(Modifier.padding(bottom = 20.dp)) { hero() }
            }
        }
        if (hero == null) {
            item(key = "next_alarm") {
                NextAlarm(nextAlarm, Modifier.padding(bottom = 12.dp))
            }
        }
        items(timeZones, key = { it.id }, contentType = { "time_zone" }) { zone ->
            Column(Modifier.animateItem(placementSpec = placement)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                row(zone)
            }
        }
        if (timeZones.isNotEmpty()) {
            item(key = "zone_ledger_end") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun LocalClock(
    clockTime: LongState,
    localDate: Calendar,
    date: String,
    timeZone: TimeZone,
    use24HourFormat: Boolean,
    nextAlarm: String,
    compactHeight: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 60.dp, topEnd = 22.dp, bottomEnd = 60.dp, bottomStart = 22.dp),
            color = colors.primary,
            contentColor = colors.onPrimary,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (compactHeight) 16.dp else 30.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 6.dp else 12.dp),
            ) {
                LocalTime(
                    clockTime,
                    timeZone,
                    use24HourFormat,
                    maxFontSize = if (compactHeight) 82.sp else 96.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomEnd = 48.dp,
                        bottomStart = 16.dp,
                    ),
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            date,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmallEmphasized,
                        )
                        MinuteDial(localDate, Modifier.size(if (compactHeight) 52.dp else 64.dp))
                    }
                }
            }
        }
        NextAlarm(nextAlarm, Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun NextAlarm(nextAlarm: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val alarmMotion = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    AnimatedVisibility(
        visible = nextAlarm.isNotEmpty(),
        enter = expandVertically(animationSpec = alarmMotion, expandFrom = Alignment.Top),
        exit = shrinkVertically(animationSpec = alarmMotion, shrinkTowards = Alignment.Top),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 30.dp, bottomEnd = 22.dp, bottomStart = 16.dp),
            color = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
        ) {
            Row(
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_alarm_vector),
                    stringResource(CommonsR.string.alarm),
                    Modifier.size(20.dp),
                )
                Text(nextAlarm, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun MinuteDial(localDate: Calendar, modifier: Modifier = Modifier) {
    val indexColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.34f)
    val handColor = MaterialTheme.colorScheme.onPrimaryContainer
    val minute = localDate.get(Calendar.MINUTE)
    val hour = localDate.get(Calendar.HOUR) + minute / 60f
    Canvas(modifier) {
        val radius = size.minDimension * 0.41f
        fun dialPoint(turn: Float, distance: Float): Offset {
            val angle = (turn - 0.25f) * 2 * PI
            return center + Offset(cos(angle).toFloat() * distance, sin(angle).toFloat() * distance)
        }
        repeat(12) { index ->
            val tick = index / 12f
            drawLine(
                indexColor,
                dialPoint(tick, radius - if (index % 3 == 0) 5.dp.toPx() else 3.dp.toPx()),
                dialPoint(tick, radius),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawLine(handColor, center, dialPoint(hour / 12f, radius * 0.54f), 3.dp.toPx(), StrokeCap.Round)
        drawLine(handColor, center, dialPoint(minute / 60f, radius), 2.dp.toPx(), StrokeCap.Round)
        drawCircle(handColor, radius = 2.5.dp.toPx())
    }
}

@Composable
private fun LocalTime(
    clockTime: LongState,
    timeZone: TimeZone,
    use24HourFormat: Boolean,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale, timeZone, use24HourFormat) {
        val pattern = if (use24HourFormat) FORMAT_24H_WITH_SECONDS else FORMAT_12H_WITH_SECONDS
        SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }
    }
    val fullTime = format.format(Date(clockTime.longValue))
    val secondsStart = fullTime.indexOf(':', startIndex = fullTime.indexOf(':') + 1)
    val time = buildAnnotatedString {
        append(fullTime)
        if (secondsStart >= 0) {
            addStyle(SpanStyle(fontSize = 0.32.em, letterSpacing = 0.em), secondsStart, fullTime.length)
        }
    }
    val style = MaterialTheme.typography.displayLargeEmphasized.copy(
        fontSize = maxFontSize,
        lineHeight = 1.06.em,
        fontFeatureSettings = "tnum",
        letterSpacing = (-0.045).em,
    )
    Text(
        text = time,
        modifier = modifier,
        style = style,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize),
        textAlign = TextAlign.Center,
    )
}

private data class ZoneReadout(val time: String, val offset: String, val date: String?)

@Composable
private fun zoneReadout(
    zoneName: String,
    minuteTime: Long,
    localDate: Calendar,
    use24HourFormat: Boolean,
): ZoneReadout {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context, locale, zoneName, minuteTime, localDate, use24HourFormat) {
        val zone = TimeZone.getTimeZone(zoneName)
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = minuteTime }
        val seconds = (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)) * 60
        val sameDay = calendar.get(Calendar.YEAR) == localDate.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == localDate.get(Calendar.DAY_OF_YEAR)
        ZoneReadout(
            time = context.getFormattedTime(seconds, showSeconds = false, makeAmPmSmaller = false).toString(),
            offset = TimeZoneFormat.getInstance(locale).formatOffsetLocalizedGMT(zone.getOffset(minuteTime)),
            date = if (sameDay) null else context.getFormattedDate(calendar),
        )
    }
}

@Composable
private fun ZoneRow(
    title: String,
    offset: String,
    time: String,
    date: String?,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) colors.tertiaryContainer else colors.surface,
        contentColor = if (selected) colors.onTertiaryContainer else colors.onSurface,
    ) {
        BoxWithConstraints(
            Modifier
                .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            val stacked = maxWidth < 224.dp || LocalDensity.current.fontScale >= 1.3f
            val label: @Composable (Modifier) -> Unit = {
                Column(it, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        offset,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) colors.onTertiaryContainer else colors.tertiary,
                    )
                }
            }
            val readout: @Composable (Alignment.Horizontal) -> Unit = { alignment ->
                Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        time,
                        style = MaterialTheme.typography.titleLargeEmphasized.copy(fontFeatureSettings = "tnum"),
                        textAlign = if (stacked) TextAlign.Start else TextAlign.End,
                    )
                    if (date != null) {
                        Text(
                            date,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = if (stacked) TextAlign.Start else TextAlign.End,
                        )
                    }
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    label(Modifier)
                    readout(Alignment.Start)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    label(Modifier.weight(1f))
                    readout(Alignment.End)
                }
            }
        }
    }
}
