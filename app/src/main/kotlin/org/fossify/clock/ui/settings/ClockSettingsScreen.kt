package org.fossify.clock.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.clock.R
import org.fossify.clock.helpers.TAB_ALARM
import org.fossify.clock.helpers.TAB_CLOCK
import org.fossify.clock.helpers.TAB_STOPWATCH
import org.fossify.clock.helpers.TAB_TIMER
import org.fossify.clock.ui.components.ClockSwitch
import org.fossify.clock.ui.dialogs.SettingsChoice
import org.fossify.clock.ui.dialogs.SettingsChoiceDialog
import org.fossify.commons.helpers.TAB_LAST_USED
import java.time.DayOfWeek
import org.fossify.commons.R as CommonsR

internal data class ClockSettingsState(
    val showPurchaseThankYou: Boolean,
    val language: String?,
    val useEnglish: Boolean?,
    val use24HourFormat: Boolean,
    val defaultTab: Int,
    val firstDayOfWeek: Int,
    val keepAwake: Boolean,
    val alarmReminder: String,
    val increaseVolume: Boolean,
    val useSameSnooze: Boolean,
    val snoozeDuration: String,
    val timerReminder: String,
)

private enum class ChoiceMenu { DefaultTab, FirstDayOfWeek }

private val DefaultTabChoices = listOf(
    SettingsChoice(TAB_CLOCK, R.string.clock),
    SettingsChoice(TAB_ALARM, CommonsR.string.alarm),
    SettingsChoice(TAB_STOPWATCH, R.string.stopwatch),
    SettingsChoice(TAB_TIMER, R.string.timer),
    SettingsChoice(TAB_LAST_USED, CommonsR.string.last_used_tab),
)

private val FirstDayOfWeekChoices = listOf(
    SettingsChoice(DayOfWeek.SUNDAY.value, CommonsR.string.sunday),
    SettingsChoice(DayOfWeek.MONDAY.value, CommonsR.string.monday),
    SettingsChoice(DayOfWeek.TUESDAY.value, CommonsR.string.tuesday),
    SettingsChoice(DayOfWeek.WEDNESDAY.value, CommonsR.string.wednesday),
    SettingsChoice(DayOfWeek.THURSDAY.value, CommonsR.string.thursday),
    SettingsChoice(DayOfWeek.FRIDAY.value, CommonsR.string.friday),
    SettingsChoice(DayOfWeek.SATURDAY.value, CommonsR.string.saturday),
)

@Composable
internal fun ClockSettingsScreen(
    state: ClockSettingsState,
    onBack: () -> Unit,
    onCustomizeColors: () -> Unit,
    onCustomizeWidgetColors: () -> Unit,
    onPurchaseThankYou: () -> Unit,
    onChangeLanguage: () -> Unit,
    onUseEnglish: (Boolean) -> Unit,
    onUse24HourFormat: (Boolean) -> Unit,
    onSelectDefaultTab: (Int) -> Unit,
    onSelectFirstDayOfWeek: (Int) -> Unit,
    onKeepAwake: (Boolean) -> Unit,
    onAlarmReminder: () -> Unit,
    onIncreaseVolume: (Boolean) -> Unit,
    onUseSameSnooze: (Boolean) -> Unit,
    onSnoozeDuration: () -> Unit,
    onTimerReminder: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var choiceMenu by rememberSaveable { mutableStateOf<ChoiceMenu?>(null) }
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Bottom),
        topBar = { SettingsTopBar(onBack) },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val gutter = if (maxWidth < 360.dp) 16.dp else 24.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppearancePanel(onCustomizeColors, onCustomizeWidgetColors)
                SettingsSection(CommonsR.string.general_settings) {
                    if (state.showPurchaseThankYou) {
                        SettingsAction(
                            CommonsR.string.purchase_simple_thank_you,
                            stringResource(CommonsR.string.enables_more_customization),
                            onClick = onPurchaseThankYou,
                        )
                        SettingsDivider()
                    }
                    state.language?.let {
                        SettingsAction(CommonsR.string.language, it, onClick = onChangeLanguage)
                        SettingsDivider()
                    }
                    state.useEnglish?.let {
                        SettingsToggle(
                            CommonsR.string.use_english_language,
                            it,
                            onUseEnglish,
                        )
                        SettingsDivider()
                    }
                    SettingsToggle(
                        CommonsR.string.use_24_hour_time_format,
                        state.use24HourFormat,
                        onUse24HourFormat,
                    )
                    SettingsDivider()
                    val defaultTab = DefaultTabChoices.firstOrNull { it.value == state.defaultTab } ?: DefaultTabChoices.last()
                    SettingsAction(
                        CommonsR.string.default_tab_to_open,
                        stringResource(defaultTab.label),
                    ) { choiceMenu = ChoiceMenu.DefaultTab }
                    SettingsDivider()
                    val weekdays = stringArrayResource(CommonsR.array.week_days)
                    SettingsAction(
                        CommonsR.string.start_week_on,
                        weekdays.getOrNull(state.firstDayOfWeek - 1),
                    ) { choiceMenu = ChoiceMenu.FirstDayOfWeek }
                    SettingsDivider()
                    SettingsToggle(
                        CommonsR.string.prevent_phone_from_sleeping,
                        state.keepAwake,
                        onKeepAwake,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsHeading(R.string.alarm_tab, colors.secondary, emphasized = true)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SettingsConnectedRow(colors.secondaryContainer, colors.onSecondaryContainer, first = true) {
                            SettingsAction(
                                R.string.max_reminder_duration,
                                state.alarmReminder,
                                summaryColor = colors.onSecondaryContainer,
                                onClick = onAlarmReminder,
                            )
                        }
                        SettingsConnectedRow(colors.secondaryContainer, colors.onSecondaryContainer) {
                            SettingsToggle(R.string.increase_volume_gradually, state.increaseVolume, onIncreaseVolume)
                        }
                        SettingsConnectedRow(
                            colors.secondaryContainer,
                            colors.onSecondaryContainer,
                            last = !state.useSameSnooze,
                        ) {
                            SettingsToggle(CommonsR.string.use_same_snooze, state.useSameSnooze, onUseSameSnooze)
                        }
                        if (state.useSameSnooze) {
                            SettingsConnectedRow(colors.secondaryContainer, colors.onSecondaryContainer, last = true) {
                                SettingsAction(
                                    CommonsR.string.snooze_time,
                                    state.snoozeDuration,
                                    summaryColor = colors.onSecondaryContainer,
                                    onClick = onSnoozeDuration,
                                )
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsHeading(R.string.timer_tab, colors.tertiary, emphasized = true)
                    SettingsConnectedRow(colors.tertiaryContainer, colors.onTertiaryContainer, first = true, last = true) {
                        SettingsAction(
                            R.string.max_reminder_duration,
                            state.timerReminder,
                            summaryColor = colors.onTertiaryContainer,
                            onClick = onTimerReminder,
                        )
                    }
                }
                SettingsSection(CommonsR.string.migrating) {
                    SettingsAction(R.string.settings_export_data, onClick = onExport)
                    SettingsDivider()
                    SettingsAction(R.string.settings_import_data, onClick = onImport)
                }
            }
        }
    }

    when (choiceMenu) {
        ChoiceMenu.DefaultTab -> SettingsChoiceDialog(
            title = stringResource(CommonsR.string.default_tab_to_open),
            options = DefaultTabChoices,
            selectedValue = state.defaultTab,
            onDismiss = { choiceMenu = null },
            onSelect = onSelectDefaultTab,
        )
        ChoiceMenu.FirstDayOfWeek -> SettingsChoiceDialog(
            title = stringResource(CommonsR.string.start_week_on),
            options = FirstDayOfWeekChoices,
            selectedValue = state.firstDayOfWeek,
            onDismiss = { choiceMenu = null },
            onSelect = onSelectFirstDayOfWeek,
        )
        null -> Unit
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val titleStyle = MaterialTheme.typography.headlineSmallEmphasized
    val titleHeight = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
    TopAppBar(
        title = {
            Text(
                stringResource(CommonsR.string.settings),
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

@Composable
private fun SettingsSection(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsHeading(title, MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsHeading(@StringRes title: Int, color: Color, emphasized: Boolean = false) {
    Text(
        text = stringResource(title),
        modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
        style = if (emphasized) {
            MaterialTheme.typography.titleLargeEmphasized
        } else {
            MaterialTheme.typography.titleMediumEmphasized
        },
        color = color,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearancePanel(onCustomizeColors: () -> Unit, onCustomizeWidgetColors: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(CommonsR.string.color_customization),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Surface(shape = MaterialShapes.Cookie6Sided.toShape(), color = colors.primary, contentColor = colors.onPrimary) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(CommonsR.drawable.ic_edit_vector), contentDescription = null)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                AppearanceAction(CommonsR.string.customize_colors, first = true, onClick = onCustomizeColors)
                AppearanceAction(CommonsR.string.customize_widget_colors, first = false, onClick = onCustomizeWidgetColors)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearanceAction(@StringRes label: Int, first: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = SettingsRowModifier,
        shapes = ButtonDefaults.shapes(
            shape = connectedSettingsShape(first = first, last = !first),
            pressedShape = RoundedCornerShape(12.dp),
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(label),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
    }
}

@Composable
private fun SettingsConnectedRow(
    color: Color,
    contentColor: Color,
    first: Boolean = false,
    last: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = connectedSettingsShape(first, last),
        color = color,
        contentColor = contentColor,
        content = content,
    )
}

private fun connectedSettingsShape(first: Boolean, last: Boolean): RoundedCornerShape {
    val top = if (first) 24.dp else 6.dp
    val bottom = if (last) 24.dp else 6.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}

private val SettingsRowModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)

@Composable
private fun SettingsAction(
    @StringRes label: Int,
    summary: String? = null,
    summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Column(
        modifier = SettingsRowModifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        summary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = summaryColor,
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = SettingsRowModifier
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        ClockSwitch(checked = checked, onCheckedChange = null)
    }
}
