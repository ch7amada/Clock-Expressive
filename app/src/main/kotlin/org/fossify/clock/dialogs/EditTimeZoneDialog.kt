package org.fossify.clock.dialogs

import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getEditedTimeZonesMap
import org.fossify.clock.extensions.getModifiedTimeZoneTitle
import org.fossify.clock.helpers.EDITED_TIME_ZONE_SEPARATOR
import org.fossify.clock.helpers.getDefaultTimeZoneTitle
import org.fossify.clock.models.MyTimeZone
import org.fossify.clock.ui.dialogs.showTimeZoneEditor

class EditTimeZoneDialog(val activity: SimpleActivity, val myTimeZone: MyTimeZone, val callback: () -> Unit) {

    init {
        showTimeZoneEditor(
            activity = activity,
            initialTitle = activity.getModifiedTimeZoneTitle(myTimeZone.id),
            defaultTitle = getDefaultTimeZoneTitle(myTimeZone.id),
            onConfirm = { dialogConfirmed(it.trim()) },
        )
    }

    private fun dialogConfirmed(newTitle: String) {
        val editedTitlesMap = activity.getEditedTimeZonesMap()

        if (newTitle.isEmpty()) {
            editedTitlesMap.remove(myTimeZone.id)
        } else {
            editedTitlesMap[myTimeZone.id] = newTitle
        }

        val newTitlesSet = HashSet<String>()
        for ((key, value) in editedTitlesMap) {
            newTitlesSet.add("$key$EDITED_TIME_ZONE_SEPARATOR$value")
        }

        activity.config.editedTimeZoneTitles = newTitlesSet
        callback()
    }
}
