package org.fossify.clock.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.fossify.clock.dialogs.ExportDataDialog
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.timerDb
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.DEFAULT_MAX_ALARM_REMINDER_SECS
import org.fossify.clock.helpers.DEFAULT_MAX_TIMER_REMINDER_SECS
import org.fossify.clock.helpers.EXPORT_BACKUP_MIME_TYPE
import org.fossify.clock.helpers.ExportHelper
import org.fossify.clock.helpers.IMPORT_BACKUP_MIME_TYPES
import org.fossify.clock.helpers.ImportHelper
import org.fossify.clock.helpers.TimerHelper
import org.fossify.clock.models.AlarmTimerBackup
import org.fossify.clock.ui.dialogs.showReminderDurationDialog
import org.fossify.clock.ui.settings.ClockSettingsScreen
import org.fossify.clock.ui.settings.ClockSettingsState
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.extensions.formatMinutesToTimeString
import org.fossify.commons.extensions.formatSecondsToTimeString
import org.fossify.commons.extensions.hasThankYouUnlock
import org.fossify.commons.extensions.launchPurchaseThankYouIntent
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ExportResult
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isTiramisuPlus
import java.util.Locale
import kotlin.system.exitProcess
import org.fossify.commons.R as CommonsR

class SettingsActivity : SimpleActivity() {
    private var settings by mutableStateOf<ClockSettingsState?>(null)
    private val exportActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_BACKUP_MIME_TYPE)) { uri ->
            if (uri == null) return@registerForActivityResult
            ensureBackgroundThread {
                exportData(uri)
            }
        }

    private val importActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            ensureBackgroundThread {
                importData(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshSettings()
        setContent {
            ClockTheme {
                settings?.let { state ->
                    ClockSettingsScreen(
                        state = state,
                        onBack = ::finish,
                        onCustomizeColors = { startCustomizationActivity() },
                        onCustomizeWidgetColors = ::customizeWidgetColors,
                        onPurchaseThankYou = { launchPurchaseThankYouIntent() },
                        onChangeLanguage = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launchChangeAppLanguageIntent()
                            }
                        },
                        onUseEnglish = {
                            config.useEnglish = it
                            exitProcess(0)
                        },
                        onUse24HourFormat = {
                            config.use24HourFormat = it
                            updateWidgets()
                            refreshSettings()
                        },
                        onSelectDefaultTab = {
                            config.defaultTab = it
                            refreshSettings()
                        },
                        onSelectFirstDayOfWeek = {
                            config.firstDayOfWeek = it
                            refreshSettings()
                        },
                        onKeepAwake = {
                            config.preventPhoneFromSleeping = it
                            refreshSettings()
                        },
                        onAlarmReminder = ::pickAlarmReminder,
                        onIncreaseVolume = {
                            config.increaseVolumeGradually = it
                            refreshSettings()
                        },
                        onUseSameSnooze = {
                            config.useSameSnooze = it
                            refreshSettings()
                        },
                        onSnoozeDuration = ::pickSnoozeDuration,
                        onTimerReminder = ::pickTimerReminder,
                        onExport = ::tryExportData,
                        onImport = ::tryImportData,
                    )
                }
            }
        }
        setupEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()
        refreshSettings()
    }

    private fun refreshSettings() {
        val locale = Locale.getDefault()
        val androidLanguageSettings = isTiramisuPlus()
        settings = ClockSettingsState(
            showPurchaseThankYou = resources.getBoolean(CommonsR.bool.is_google_play_build) && !hasThankYouUnlock(),
            language = locale.displayLanguage.takeIf { androidLanguageSettings },
            useEnglish = config.useEnglish.takeIf {
                !androidLanguageSettings && (config.wasUseEnglishToggled || locale.language != "en")
            },
            use24HourFormat = config.use24HourFormat,
            defaultTab = config.defaultTab,
            firstDayOfWeek = config.firstDayOfWeek,
            keepAwake = config.preventPhoneFromSleeping,
            alarmReminder = formatSecondsToTimeString(config.alarmMaxReminderSecs),
            increaseVolume = config.increaseVolumeGradually,
            useSameSnooze = config.useSameSnooze,
            snoozeDuration = formatMinutesToTimeString(config.snoozeTime),
            timerReminder = formatSecondsToTimeString(config.timerMaxReminderSecs),
        )
    }

    private fun pickAlarmReminder() {
        showReminderDurationDialog(
            curSeconds = config.alarmMaxReminderSecs,
            showSecondsForCustom = true,
        ) {
            config.alarmMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_ALARM_REMINDER_SECS
            refreshSettings()
        }
    }

    private fun pickSnoozeDuration() {
        showReminderDurationDialog(curSeconds = config.snoozeTime * MINUTE_SECONDS) {
            config.snoozeTime = it / MINUTE_SECONDS
            refreshSettings()
        }
    }

    private fun pickTimerReminder() {
        showReminderDurationDialog(
            curSeconds = config.timerMaxReminderSecs,
            showSecondsForCustom = true,
        ) {
            config.timerMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_TIMER_REMINDER_SECS
            refreshSettings()
        }
    }

    private fun customizeWidgetColors() {
        Intent(this, WidgetDigitalConfigureActivity::class.java).apply {
            putExtra(IS_CUSTOMIZING_COLORS, true)
            startActivity(this)
        }
    }

    private fun exportData(outputUri: Uri) {
        val alarms = dbHelper.getAlarms()
        val timers = timerDb.getTimers()
        if (alarms.isEmpty() && timers.isEmpty()) {
            toast(org.fossify.commons.R.string.no_entries_for_exporting)
        } else {
            ExportHelper(this).exportData(
                backup = AlarmTimerBackup(alarms, timers),
                outputUri = outputUri,
            ) {
                toast(
                    when (it) {
                        ExportResult.EXPORT_OK -> org.fossify.commons.R.string.exporting_successful
                        else -> org.fossify.commons.R.string.exporting_failed
                    }
                )
            }
        }
    }

    private fun tryExportData() {
        ExportDataDialog(this, config.lastDataExportPath) { file ->
            try {
                exportActivityResultLauncher.launch(file.name)
            } catch (@Suppress("SwallowedException") _: ActivityNotFoundException) {
                toast(
                    id = org.fossify.commons.R.string.system_service_disabled,
                    length = Toast.LENGTH_LONG
                )
            }
        }
    }

    private fun tryImportData() {
        try {
            importActivityResultLauncher.launch(IMPORT_BACKUP_MIME_TYPES.toTypedArray())
        } catch (@Suppress("SwallowedException") _: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
        }
    }

    private fun importData(uri: Uri) {
        val result = ImportHelper(
            context = this,
            dbHelper = dbHelper,
            timerHelper = TimerHelper(this)
        ).importData(uri)

        toast(
            when (result) {
                ImportHelper.ImportResult.IMPORT_OK -> org.fossify.commons.R.string.importing_successful
                ImportHelper.ImportResult.IMPORT_INCOMPLETE -> org.fossify.commons.R.string.no_new_entries_for_importing
                ImportHelper.ImportResult.IMPORT_FAIL -> org.fossify.commons.R.string.no_items_found
            }
        )
    }
}
