package org.fossify.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import org.fossify.clock.BuildConfig
import org.fossify.clock.R
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getEnabledAlarms
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.fragments.AlarmFragment
import org.fossify.clock.fragments.ClockFragment
import org.fossify.clock.fragments.StopwatchFragment
import org.fossify.clock.fragments.TimerFragment
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
import org.fossify.commons.extensions.shortcutManager
import org.fossify.commons.extensions.storeNewYourAlarmSound
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import java.time.temporal.WeekFields
import java.util.Locale

class MainActivity : SimpleActivity() {
    private lateinit var pagerState: PagerState
    private val selectedTab get() = pagerState.settledPage
    private var pendingTab: TabRequest? = null
    private val tabLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
            deliverTabRequest()
        }

        override fun onFragmentPaused(fm: FragmentManager, fragment: Fragment) {
            when (fragment) {
                is AlarmFragment -> if (selectedTab != TAB_ALARM_INDEX) fragment.clearSelection()
                is TimerFragment -> if (selectedTab != TAB_TIMER_INDEX) fragment.clearSelection()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPager(savedInstanceState)
        setContent { ClockTheme(window = window) { MainScreen() } }
        appLaunched(BuildConfig.APPLICATION_ID)
        // Commons pads the host horizontally for side navigation bars and cutouts.
        setupEdgeToEdge()

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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_TAB_STATE, selectedTab)
        pendingTab?.let { request ->
            outState.putInt(PENDING_TAB_STATE, request.page)
            outState.putInt(TIMER_ID, request.timerId)
            outState.putBoolean(TOGGLE_STOPWATCH, request.startStopwatch)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(tabLifecycleCallbacks)
        super.onDestroy()
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
        config.lastUsedViewPagerPage = selectedTab
    }

    @Composable
    private fun MainScreen() {
        val scope = rememberCoroutineScope()
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                MainHeader()
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = TABS_COUNT - 1,
                    // AndroidFragment derives its container ID from the composition key.
                    key = { MainTab.entries[it].name },
                ) { page ->
                    val maxLifecycle = if (page == selectedTab) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
                    val modifier = Modifier.fillMaxSize()
                    when (page) {
                        TAB_CLOCK_INDEX -> AndroidFragment<ClockFragment>(modifier, maxLifecycle = maxLifecycle)
                        TAB_ALARM_INDEX -> AndroidFragment<AlarmFragment>(modifier, maxLifecycle = maxLifecycle)
                        TAB_STOPWATCH_INDEX -> AndroidFragment<StopwatchFragment>(
                            modifier, maxLifecycle = maxLifecycle, onUpdate = { deliverTabRequest() },
                        )
                        TAB_TIMER_INDEX -> AndroidFragment<TimerFragment>(
                            modifier, maxLifecycle = maxLifecycle, onUpdate = { deliverTabRequest() },
                        )
                    }
                }
                MainNavigation { page ->
                    if (page != pagerState.currentPage || pagerState.isScrollInProgress) {
                        scope.launch { pagerState.animateScrollToPage(page) }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainHeader() {
        val tab = MainTab.entries[selectedTab]
        val hasSort = tab == MainTab.ALARM || tab == MainTab.TIMER
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val motion = MaterialTheme.motionScheme
        val colors = MaterialTheme.colorScheme
        val headerInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout)
            .union(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(headerInsets)
                .padding(start = 24.dp, end = 24.dp, bottom = if (landscape) 2.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = colors.surfaceContainerHigh,
                    toolbarContentColor = colors.onSurface,
                ),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = CircleShape,
                expandedShadowElevation = 2.dp,
                collapsedShadowElevation = 2.dp,
                leadingContent = {
                    AnimatedVisibility(
                        visible = hasSort,
                        enter = expandHorizontally(motion.fastSpatialSpec(), expandFrom = Alignment.End) +
                            fadeIn(motion.fastEffectsSpec()),
                        exit = shrinkHorizontally(motion.fastSpatialSpec(), shrinkTowards = Alignment.End) +
                            fadeOut(motion.fastEffectsSpec()),
                    ) {
                        IconButton(onClick = ::showSortDialog, enabled = hasSort) {
                            Icon(
                                painter = painterResource(org.fossify.commons.R.drawable.ic_sort_vector),
                                contentDescription = stringResource(org.fossify.commons.R.string.sort_by),
                            )
                        }
                    }
                },
            ) {
                MainOverflowMenu()
            }
        }
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
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 200.dp),
                offset = DpOffset(0.dp, 12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                val itemModifier = Modifier.heightIn(min = 56.dp)
                val itemPadding = PaddingValues(horizontal = 20.dp)
                DropdownMenuItem(
                    modifier = itemModifier,
                    contentPadding = itemPadding,
                    text = { Text(stringResource(org.fossify.commons.R.string.settings)) },
                    onClick = {
                        expanded = false
                        launchSettings()
                    },
                )
                DropdownMenuItem(
                    modifier = itemModifier,
                    contentPadding = itemPadding,
                    text = { Text(stringResource(org.fossify.commons.R.string.about)) },
                    onClick = {
                        expanded = false
                        launchAbout()
                    },
                )
                if (resources.getBoolean(org.fossify.commons.R.bool.is_google_play_build)) {
                    DropdownMenuItem(
                        modifier = itemModifier,
                        contentPadding = itemPadding,
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
        when (selectedTab) {
            TAB_ALARM_INDEX -> mountedTab<AlarmFragment>()?.showSortingDialog()
            TAB_TIMER_INDEX -> mountedTab<TimerFragment>()?.showSortingDialog()
        }
    }

    @Composable
    private fun MainNavigation(onTabClick: (Int) -> Unit) {
        val colors = MaterialTheme.colorScheme
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dockShape = CircleShape
        val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
            .union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(bottomInsets)
                .padding(horizontal = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 24.dp, vertical = if (landscape) 4.dp else 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            ShortNavigationBar(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .shadow(3.dp, dockShape)
                    .clip(dockShape)
                    // Keep the width bounded without passing a full-width minimum to each item.
                    .wrapContentWidth(),
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurface,
                windowInsets = WindowInsets(0),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                MainTab.entries.forEachIndexed { index, tab ->
                    val selected = index == selectedTab
                    val (containerColor, contentColor) = tabAccent(tab)
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = { onTabClick(index) },
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
                        iconPosition = if (landscape) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                        colors = ShortNavigationBarItemDefaults.colors(
                            selectedIconColor = contentColor,
                            selectedTextColorTopIconPosition = colors.onSurface,
                            selectedTextColorStartIconPosition = contentColor,
                            selectedIndicatorColor = containerColor,
                            unselectedIconColor = colors.onSurfaceVariant,
                            unselectedTextColor = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }

    @Composable
    private fun tabAccent(tab: MainTab) = with(MaterialTheme.colorScheme) {
        when (tab) {
            MainTab.CLOCK -> primary to onPrimary
            MainTab.ALARM -> secondary to onSecondary
            MainTab.STOPWATCH -> secondary to onSecondary
            MainTab.TIMER -> tertiary to onTertiary
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(OPEN_TAB)) {
            val request = readTabRequest(intent)
            intent.removeExtra(OPEN_TAB)
            pendingTab = request
            pagerState.requestScrollToPage(request.page)
            deliverTabRequest()
        }
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

        when (selectedTab) {
            TAB_ALARM_INDEX -> mountedTab<AlarmFragment>()?.updateAlarmSound(newAlarmSound)
            TAB_TIMER_INDEX -> mountedTab<TimerFragment>()?.updateAlarmSound(newAlarmSound)
        }
    }

    fun updateClockTabAlarm() {
        mountedTab<ClockFragment>()?.updateAlarm()
    }

    private inline fun <reified T : Fragment> mountedTab(): T? =
        supportFragmentManager.fragments.filterIsInstance<T>().firstOrNull { it.view?.parent != null }

    private fun setupPager(savedInstanceState: Bundle?) {
        // The XML pager added its tab fragments without the Compose hosts' tags.
        val legacyTabs = supportFragmentManager.fragments.filter { fragment ->
            when (fragment) {
                is ClockFragment, is AlarmFragment, is StopwatchFragment, is TimerFragment -> fragment.tag == null
                else -> false
            }
        }
        if (legacyTabs.isNotEmpty()) {
            supportFragmentManager.commitNow { legacyTabs.forEach { remove(it) } }
        }

        val hasTabIntent = savedInstanceState == null && intent.hasExtra(OPEN_TAB)
        val request = when {
            hasTabIntent -> readTabRequest(intent)
            savedInstanceState?.containsKey(PENDING_TAB_STATE) == true -> TabRequest(
                page = savedInstanceState.getInt(PENDING_TAB_STATE),
                timerId = savedInstanceState.getInt(TIMER_ID, INVALID_TIMER_ID),
                startStopwatch = savedInstanceState.getBoolean(TOGGLE_STOPWATCH),
            )
            else -> null
        }
        pendingTab = request
        val initialPage = when {
            request != null -> request.page
            savedInstanceState?.containsKey(SELECTED_TAB_STATE) == true -> savedInstanceState.getInt(SELECTED_TAB_STATE)
            legacyTabs.isNotEmpty() -> config.lastUsedViewPagerPage
            else -> getTabIndex(config.defaultTab)
        }
        pagerState = PagerState(currentPage = initialPage.coerceIn(0, TABS_COUNT - 1)) { TABS_COUNT }
        intent.removeExtra(OPEN_TAB)
        supportFragmentManager.registerFragmentLifecycleCallbacks(tabLifecycleCallbacks, false)
    }

    private fun readTabRequest(intent: Intent): TabRequest {
        val page = getTabIndex(intent.getIntExtra(OPEN_TAB, TAB_CLOCK)).coerceIn(0, TABS_COUNT - 1)
        return TabRequest(
            page = page,
            timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID),
            startStopwatch = page == TAB_STOPWATCH_INDEX && intent.getBooleanExtra(TOGGLE_STOPWATCH, false),
        )
    }

    private fun deliverTabRequest() {
        val request = pendingTab ?: return
        if (request.page != selectedTab) return
        when {
            request.page == TAB_TIMER_INDEX -> {
                val timer = mountedTab<TimerFragment>()?.takeIf { it.isResumed } ?: return
                pendingTab = null
                timer.updatePosition(request.timerId)
            }
            request.startStopwatch -> {
                val stopwatch = mountedTab<StopwatchFragment>()?.takeIf { it.isResumed } ?: return
                pendingTab = null
                stopwatch.startStopWatch()
            }
            else -> pendingTab = null
        }
    }

    private data class TabRequest(val page: Int, val timerId: Int, val startStopwatch: Boolean)

    private companion object {
        const val SELECTED_TAB_STATE = "selected_tab"
        const val PENDING_TAB_STATE = "pending_tab"
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
        startActivity(Intent(this, AboutActivity::class.java))
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
