package org.fossify.clock.dialogs

import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.ui.dialogs.showTimerDurationPicker

class MyTimePickerDialogDialog(val activity: SimpleActivity, val initialSeconds: Int, val callback: (result: Int) -> Unit) {
    init {
        showTimerDurationPicker(activity, initialSeconds, callback)
    }
}
