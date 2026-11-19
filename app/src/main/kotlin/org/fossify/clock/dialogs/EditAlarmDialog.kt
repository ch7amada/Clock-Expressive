package org.fossify.clock.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.RingtoneManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.extensions.checkAlarmsWithDeletedSoundUri
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.rotateWeekdays
import org.fossify.clock.helpers.getCurrentDayMinutes
import org.fossify.clock.helpers.updateNonRecurringAlarmDay
import org.fossify.clock.models.Alarm
import org.fossify.clock.ui.dialogs.AlarmEditorContent
import org.fossify.clock.ui.dialogs.showAlarmTimePicker
import org.fossify.clock.ui.dialogs.showReminderSoundDialog
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.toast
import org.fossify.commons.models.AlarmSound
import org.fossify.commons.R as CommonsR

class EditAlarmDialog(
    val activity: SimpleActivity,
    val alarm: Alarm,
    val onDismiss: () -> Unit = {},
    val callback: (alarmId: Int) -> Unit,
) {
    init {
        restoreLastAlarm()
    }

    private val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
    private val label = TextFieldState(initialText = alarm.label)
    private var timeInMinutes by mutableIntStateOf(alarm.timeInMinutes)
    private var days by mutableIntStateOf(alarm.days)
    private var vibrate by mutableStateOf(alarm.vibrate)
    private var soundTitle by mutableStateOf(alarm.soundTitle)

    init {
        val dayIndexes = activity.rotateWeekdays(arrayListOf(0, 1, 2, 3, 4, 5, 6))
        val dismissOnDestroy = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialog.dismiss()
            }
        }
        activity.lifecycle.addObserver(dismissOnDestroy)
        dialog.setOnDismissListener {
            activity.lifecycle.removeObserver(dismissOnDestroy)
            onDismiss()
        }
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            WindowCompat.setDecorFitsSystemWindows(this, false)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.32f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                attributes = attributes.apply { setFitInsetsTypes(0) }
            }
            @Suppress("DEPRECATION")
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                    } else {
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    },
            )
        }
        dialog.setContentView(ComposeView(dialog.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ClockTheme {
                    AlarmEditorContent(
                        time = activity.getFormattedTime(
                            passedSeconds = timeInMinutes * 60,
                            showSeconds = false,
                            makeAmPmSmaller = false,
                        ).toString(),
                        daylessLabel = if (days <= 0) {
                            if (timeInMinutes > getCurrentDayMinutes()) CommonsR.string.today else CommonsR.string.tomorrow
                        } else {
                            null
                        },
                        dayIndexes = dayIndexes,
                        days = days,
                        label = label,
                        soundTitle = soundTitle,
                        vibrate = vibrate,
                        onTimeClick = ::openTimePicker,
                        onDayClick = ::toggleDay,
                        onSoundClick = ::openSoundPicker,
                        onVibrateChange = {
                            alarm.vibrate = it
                            vibrate = it
                        },
                        onCancel = dialog::dismiss,
                        onConfirm = ::saveAlarm,
                    )
                }
            }
        })
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun restoreLastAlarm() {
        if (alarm.id == 0) {
            activity.config.alarmLastConfig?.let { lastConfig ->
                alarm.label = lastConfig.label
                alarm.days = lastConfig.days
                alarm.soundTitle = lastConfig.soundTitle
                alarm.soundUri = lastConfig.soundUri
                alarm.timeInMinutes = lastConfig.timeInMinutes
                alarm.vibrate = lastConfig.vibrate
            }
        }
    }

    private fun openTimePicker() {
        showAlarmTimePicker(
            activity = activity,
            initialHour = timeInMinutes / 60,
            initialMinute = timeInMinutes % 60,
            is24Hour = activity.config.use24HourFormat,
            onTimePicked = { hours, minutes ->
                alarm.timeInMinutes = hours * 60 + minutes
                timeInMinutes = alarm.timeInMinutes
            },
        )
    }

    private fun toggleDay(dayIndex: Int) {
        val selectedDays = if (alarm.isRecurring()) alarm.days else 0
        alarm.days = selectedDays xor (1 shl dayIndex)
        days = alarm.days
    }

    private fun openSoundPicker() {
        showReminderSoundDialog(
            activity = activity,
            currentUri = alarm.soundUri,
            onAlarmPicked = ::updateSelectedAlarmSound,
            onAlarmSoundDeleted = {
                if (alarm.soundUri == it.uri) {
                    updateSelectedAlarmSound(activity.getDefaultAlarmSound(RingtoneManager.TYPE_ALARM))
                }
                activity.checkAlarmsWithDeletedSoundUri(it.uri)
            },
        )
    }

    private fun saveAlarm() {
        if (!dialog.isShowing) return

        if (!activity.config.wasAlarmWarningShown) {
            ConfirmationDialog(
                activity = activity,
                messageId = CommonsR.string.alarm_warning,
                positive = CommonsR.string.ok,
                negative = 0,
            ) {
                activity.config.wasAlarmWarningShown = true
                saveAlarm()
            }
            return
        }

        updateNonRecurringAlarmDay(alarm)
        days = alarm.days
        alarm.label = label.text.toString().trim()
        alarm.isEnabled = true
        alarm.oneShot = false

        var alarmId = alarm.id
        activity.handleFullScreenNotificationsPermission { granted ->
            if (granted && dialog.isShowing) {
                if (alarm.id == 0) {
                    alarmId = activity.dbHelper.insertAlarm(alarm)
                    if (alarmId == -1) {
                        activity.toast(CommonsR.string.unknown_error_occurred)
                    }
                } else if (!activity.dbHelper.updateAlarm(alarm)) {
                    activity.toast(CommonsR.string.unknown_error_occurred)
                }

                activity.config.alarmLastConfig = alarm
                callback(alarmId)
                dialog.dismiss()
            }
        }
    }

    fun updateSelectedAlarmSound(alarmSound: AlarmSound) {
        alarm.soundTitle = alarmSound.title
        alarm.soundUri = alarmSound.uri
        soundTitle = alarmSound.title
    }
}
