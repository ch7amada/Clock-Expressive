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
import org.fossify.clock.extensions.timerHelper
import org.fossify.clock.models.Timer
import org.fossify.clock.ui.dialogs.TimerEditorContent
import org.fossify.clock.ui.dialogs.showReminderSoundDialog
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.getFormattedDuration
import org.fossify.commons.models.AlarmSound

class EditTimerDialog(val activity: SimpleActivity, val timer: Timer, val callback: (id: Long) -> Unit) {
    private val dialog by lazy { ComponentDialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar) }
    private val lastConfig = if (timer.id == null) activity.config.timerLastConfig else null
    private val label = TextFieldState(initialText = lastConfig?.label ?: timer.label)
    private var seconds by mutableIntStateOf(lastConfig?.seconds ?: timer.seconds)
    private var soundTitle by mutableStateOf(lastConfig?.soundTitle ?: timer.soundTitle)
    private var soundUri = lastConfig?.soundUri ?: timer.soundUri
    private var vibrate by mutableStateOf(lastConfig?.vibrate ?: timer.vibrate)
    private var channelId = timer.channelId
    private var saving by mutableStateOf(false)

    init {
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) {
                showDialog()
            }
        }
    }

    private fun showDialog() {
        val dismissOnDestroy = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialog.dismiss()
            }
        }
        activity.lifecycle.addObserver(dismissOnDestroy)
        dialog.setOnDismissListener { activity.lifecycle.removeObserver(dismissOnDestroy) }
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
                    TimerEditorContent(
                        duration = seconds.getFormattedDuration(),
                        label = label,
                        soundTitle = soundTitle,
                        vibrate = vibrate,
                        saving = saving,
                        onDurationClick = ::openDurationPicker,
                        onSoundClick = ::openSoundPicker,
                        onVibrateChange = {
                            vibrate = it
                            channelId = null
                        },
                        onCancel = { if (!saving) dialog.dismiss() },
                        onConfirm = ::saveTimer,
                    )
                }
            }
        })
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun openDurationPicker() {
        MyTimePickerDialogDialog(activity, seconds) { selectedSeconds ->
            seconds = if (selectedSeconds <= 0) 10 else selectedSeconds
        }
    }

    private fun openSoundPicker() {
        showReminderSoundDialog(
            activity = activity,
            currentUri = soundUri,
            onAlarmPicked = ::updateAlarmSound,
            onAlarmSoundDeleted = {
                if (soundUri == it.uri) {
                    updateAlarmSound(activity.getDefaultAlarmSound(RingtoneManager.TYPE_ALARM))
                }
                activity.checkAlarmsWithDeletedSoundUri(it.uri)
            },
        )
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        if (dialog.isShowing && !saving) {
            soundTitle = alarmSound.title
            soundUri = alarmSound.uri
            channelId = null
        }
    }

    private fun saveTimer() {
        if (saving || !dialog.isShowing) return

        saving = true
        dialog.setCancelable(false)
        timer.label = label.text.toString().trim()
        timer.seconds = seconds
        timer.vibrate = vibrate
        timer.soundTitle = soundTitle
        timer.soundUri = soundUri
        timer.channelId = channelId
        activity.timerHelper.insertOrUpdateTimer(timer) { id ->
            activity.runOnUiThread {
                activity.config.timerLastConfig = timer
                callback(id)
                dialog.dismiss()
            }
        }
    }
}
