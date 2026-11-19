package org.fossify.clock.dialogs

import org.fossify.clock.R
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.SORT_BY_ALARM_TIME
import org.fossify.clock.helpers.SORT_BY_CREATION_ORDER
import org.fossify.clock.helpers.SORT_BY_DATE_AND_TIME
import org.fossify.clock.ui.dialogs.SortOption
import org.fossify.clock.ui.dialogs.showSortDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.helpers.SORT_BY_CUSTOM

class ChangeAlarmSortDialog(val activity: BaseSimpleActivity, val callback: () -> Unit) {
    init {
        showSortDialog(
            activity = activity,
            options = listOf(
                SortOption(SORT_BY_CREATION_ORDER, R.string.sort_by_creation_order),
                SortOption(SORT_BY_ALARM_TIME, R.string.sort_by_alarm_time),
                SortOption(SORT_BY_DATE_AND_TIME, R.string.sort_by_day_and_alarm_time),
                SortOption(SORT_BY_CUSTOM, org.fossify.commons.R.string.custom),
            ),
            currentSort = activity.config.alarmSort,
        ) { sort ->
            activity.config.alarmSort = sort
            callback()
        }
    }
}
