package org.fossify.clock.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.dialogs.ChangeTimerSortDialog
import org.fossify.clock.dialogs.EditTimerDialog
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.createNewTimer
import org.fossify.clock.extensions.getFormattedDuration
import org.fossify.clock.extensions.hideTimerNotification
import org.fossify.clock.extensions.secondsToMillis
import org.fossify.clock.extensions.timerHelper
import org.fossify.clock.helpers.INVALID_TIMER_ID
import org.fossify.clock.helpers.SORT_BY_TIMER_DURATION
import org.fossify.clock.models.Timer
import org.fossify.clock.models.TimerEvent
import org.fossify.clock.models.TimerState
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.clock.ui.timer.TimerListItem
import org.fossify.clock.ui.timer.TimerScreen
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.getFormattedDuration
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.SORT_BY_DATE_CREATED
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.fossify.commons.R as CommonsR

class TimerFragment : Fragment() {
    private var timers = emptyList<Timer>()
    private var rows by mutableStateOf<List<TimerListItem>?>(null)
    private var selectedIds by mutableStateOf(emptySet<Int>())
    private var timerIdToScrollTo by mutableStateOf<Int?>(null)
    private var animatePlacement by mutableStateOf(true)
    private var skipNextPlacementAnimation = false
    private var refreshRequest = 0L
    private val pendingDeletes = mutableSetOf<Int>()
    private var currentEditTimerDialog: EditTimerDialog? = null

    private val selectionBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = updateSelection(emptySet())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        id = R.id.timers_list
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                TimerScreen(
                    timers = rows,
                    selectedIds = selectedIds,
                    timerIdToScrollTo = timerIdToScrollTo,
                    animatePlacement = animatePlacement,
                    onAdd = ::addTimer,
                    onClick = ::onTimerClick,
                    onLongClick = ::toggleSelection,
                    onToggle = ::toggleTimer,
                    onReset = ::resetTimer,
                    onMove = ::moveTimer,
                    onDelete = ::deleteSelectedTimers,
                    onClearSelection = { updateSelection(emptySet()) },
                    onScrollComplete = { id ->
                        if (timerIdToScrollTo == id) timerIdToScrollTo = null
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, selectionBackCallback)
        refreshTimers()

        // The initial timer is created asynchronously on the first launch.
        if (requireContext().config.appRunCount == 1) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                refreshTimers()
            }
        }
    }

    override fun onDestroyView() {
        updateSelection(emptySet())
        timers = emptyList()
        rows = null
        super.onDestroyView()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser) updateSelection(emptySet())
    }

    override fun onResume() {
        super.onResume()
        refreshTimers()
    }

    fun showSortingDialog() {
        val activity = activity as? SimpleActivity ?: return
        ChangeTimerSortDialog(activity) {
            skipNextPlacementAnimation = true
            refreshTimers()
        }
    }

    private fun refreshTimers() {
        val activity = activity ?: return
        val owner = viewLifecycleOwnerLiveData.value ?: return
        val request = ++refreshRequest
        activity.timerHelper.getTimers { loadedTimers ->
            activity.runOnUiThread {
                if (request != refreshRequest || owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    return@runOnUiThread
                }

                // A refresh can see timers whose asynchronous delete has not finished.
                if (pendingDeletes.isNotEmpty()) {
                    pendingDeletes.retainAll(loadedTimers.mapNotNull { it.id }.toSet())
                }
                val availableTimers = loadedTimers.filter { it.id != null && it.id !in pendingDeletes }
                val config = activity.config
                val sortedTimers = when (config.timerSort) {
                    SORT_BY_TIMER_DURATION -> availableTimers.sortedBy { it.seconds }
                    SORT_BY_DATE_CREATED -> availableTimers.sortedBy { it.id }
                    SORT_BY_CUSTOM -> {
                        val order = config.timersCustomSorting.split(", ")
                            .mapNotNull(String::toIntOrNull)
                            .distinct()
                            .withIndex()
                            .associate { (index, id) -> id to index }
                        if (order.isEmpty()) {
                            availableTimers.sortedBy { it.id }
                        } else {
                            availableTimers.sortedBy { order[it.id] ?: Int.MAX_VALUE }
                        }
                    }

                    else -> availableTimers
                }

                animatePlacement = !skipNextPlacementAnimation
                skipNextPlacementAnimation = false
                publishTimers(sortedTimers)
                if (selectedIds.isNotEmpty()) {
                    updateSelection(selectedIds.intersect(availableTimers.mapNotNull { it.id }.toSet()))
                }
            }
        }
    }

    private fun publishTimers(timers: List<Timer>) {
        this.timers = timers
        rows = timers.map { timer ->
            val time = when (val state = timer.state) {
                is TimerState.Finished -> 0.getFormattedDuration()
                is TimerState.Idle -> timer.seconds.getFormattedDuration()
                is TimerState.Paused -> state.tick.getFormattedDuration()
                is TimerState.Running -> state.tick.getFormattedDuration()
            }
            val progress = when (val state = timer.state) {
                is TimerState.Running -> remainingProgress(state.tick, state.duration)
                is TimerState.Paused -> remainingProgress(state.tick, state.duration)
                is TimerState.Finished -> 0f
                is TimerState.Idle -> null
            }
            TimerListItem(
                id = checkNotNull(timer.id),
                time = time,
                label = timer.label,
                running = timer.state is TimerState.Running,
                paused = timer.state is TimerState.Paused,
                canReset = timer.state !is TimerState.Idle,
                progress = progress,
            )
        }
    }

    private fun remainingProgress(tick: Long, duration: Long): Float? =
        if (duration > 0) (tick.toDouble() / duration).coerceIn(0.0, 1.0).toFloat() else null

    private fun addTimer() {
        val activity = activity as? SimpleActivity ?: return
        activity.hideKeyboard()
        openEditTimer(activity.createNewTimer())
    }

    private fun onTimerClick(id: Int) {
        if (selectedIds.isNotEmpty()) {
            toggleSelection(id)
        } else {
            timers.firstOrNull { it.id == id }?.let { openEditTimer(it.copy()) }
        }
    }

    private fun toggleSelection(id: Int) {
        if (timers.none { it.id == id }) return
        updateSelection(if (id in selectedIds) selectedIds - id else selectedIds + id)
    }

    private fun updateSelection(ids: Set<Int>) {
        selectedIds = ids
        selectionBackCallback.isEnabled = ids.isNotEmpty()
    }

    private fun moveTimer(fromId: Int, toId: Int) {
        val config = context?.config ?: return
        if (selectedIds.isEmpty() || fromId == toId) return
        val from = timers.indexOfFirst { it.id == fromId }
        val to = timers.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0) return

        val reordered = timers.toMutableList().apply { add(to, removeAt(from)) }
        config.timersCustomSorting = reordered.joinToString { it.id.toString() }
        if (config.timerSort != SORT_BY_CUSTOM) config.timerSort = SORT_BY_CUSTOM
        animatePlacement = true
        publishTimers(reordered)
    }

    private fun toggleTimer(id: Int) {
        val activity = activity as? SimpleActivity ?: return
        val owner = viewLifecycleOwnerLiveData.value ?: return
        activity.handleNotificationPermission { granted ->
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return@handleNotificationPermission
            if (granted) {
                val timer = timers.firstOrNull { it.id == id } ?: return@handleNotificationPermission
                val event = when (val state = timer.state) {
                    is TimerState.Idle, is TimerState.Finished -> TimerEvent.Start(id, timer.seconds.secondsToMillis)
                    is TimerState.Paused -> TimerEvent.Start(id, state.tick)
                    is TimerState.Running -> TimerEvent.Pause(id, state.tick)
                }
                EventBus.getDefault().post(event)
            } else {
                PermissionRequiredDialog(
                    activity = activity,
                    textId = CommonsR.string.allow_notifications_reminders,
                    positiveActionCallback = { activity.openNotificationSettings() },
                )
            }
        }
    }

    private fun resetTimer(id: Int) {
        val activity = activity as? SimpleActivity ?: return
        EventBus.getDefault().post(TimerEvent.Reset(id))
        activity.hideTimerNotification(id)
    }

    private fun deleteSelectedTimers() {
        val activity = activity as? SimpleActivity ?: return
        val ids = selectedIds
        if (ids.isEmpty()) return
        pendingDeletes.addAll(ids)
        publishTimers(timers.filterNot { it.id in ids })
        updateSelection(emptySet())
        ids.forEach { id ->
            EventBus.getDefault().post(TimerEvent.Delete(id))
            activity.hideTimerNotification(id)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: TimerEvent.Refresh) {
        refreshTimers()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditTimerDialog?.updateAlarmSound(alarmSound)
    }

    fun updatePosition(timerId: Int) {
        timerIdToScrollTo = timerId.takeUnless { it == INVALID_TIMER_ID }
        if (timerIdToScrollTo != null) refreshTimers()
    }

    private fun openEditTimer(timer: Timer) {
        val activity = activity as? SimpleActivity ?: return
        currentEditTimerDialog = EditTimerDialog(activity, timer) {
            currentEditTimerDialog = null
            refreshTimers()
        }
    }
}
