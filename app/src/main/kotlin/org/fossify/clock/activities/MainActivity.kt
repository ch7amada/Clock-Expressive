package org.fossify.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.clock.BuildConfig
import org.fossify.clock.R
import org.fossify.clock.adapters.ViewPagerAdapter
import org.fossify.clock.databinding.ActivityMainBinding
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getEnabledAlarms
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.INVALID_TIMER_ID
import org.fossify.clock.helpers.OPEN_TAB
import org.fossify.clock.helpers.PICK_AUDIO_FILE_INTENT_ID
import org.fossify.clock.helpers.STOPWATCH_SHORTCUT_ID
import org.fossify.clock.helpers.STOPWATCH_TOGGLE_ACTION
import org.fossify.clock.helpers.TABS_COUNT
import org.fossify.clock.helpers.TAB_ALARM
import org.fossify.clock.helpers.TAB_ALARM_INDEX
import org.fossify.clock.helpers.TAB_CLOCK
import org.fossify.clock.helpers.TAB_CLOCK_INDEX
import org.fossify.clock.helpers.TAB_STOPWATCH
import org.fossify.clock.helpers.TAB_STOPWATCH_INDEX
import org.fossify.clock.helpers.TAB_TIMER
import org.fossify.clock.helpers.TAB_TIMER_INDEX
import org.fossify.clock.helpers.TIMER_ID
import org.fossify.clock.helpers.TOGGLE_STOPWATCH
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.convertToBitmap
import org.fossify.commons.extensions.launchMoreAppsFromUsIntent
import org.fossify.commons.extensions.onPageChangeListener
import org.fossify.commons.extensions.shortcutManager
import org.fossify.commons.extensions.storeNewYourAlarmSound
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.LICENSE_AUTOFITTEXTVIEW
import org.fossify.commons.helpers.LICENSE_NUMBER_PICKER
import org.fossify.commons.helpers.LICENSE_RTL
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.FAQItem
import java.time.temporal.WeekFields
import java.util.Locale

class MainActivity : SimpleActivity() {
    private var selectedTab by mutableIntStateOf(TAB_CLOCK_INDEX)
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        // Commons pads the host horizontally for side navigation bars and cutouts.
        setupEdgeToEdge()

        initFragments()
        setupShell()
        updateWidgets()
        migrateFirstDayOfWeek()
        ensureBackgroundThread {
            alarmController.rescheduleEnabledAlarms()
        }

        getEnabledAlarms { enabledAlarms ->
            if (!enabledAlarms.isNullOrEmpty()) {
                handleFullScreenNotificationsPermission {
                    if (!it) {
                        toast(org.fossify.commons.R.string.notifications_disabled)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        checkShortcuts()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedTab = binding.viewPager.currentItem
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (config.lastHandledShortcutColor != appIconColor) {
            val stopWatchShortcutInfo = getLaunchStopwatchShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = listOf(stopWatchShortcutInfo)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getLaunchStopwatchShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.start_stopwatch)
        val drawable = resources.getDrawable(R.drawable.shortcut_stopwatch)
        (drawable as LayerDrawable)
            .findDrawableByLayerId(R.id.shortcut_stopwatch_background)
            .applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, SplashActivity::class.java).apply {
            putExtra(OPEN_TAB, TAB_STOPWATCH)
            putExtra(TOGGLE_STOPWATCH, true)
            action = STOPWATCH_TOGGLE_ACTION
        }

        return ShortcutInfo.Builder(this, STOPWATCH_SHORTCUT_ID)
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    override fun onPause() {
        super.onPause()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        config.lastUsedViewPagerPage = binding.viewPager.currentItem
    }

    private fun setupShell() {
        binding.mainHeader.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ClockTheme { MainHeader() } }
        }
        binding.mainNavigation.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ClockTheme { MainNavigation() } }
        }
    }

    @Composable
    private fun MainHeader() {
        val colors = MaterialTheme.colorScheme
        val titleStyle = MaterialTheme.typography.headlineSmallEmphasized
        val titleHeight = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.clock),
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = {
                if (selectedTab == TAB_ALARM_INDEX || selectedTab == TAB_TIMER_INDEX) {
                    IconButton(onClick = ::showSortDialog) {
                        Icon(
                            painter = painterResource(org.fossify.commons.R.drawable.ic_sort_vector),
                            contentDescription = stringResource(org.fossify.commons.R.string.sort_by),
                        )
                    }
                }
                MainOverflowMenu()
            },
            expandedHeight = maxOf(64.dp, titleHeight + 16.dp),
            windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Top),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.surface,
                titleContentColor = colors.onSurface,
                actionIconContentColor = colors.onSurface,
            ),
        )
    }

    @Composable
    private fun MainOverflowMenu() {
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    painter = painterResource(org.fossify.commons.R.drawable.ic_three_dots_vector),
                    contentDescription = stringResource(org.fossify.commons.R.string.more_options),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(org.fossify.commons.R.string.settings)) },
                    onClick = {
                        expanded = false
                        launchSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(org.fossify.commons.R.string.about)) },
                    onClick = {
                        expanded = false
                        launchAbout()
                    },
                )
                if (resources.getBoolean(org.fossify.commons.R.bool.is_google_play_build)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(org.fossify.commons.R.string.more_fossify_apps)) },
                        onClick = {
                            expanded = false
                            launchMoreAppsFromUsIntent()
                        },
                    )
                }
            }
        }
    }

    private fun showSortDialog() {
        when (binding.viewPager.currentItem) {
            TAB_ALARM_INDEX -> getViewPagerAdapter()?.showAlarmSortDialog()
            TAB_TIMER_INDEX -> getViewPagerAdapter()?.showTimerSortDialog()
        }
    }

    @Composable
    private fun MainNavigation() {
        val colors = MaterialTheme.colorScheme
        val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
            .union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom)
        Surface(color = colors.surface) {
            ShortNavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(bottomInsets)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(MaterialTheme.shapes.large)
                    // Keep the width bounded without passing a full-width minimum to each item.
                    .wrapContentWidth(),
                containerColor = colors.surfaceContainerHigh,
                contentColor = colors.onSurface,
                windowInsets = WindowInsets(0),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                val itemColors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimaryContainer,
                    selectedTextColorTopIconPosition = colors.onSurface,
                    selectedIndicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                )
                MainTab.entries.forEachIndexed { index, tab ->
                    val selected = index == selectedTab
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = { binding.viewPager.currentItem = index },
                        icon = {
                            Icon(
                                painter = painterResource(if (selected) tab.filledIcon else tab.outlineIcon),
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.label),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp),
                            )
                        },
                        iconPosition = NavigationItemIconPosition.Top,
                        colors = itemColors,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.extras?.containsKey(OPEN_TAB) == true) {
            val tabToOpen = intent.getIntExtra(OPEN_TAB, TAB_CLOCK)
            binding.viewPager.setCurrentItem(getTabIndex(tabToOpen), false)
            if (tabToOpen == TAB_TIMER) {
                val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
                (binding.viewPager.adapter as ViewPagerAdapter).updateTimerPosition(timerId)
            }
            if (tabToOpen == TAB_STOPWATCH) {
                if (intent.getBooleanExtra(TOGGLE_STOPWATCH, false)) {
                    (binding.viewPager.adapter as ViewPagerAdapter).startStopWatch()
                }
            }
        }
        super.onNewIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        when {
            requestCode == PICK_AUDIO_FILE_INTENT_ID && resultCode == RESULT_OK && resultData != null -> {
                storeNewAlarmSound(resultData)
            }
        }
    }

    private fun storeNewAlarmSound(resultData: Intent) {
        val newAlarmSound = storeNewYourAlarmSound(resultData)

        when (binding.viewPager.currentItem) {
            TAB_ALARM_INDEX -> getViewPagerAdapter()?.updateAlarmTabAlarmSound(newAlarmSound)
            TAB_TIMER_INDEX -> getViewPagerAdapter()?.updateTimerTabAlarmSound(newAlarmSound)
        }
    }

    fun updateClockTabAlarm() {
        getViewPagerAdapter()?.updateClockTabAlarm()
    }

    private fun getViewPagerAdapter() = binding.viewPager.adapter as? ViewPagerAdapter

    private fun initFragments() {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.onPageChangeListener { selectedTab = it }

        val tabToOpen = intent.getIntExtra(OPEN_TAB, config.defaultTab)
        intent.removeExtra(OPEN_TAB)
        if (tabToOpen == TAB_TIMER) {
            val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
            viewPagerAdapter.updateTimerPosition(timerId)
        }

        if (tabToOpen == TAB_STOPWATCH) {
            config.toggleStopwatch = intent.getBooleanExtra(TOGGLE_STOPWATCH, false)
        }

        binding.viewPager.offscreenPageLimit = TABS_COUNT - 1
        binding.viewPager.currentItem = getTabIndex(tabToOpen)
        selectedTab = binding.viewPager.currentItem
    }

    private enum class MainTab(val label: Int, val outlineIcon: Int, val filledIcon: Int) {
        CLOCK(R.string.clock, R.drawable.ic_clock_vector, R.drawable.ic_clock_filled_vector),
        ALARM(
            org.fossify.commons.R.string.alarm,
            R.drawable.ic_alarm_vector,
            R.drawable.ic_alarm_filled_vector,
        ),
        STOPWATCH(R.string.stopwatch, R.drawable.ic_stopwatch_vector, R.drawable.ic_stopwatch_filled_vector),
        TIMER(R.string.timer, R.drawable.ic_hourglass_vector, R.drawable.ic_hourglass_filled_vector),
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses =
            LICENSE_NUMBER_PICKER or LICENSE_RTL or LICENSE_AUTOFITTEXTVIEW

        val faqItems = arrayListOf(
            FAQItem(
                title = R.string.faq_1_title,
                text = R.string.faq_1_text
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_1_title_commons,
                text = org.fossify.commons.R.string.faq_1_text_commons
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_4_title_commons,
                text = org.fossify.commons.R.string.faq_4_text_commons
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_9_title_commons,
                text = org.fossify.commons.R.string.faq_9_text_commons
            )
        )

        if (resources.getBoolean(org.fossify.commons.R.bool.is_google_play_build)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startAboutActivity(
            appNameId = R.string.app_name,
            licenseMask = licenses,
            versionName = BuildConfig.VERSION_NAME,
            faqItems = faqItems,
            showFAQBeforeMail = true
        )
    }

    @Deprecated("Remove this method in future releases")
    private fun migrateFirstDayOfWeek() {
        if (config.migrateFirstDayOfWeek) {
            config.migrateFirstDayOfWeek = false
            config.firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek.value
        }
    }

    private fun getTabIndex(tabId: Int): Int {
        return when (tabId) {
            TAB_CLOCK -> TAB_CLOCK_INDEX
            TAB_ALARM -> TAB_ALARM_INDEX
            TAB_STOPWATCH -> TAB_STOPWATCH_INDEX
            TAB_TIMER -> TAB_TIMER_INDEX
            else -> config.lastUsedViewPagerPage
        }
    }
}
