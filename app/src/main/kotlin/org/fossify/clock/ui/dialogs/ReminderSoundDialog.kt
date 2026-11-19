package org.fossify.clock.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.fossify.clock.helpers.PICK_AUDIO_FILE_INTENT_ID
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getAlarmSounds
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.models.AlarmSound
import org.fossify.commons.R as CommonsR

internal fun showReminderSoundDialog(
    activity: BaseSimpleActivity,
    currentUri: String,
    onAlarmPicked: (AlarmSound) -> Unit,
    onAlarmSoundDeleted: (AlarmSound) -> Unit,
) {
    val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
    val preview = SoundPreview(activity)
    val config = activity.baseConfig
    val gson = Gson()
    val soundListType = object : TypeToken<ArrayList<AlarmSound>>() {}.type
    fun readCustomSounds() =
        gson.fromJson<ArrayList<AlarmSound>>(config.yourAlarmSounds, soundListType) ?: arrayListOf()

    var customSounds by mutableStateOf(readCustomSounds().map { SoundOption(it, custom = true) })
    var systemSounds by mutableStateOf(emptyList<SoundOption>())
    var selection by mutableStateOf(customSounds.firstOrNull { it.sound.uri == currentUri })

    val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            preview.release()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            preview.release()
            dialog.dismiss()
        }
    }
    activity.lifecycle.addObserver(lifecycleObserver)
    dialog.setOnDismissListener {
        preview.release()
        activity.lifecycle.removeObserver(lifecycleObserver)
    }
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        volumeControlStream = AudioManager.STREAM_ALARM
    }
    dialog.setCanceledOnTouchOutside(true)
    dialog.setContentView(ComposeView(dialog.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClockTheme {
                ReminderSoundContent(
                    customSounds = customSounds,
                    systemSounds = systemSounds,
                    selection = selection,
                    onSelect = {
                        selection = it
                        preview.play(it.sound.uri)
                    },
                    onAdd = {
                        preview.release()
                        dialog.dismiss()
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            setType("audio/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        }
                        try {
                            activity.startActivityForResult(intent, PICK_AUDIO_FILE_INTENT_ID)
                        } catch (_: ActivityNotFoundException) {
                            activity.toast(CommonsR.string.no_app_found)
                        }
                    },
                    onStopPreview = preview::release,
                    onRemove = { option ->
                        val remainingSounds = readCustomSounds()
                        remainingSounds.remove(option.sound)
                        config.yourAlarmSounds = gson.toJson(remainingSounds)
                        customSounds = remainingSounds.map { SoundOption(it, custom = true) }
                        if (selection == option) {
                            selection = systemSounds.firstOrNull()
                        }
                        onAlarmSoundDeleted(option.sound)
                    },
                    onCancel = dialog::dismiss,
                    onConfirm = {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                            onAlarmPicked(it.sound)
                        }
                    },
                )
            }
        }
    })
    dialog.show()
    activity.getAlarmSounds(RingtoneManager.TYPE_ALARM) { sounds ->
        if (dialog.isShowing) {
            systemSounds = sounds.map { SoundOption(it, custom = false) }
            if (selection == null) {
                selection = systemSounds.firstOrNull { it.sound.uri == currentUri }
            }
        }
    }
}

private data class SoundOption(val sound: AlarmSound, val custom: Boolean) {
    val key get() = if (custom) "custom-${sound.id}" else "system-${sound.id}"
}

@Composable
private fun ReminderSoundContent(
    customSounds: List<SoundOption>,
    systemSounds: List<SoundOption>,
    selection: SoundOption?,
    onSelect: (SoundOption) -> Unit,
    onAdd: () -> Unit,
    onStopPreview: () -> Unit,
    onRemove: (SoundOption) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (SoundOption) -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<SoundOption?>(null) }
    val title = stringResource(CommonsR.string.notification_sound)
    Surface(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .semantics { paneTitle = title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.semantics { heading() },
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "custom-heading") {
                    SoundSectionHeading(stringResource(CommonsR.string.your_sounds))
                }
                items(customSounds, key = { it.key }) { option ->
                    SoundRow(option, option == selection, onClick = { onSelect(option) }) {
                        onStopPreview()
                        pendingRemoval = option
                    }
                }
                item(key = "add") {
                    Surface(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(CommonsR.drawable.ic_plus_vector), contentDescription = null)
                            Text(
                                text = stringResource(CommonsR.string.add_new_sound),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                            )
                        }
                    }
                }
                item(key = "system-heading") {
                    SoundSectionHeading(
                        title = stringResource(CommonsR.string.system_sounds),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(systemSounds, key = { it.key }) { option ->
                    SoundRow(option, option == selection, onClick = { onSelect(option) })
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(CommonsR.string.cancel)) }
                Button(
                    enabled = selection != null,
                    onClick = { selection?.let(onConfirm) },
                ) {
                    Text(stringResource(CommonsR.string.ok))
                }
            }
        }
    }

    pendingRemoval?.let { option ->
        val remove = stringResource(CommonsR.string.remove)
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(remove) },
            text = { Text(option.sound.title) },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(CommonsR.string.cancel)) }
            },
            confirmButton = {
                Button(onClick = {
                    pendingRemoval = null
                    onRemove(option)
                }) {
                    Text(remove)
                }
            },
        )
    }
}

@Composable
private fun SoundSectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SoundRow(option: SoundOption, isSelected: Boolean, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(shape)
                .combinedClickable(
                    role = Role.RadioButton,
                    onClick = onClick,
                    onLongClick = onRemove,
                    onLongClickLabel = if (onRemove != null) stringResource(CommonsR.string.remove) else null,
                )
                .semantics { selected = isSelected }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Text(text = option.sound.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private class SoundPreview(private val activity: BaseSimpleActivity) {
    private var player: MediaPlayer? = null

    @Suppress("DEPRECATION")
    fun play(uri: String) {
        release()
        if (uri == SILENT) return

        try {
            val nextPlayer = MediaPlayer()
            player = nextPlayer
            nextPlayer.apply {
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = true
                setDataSource(activity, Uri.parse(uri))
                setOnPreparedListener {
                    if (player === it) {
                        try {
                            it.start()
                        } catch (error: IllegalStateException) {
                            release()
                            activity.showErrorToast(error)
                        }
                    }
                }
                setOnErrorListener { failedPlayer, what, extra ->
                    if (player === failedPlayer) {
                        release()
                        activity.showErrorToast(IllegalStateException("MediaPlayer error ($what, $extra)"))
                    }
                    true
                }
                prepareAsync()
            }
        } catch (error: Exception) {
            release()
            activity.showErrorToast(error)
        }
    }

    fun release() {
        val previousPlayer = player
        player = null
        previousPlayer?.release()
    }
}
