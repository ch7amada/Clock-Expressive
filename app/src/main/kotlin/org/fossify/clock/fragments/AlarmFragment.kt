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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.clock.R
import org.fossify.clock.activities.MainActivity
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.dialogs.ChangeAlarmSortDialog
import org.fossify.clock.dialogs.EditAlarmDialog
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.createNewAlarm
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.firstDayOrder
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.DEFAULT_ALARM_MINUTES
import org.fossify.clock.helpers.SORT_BY_ALARM_TIME
import org.fossify.clock.helpers.SORT_BY_DATE_AND_TIME
import org.fossify.clock.helpers.getTomorrowBit
import org.fossify.clock.helpers.updateNonRecurringAlarmDay
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.ui.alarm.AlarmListItem
import org.fossify.clock.ui.alarm.AlarmScreen
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.getSelectedDaysString
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.EVERY_DAY_BIT
import org.fossify.commons.helpers.SORT_BY_CUSTOM
import org.fossify.commons.helpers.SORT_BY_DATE_CREATED
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.fossify.commons.R as CommonsR

class AlarmFragment : Fragment() {
    private var alarms: List<Alarm> = emptyList()
    private var rows by mutableStateOf<List<AlarmListItem>?>(null)
    private var selectedIds by mutableStateOf(emptySet<Int>())
    private var alarmLoad: Job? = null
    private var currentEditAlarmDialog: EditAlarmDialog? = null
    private val selectionBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = clearSelection()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        id = R.id.alarms_list
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                AlarmScreen(
                    alarms = rows,
                    selectedIds = selectedIds,
                    onAdd = ::addAlarm,
                    onClick = ::onAlarmClick,
                    onLongClick = ::toggleSelection,
                    onToggle = ::toggleAlarm,
                    onMove = ::moveAlarm,
                    onDelete = ::deleteSelectedAlarms,
                    onClearSelection = ::clearSelection,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, selectionBackCallback)
        setupAlarms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onDestroyView() {
        clearSelection()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        setupAlarms()
    }

    fun showSortingDialog() {
        val activity = activity as? SimpleActivity ?: return
        ChangeAlarmSortDialog(activity) { setupAlarms() }
    }

    private fun setupAlarms() {
        val context = context ?: return
        val owner = viewLifecycleOwnerLiveData.value ?: return
        alarmLoad?.cancel()
        alarmLoad = owner.lifecycleScope.launch {
            val loadedAlarms = withContext(Dispatchers.IO) { context.dbHelper.getAlarms() }
            val config = context.config
            val sortedAlarms = when (config.alarmSort) {
                SORT_BY_ALARM_TIME -> loadedAlarms.sortedBy { it.timeInMinutes }
                SORT_BY_DATE_CREATED -> loadedAlarms.sortedBy { it.id }
                SORT_BY_DATE_AND_TIME -> loadedAlarms.sortedWith(
                    compareBy<Alarm> { context.firstDayOrder(it.days) }.thenBy { it.timeInMinutes },
                )

                SORT_BY_CUSTOM -> {
                    val order = config.alarmsCustomSorting.split(", ")
                        .mapNotNull(String::toIntOrNull)
                        .withIndex()
                        .associate { (index, id) -> id to index }
                    if (order.isEmpty()) {
                        loadedAlarms.sortedBy { it.id }
                    } else {
                        loadedAlarms.sortedBy { order[it.id] ?: Int.MAX_VALUE }
                    }
                }

                else -> loadedAlarms
            }
            publishAlarms(sortedAlarms)
            clearSelection()
        }
    }

    private fun publishAlarms(alarms: List<Alarm>) {
        val context = context ?: return
        this.alarms = alarms
        rows = alarms.map { alarm ->
            val days = when {
                alarm.days == EVERY_DAY_BIT -> context.getString(CommonsR.string.every_day)
                alarm.isRecurring() -> context.getSelectedDaysString(alarm.days)
                !alarm.isEnabled -> context.getString(R.string.not_scheduled)
                alarm.isToday() -> context.getString(CommonsR.string.today)
                else -> context.getString(CommonsR.string.tomorrow)
            }
            AlarmListItem(
                id = alarm.id,
                time = context.getFormattedTime(alarm.timeInMinutes * 60, showSeconds = false, makeAmPmSmaller = false).toString(),
                days = days,
                label = alarm.label,
                enabled = alarm.isEnabled,
            )
        }
    }

    private fun addAlarm() {
        val newAlarm = requireContext().createNewAlarm(DEFAULT_ALARM_MINUTES, 0).apply {
            isEnabled = true
            days = getTomorrowBit()
        }
        openEditAlarm(newAlarm)
    }

    private fun onAlarmClick(id: Int) {
        if (selectedIds.isNotEmpty()) {
            toggleSelection(id)
        } else {
            alarms.firstOrNull { it.id == id }?.let { openEditAlarm(it.copy()) }
        }
    }

    private fun toggleSelection(id: Int) {
        updateSelection(if (id in selectedIds) selectedIds - id else selectedIds + id)
    }

    internal fun clearSelection() = updateSelection(emptySet())

    private fun updateSelection(ids: Set<Int>) {
        selectedIds = ids
        selectionBackCallback.isEnabled = ids.isNotEmpty()
    }

    private fun openEditAlarm(alarm: Alarm) {
        val activity = activity as? SimpleActivity ?: return
        currentEditAlarmDialog = EditAlarmDialog(
            activity = activity,
            alarm = alarm,
            onDismiss = { currentEditAlarmDialog = null },
        ) {
            alarm.id = it
            setupAlarms()
            checkAlarmState(alarm)
        }
    }

    private fun deleteSelectedAlarms() {
        val activity = activity as? SimpleActivity ?: return
        val toDelete = alarms.filter { it.id in selectedIds }
        if (toDelete.isEmpty()) return

        activity.dbHelper.deleteAlarms(ArrayList(toDelete))
        publishAlarms(alarms.filterNot { it.id in selectedIds })
        clearSelection()
        EventBus.getDefault().post(AlarmEvent.Refresh)
    }

    private fun moveAlarm(fromId: Int, toId: Int) {
        val config = context?.config ?: return
        if (selectedIds.isEmpty() || fromId == toId) return

        val from = alarms.indexOfFirst { it.id == fromId }
        val to = alarms.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0) return

        val reordered = alarms.toMutableList().apply { add(to, removeAt(from)) }
        config.alarmsCustomSorting = reordered.joinToString { it.id.toString() }
        if (config.alarmSort != SORT_BY_CUSTOM) config.alarmSort = SORT_BY_CUSTOM
        publishAlarms(reordered)
    }

    private fun toggleAlarm(id: Int, enabled: Boolean) {
        val activity = activity as? SimpleActivity ?: return
        val alarm = alarms.firstOrNull { it.id == id } ?: return

        if (alarm.isRecurring() && !activity.config.wasAlarmWarningShown) {
            ConfirmationDialog(
                activity = activity,
                messageId = CommonsR.string.alarm_warning,
                positive = CommonsR.string.ok,
                negative = 0,
            ) {
                activity.config.wasAlarmWarningShown = true
                updateAlarmEnabledState(id, enabled)
            }
        } else {
            if (!alarm.isRecurring()) {
                val updated = alarm.copy()
                updateNonRecurringAlarmDay(updated)
                activity.dbHelper.updateAlarm(updated)
                replaceAlarm(updated)
            }
            updateAlarmEnabledState(id, enabled)
        }
    }

    private fun updateAlarmEnabledState(id: Int, enabled: Boolean) {
        val activity = activity as? SimpleActivity ?: return
        activity.handleFullScreenNotificationsPermission { granted ->
            if (!granted) {
                setupAlarms()
                return@handleFullScreenNotificationsPermission
            }

            val alarm = alarms.firstOrNull { it.id == id }?.copy(isEnabled = enabled)
                ?: return@handleFullScreenNotificationsPermission
            if (activity.dbHelper.updateAlarmEnabledState(id, enabled)) {
                checkAlarmState(alarm)
                if (!enabled && alarm.oneShot) {
                    activity.dbHelper.deleteAlarms(arrayListOf(alarm))
                    setupAlarms()
                } else {
                    replaceAlarm(alarm)
                }
            } else {
                activity.toast(CommonsR.string.unknown_error_occurred)
            }
            activity.updateWidgets()
        }
    }

    private fun replaceAlarm(alarm: Alarm) {
        publishAlarms(alarms.map { if (it.id == alarm.id) alarm else it })
        // A query already in flight may have read the row before the write.
        if (alarmLoad?.isActive == true) setupAlarms()
    }

    private fun checkAlarmState(alarm: Alarm) {
        val activity = activity as? MainActivity ?: return
        if (alarm.isEnabled) {
            activity.alarmController.scheduleNextOccurrence(alarm = alarm, showToasts = true)
        } else {
            activity.cancelAlarmClock(alarm)
        }
        activity.updateClockTabAlarm()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditAlarmDialog?.updateSelectedAlarmSound(alarmSound)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: AlarmEvent.Refresh) {
        setupAlarms()
    }
}
