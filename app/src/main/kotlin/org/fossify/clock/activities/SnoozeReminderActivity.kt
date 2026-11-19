package org.fossify.clock.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.ui.dialogs.showReminderDurationDialog
import org.fossify.commons.helpers.MINUTE_SECONDS

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId == -1) {
            finish()
            return
        }

        alarmController.silenceAlarm(alarmId)
        showReminderDurationDialog(
            curSeconds = config.snoozeTime * MINUTE_SECONDS,
            cancelCallback = {
                alarmController.stopAlarm(alarmId)
                dialogCancelled()
            }
        ) {
            config.snoozeTime = it / MINUTE_SECONDS
            alarmController.snoozeAlarm(alarmId, config.snoozeTime)
            finishActivity()
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
