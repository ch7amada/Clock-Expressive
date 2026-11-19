package org.fossify.clock.ui.clock

import android.icu.text.TimeZoneFormat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.fossify.clock.R
import org.fossify.clock.extensions.getFormattedDate
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.helpers.FORMAT_12H_WITH_SECONDS
import org.fossify.clock.helpers.FORMAT_24H_WITH_SECONDS
import org.fossify.clock.models.MyTimeZone
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
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
    val hero: @Composable () -> Unit = {
        LocalClock(clockTime, date, localTimeZone, use24HourFormat, nextAlarm)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
            val gutter = if (maxWidth < 400.dp) 16.dp else 24.dp
            val sideBySide = maxWidth >= 700.dp && maxWidth > maxHeight && timeZones.isNotEmpty()
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
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            hero = if (sideBySide) null else hero,
                            row = { zone, modifier ->
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
                                    modifier = modifier,
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
    row: @Composable (MyTimeZone, Modifier) -> Unit,
) {
    val placement = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = if (selectedIds.isEmpty()) 88.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hero != null) {
            item(key = "local_clock", contentType = "local_clock") { hero() }
        }
        items(timeZones, key = { it.id }, contentType = { "time_zone" }) { zone ->
            row(zone, Modifier.animateItem(placementSpec = placement))
        }
    }
}

@Composable
private fun LocalClock(
    clockTime: LongState,
    date: String,
    timeZone: TimeZone,
    use24HourFormat: Boolean,
    nextAlarm: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(date, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            LocalTime(clockTime, timeZone, use24HourFormat)
            if (nextAlarm.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_alarm_vector),
                            stringResource(CommonsR.string.alarm),
                            Modifier.size(18.dp),
                        )
                        Text(nextAlarm, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTime(clockTime: LongState, timeZone: TimeZone, use24HourFormat: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale, timeZone, use24HourFormat) {
        val pattern = if (use24HourFormat) FORMAT_24H_WITH_SECONDS else FORMAT_12H_WITH_SECONDS
        SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }
    }
    val periodFormat = remember(locale, timeZone) {
        SimpleDateFormat("a", locale).apply { this.timeZone = timeZone }
    }
    val now = Date(clockTime.longValue)
    val fullTime = format.format(now)
    val period = if (use24HourFormat) "" else periodFormat.format(now)
    val digits = fullTime.removeSuffix(period).trimEnd()
    val style = MaterialTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum")
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val separatePeriod = period.isNotEmpty() && (maxWidth < 272.dp || LocalDensity.current.fontScale >= 1.3f)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) {},
        ) {
            val time = buildAnnotatedString {
                append(digits)
                if (period.isNotEmpty() && !separatePeriod) {
                    withStyle(SpanStyle(fontSize = 0.4.em)) { append(" $period") }
                }
            }
            Text(
                text = time,
                style = style,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize),
                textAlign = TextAlign.Center,
            )
            if (separatePeriod) {
                Text(period, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
    }
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
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
    ) {
        BoxWithConstraints(
            Modifier
                .then(if (selecting) Modifier.semantics { this.selected = selected } else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val stacked = maxWidth < 224.dp || LocalDensity.current.fontScale >= 1.3f
            val label: @Composable (Modifier) -> Unit = {
                Column(it, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(offset, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val readout: @Composable (Alignment.Horizontal) -> Unit = { alignment ->
                Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        time,
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
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

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit, modifier: Modifier) {
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
