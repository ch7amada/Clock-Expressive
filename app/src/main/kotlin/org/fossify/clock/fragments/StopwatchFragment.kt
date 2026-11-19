package org.fossify.clock.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.STOPWATCH_LIVE_LAP_ID
import org.fossify.clock.helpers.Stopwatch
import org.fossify.clock.models.Lap
import org.fossify.clock.ui.dialogs.showPermissionExplanation
import org.fossify.clock.ui.stopwatch.StopwatchLap
import org.fossify.clock.ui.stopwatch.StopwatchScreen
import org.fossify.clock.ui.stopwatch.StopwatchUiState
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.extensions.flipBit
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.helpers.SORT_DESCENDING

class StopwatchFragment : Fragment() {
    private var uiState by mutableStateOf(StopwatchUiState())
    private var latestLapTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        id = R.id.stopwatch_fragment
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                StopwatchScreen(
                    state = uiState,
                    onToggle = ::togglePlayPause,
                    onReset = Stopwatch::reset,
                    onLap = ::recordLap,
                    onSort = ::changeSorting,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val config = requireContext().config
        Lap.sorting = config.stopwatchLapsSort
        Stopwatch.addUpdateListener(updateListener)

        if (config.toggleStopwatch) {
            config.toggleStopwatch = false
            startStopWatch()
        }
    }

    override fun onPause() {
        Stopwatch.removeUpdateListener(updateListener)
        super.onPause()
    }

    fun startStopWatch() {
        if (Stopwatch.state == Stopwatch.State.STOPPED) {
            togglePlayPause()
        }
    }

    private fun togglePlayPause() {
        val activity = activity as? SimpleActivity ?: return
        activity.handleNotificationPermission { granted ->
            if (granted) {
                Stopwatch.toggle()
            } else {
                showPermissionExplanation(
                    activity,
                    org.fossify.commons.R.string.allow_notifications_reminders,
                    activity::openNotificationSettings,
                )
            }
        }
    }

    private fun recordLap() {
        if (Stopwatch.state != Stopwatch.State.RUNNING) return
        Stopwatch.lap()
        latestLapTime = 0
        val totalTime = Stopwatch.laps.first().totalTime
        publishSnapshot(totalTime)
    }

    private fun changeSorting(column: Int) {
        val sorting = if (Lap.sorting and column != 0) {
            Lap.sorting.flipBit(SORT_DESCENDING)
        } else {
            column or SORT_DESCENDING
        }
        Lap.sorting = sorting
        requireContext().config.stopwatchLapsSort = sorting
        publishSnapshot()
    }

    private fun publishSnapshot(
        totalTime: Long = uiState.totalTime,
        useLongerMSFormat: Boolean = uiState.useLongerMSFormat,
    ) {
        val state = Stopwatch.state
        val recordedLaps = Stopwatch.laps
        val laps = if (state == Stopwatch.State.STOPPED || recordedLaps.isEmpty()) {
            emptyList()
        } else {
            val live = Lap(STOPWATCH_LIVE_LAP_ID, latestLapTime, totalTime)
            (recordedLaps + live).sorted().map { lap ->
                StopwatchLap(
                    id = lap.id,
                    number = if (lap === live) recordedLaps.size + 1 else lap.id,
                    lapTime = lap.lapTime,
                    totalTime = lap.totalTime,
                )
            }
        }
        uiState = StopwatchUiState(state, totalTime, useLongerMSFormat, Lap.sorting, laps)
    }

    private val updateListener = object : Stopwatch.UpdateListener {
        override fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
            latestLapTime = lapTime
            publishSnapshot(totalTime, useLongerMSFormat)
        }

        override fun onStateChanged(state: Stopwatch.State) {
            uiState = uiState.copy(status = state)
        }
    }
}
