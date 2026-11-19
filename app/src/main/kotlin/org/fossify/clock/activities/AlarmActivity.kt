package org.fossify.clock.activities

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.getPassedSeconds
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.ui.alarm.RingingAlarmScreen
import org.fossify.clock.ui.dialogs.showReminderDurationDialog
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.isOreoMr1Plus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AlarmActivity : SimpleActivity() {
    private var alarm: Alarm? = null
    private var snoozePickerOpen by mutableStateOf(false)
    private var actionDispatched by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockscreen()

        val id = intent.getIntExtra(ALARM_ID, -1)
        val ringingAlarm = dbHelper.getAlarmWithId(id)
        if (ringingAlarm == null) {
            finish()
            return
        }
        alarm = ringingAlarm

        val label = ringingAlarm.label.ifEmpty {
            getString(org.fossify.commons.R.string.alarm)
        }
        val time = getFormattedTime(
            passedSeconds = getPassedSeconds(),
            showSeconds = false,
            makeAmPmSmaller = false,
        ).toString()

        EventBus.getDefault().register(this)
        setContent {
            ClockTheme(window = window) {
                RingingAlarmScreen(
                    label = label,
                    time = time,
                    actionsEnabled = !snoozePickerOpen && !actionDispatched,
                    onSnooze = { snoozeAlarm() },
                    onDismiss = { if (!snoozePickerOpen) dismissAlarmAndFinish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            AlarmClock.ACTION_DISMISS_ALARM -> dismissAlarmAndFinish()
            AlarmClock.ACTION_SNOOZE_ALARM -> {
                val durationMinutes = intent.getIntExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, -1)
                if (durationMinutes == -1) {
                    snoozeAlarm()
                } else {
                    snoozeAlarm(durationMinutes)
                }
            }

            else -> {
                // no-op. user probably clicked the notification
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (alarm != null) EventBus.getDefault().unregister(this)
    }

    private fun snoozeAlarm(overrideSnoozeDuration: Int? = null) {
        val alarmId = alarm?.id ?: return
        if (actionDispatched || isFinishing) return

        if (overrideSnoozeDuration != null) {
            dismissAlarmAndFinish(overrideSnoozeDuration)
        } else if (snoozePickerOpen) {
            return
        } else if (config.useSameSnooze) {
            dismissAlarmAndFinish(config.snoozeTime)
        } else {
            snoozePickerOpen = true
            alarmController.silenceAlarm(alarmId)
            showReminderDurationDialog(
                curSeconds = config.snoozeTime * MINUTE_SECONDS,
                cancelCallback = {
                    dismissAlarmAndFinish()
                },
                callback = {
                    if (!actionDispatched && !isFinishing) {
                        config.snoozeTime = it / MINUTE_SECONDS
                        dismissAlarmAndFinish(config.snoozeTime)
                    }
                }
            )
        }
    }

    private fun dismissAlarmAndFinish(snoozeMinutes: Int = -1) {
        val alarmId = alarm?.id ?: return
        if (actionDispatched || isFinishing) return

        actionDispatched = true
        if (snoozeMinutes != -1) {
            alarmController.snoozeAlarm(alarmId, snoozeMinutes)
        } else {
            alarmController.stopAlarm(alarmId)
        }
        finishActivity()
    }

    private fun showOverLockscreen() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (isOreoMr1Plus()) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAlarmStoppedEvent(event: AlarmEvent.Stopped) {
        if (event.alarmId == alarm?.id && !actionDispatched && !isFinishing) {
            actionDispatched = true
            finishActivity()
        }
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
