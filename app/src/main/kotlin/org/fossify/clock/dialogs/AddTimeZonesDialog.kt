package org.fossify.clock.dialogs

import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.getAllTimeZones
import org.fossify.clock.ui.dialogs.showTimeZonePicker

class AddTimeZonesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    init {
        val timeZones = getAllTimeZones()
        val selectedIds = timeZones.map { it.id.toString() }.intersect(activity.config.selectedTimeZones)
        showTimeZonePicker(activity, timeZones, selectedIds) { selection ->
            activity.config.selectedTimeZones = selection
            callback()
        }
    }
}
