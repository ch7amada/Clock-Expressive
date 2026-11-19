package org.fossify.clock.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.dialogs.AddTimeZonesDialog
import org.fossify.clock.dialogs.EditTimeZoneDialog
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getAllTimeZonesModified
import org.fossify.clock.extensions.getClosestEnabledAlarmString
import org.fossify.clock.models.MyTimeZone
import org.fossify.clock.ui.clock.ClockScreen
import org.fossify.clock.ui.theme.ClockTheme
import java.util.TimeZone

class ClockFragment : Fragment() {
    private val clockTime = mutableLongStateOf(System.currentTimeMillis())
    private var clockMinute by mutableLongStateOf(clockTime.longValue / 60_000L)
    private var localTimeZone by mutableStateOf(TimeZone.getDefault())
    private var use24HourFormat by mutableStateOf(false)
    private var nextAlarm by mutableStateOf("")
    private var timeZones by mutableStateOf(emptyList<MyTimeZone>())
    private var selectedIds by mutableStateOf(emptySet<Int>())
    private var ticker: Job? = null
    private var alarmRequest = 0
    private val selectionBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = clearSelection()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        id = R.id.clock_fragment
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                ClockScreen(
                    clockTime = clockTime,
                    clockMinute = clockMinute,
                    localTimeZone = localTimeZone,
                    use24HourFormat = use24HourFormat,
                    nextAlarm = nextAlarm,
                    timeZones = timeZones,
                    selectedIds = selectedIds,
                    onAdd = ::addTimeZones,
                    onClick = ::onTimeZoneClick,
                    onLongClick = ::toggleSelection,
                    onDelete = ::deleteSelectedZones,
                    onClearSelection = ::clearSelection,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, selectionBackCallback)
        updateAlarm()
    }

    override fun onResume() {
        super.onResume()
        refreshClock()
    }

    override fun onPause() {
        stopTicker()
        clearSelection()
        super.onPause()
    }

    override fun onDestroyView() {
        stopTicker()
        alarmRequest++
        clearSelection()
        nextAlarm = ""
        super.onDestroyView()
    }

    private fun refreshClock() {
        val context = context ?: return
        val owner = viewLifecycleOwnerLiveData.value ?: return
        use24HourFormat = context.config.use24HourFormat
        updateTimeZones()
        updateAlarm()
        stopTicker()
        ticker = owner.lifecycleScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                clockTime.longValue = now
                clockMinute = Math.floorDiv(now, 60_000L)
                localTimeZone = TimeZone.getDefault()
                delay(1000L - Math.floorMod(now, 1000L))
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    fun updateAlarm() {
        val context = context ?: return
        if (viewLifecycleOwnerLiveData.value == null) return
        val request = ++alarmRequest
        context.getClosestEnabledAlarmString { alarm ->
            if (request == alarmRequest) nextAlarm = alarm
        }
    }

    private fun updateTimeZones() {
        val context = context ?: return
        val ids = context.config.selectedTimeZones
        timeZones = if (ids.isEmpty()) {
            emptyList()
        } else {
            context.getAllTimeZonesModified().filter { it.id.toString() in ids }
        }
        clearSelection()
    }

    private fun addTimeZones() {
        val activity = activity as? SimpleActivity ?: return
        AddTimeZonesDialog(activity, ::updateTimeZones)
    }

    private fun onTimeZoneClick(id: Int) {
        if (selectedIds.isNotEmpty()) {
            toggleSelection(id)
            return
        }

        val activity = activity as? SimpleActivity ?: return
        val zone = timeZones.firstOrNull { it.id == id } ?: return
        EditTimeZoneDialog(activity, zone, ::updateTimeZones)
    }

    private fun toggleSelection(id: Int) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        selectionBackCallback.isEnabled = selectedIds.isNotEmpty()
    }

    private fun clearSelection() {
        selectedIds = emptySet()
        selectionBackCallback.isEnabled = false
    }

    private fun deleteSelectedZones() {
        val config = context?.config ?: return
        config.selectedTimeZones = config.selectedTimeZones - selectedIds.map(Int::toString).toSet()
        updateTimeZones()
    }
}
