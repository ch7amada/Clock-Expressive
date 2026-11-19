package org.fossify.clock.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.clock.R
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.DATA_EXPORT_EXTENSION
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getCurrentFormattedDateTime
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.internalStoragePath
import org.fossify.commons.extensions.isAValidFilename
import org.fossify.commons.extensions.toast
import java.io.File
import org.fossify.commons.R as CommonsR

class ExportDataDialog(
    private val activity: BaseSimpleActivity,
    path: String,
    private val callback: (file: File) -> Unit,
) {
    companion object {
        private const val EXPORT_FILE_NAME = "alarms_and_timers"
    }

    private val realPath = path.ifEmpty { activity.internalStoragePath }

    init {
        val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
        var submitting by mutableStateOf(false)
        var validationJob: Job? = null
        val dismissOnDestroy = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialog.dismiss()
            }
        }
        activity.lifecycle.addObserver(dismissOnDestroy)
        dialog.setOnDismissListener {
            validationJob?.cancel()
            activity.lifecycle.removeObserver(dismissOnDestroy)
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            @Suppress("DEPRECATION")
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        dialog.setCanceledOnTouchOutside(true)

        val initialFilename = "${EXPORT_FILE_NAME}_${activity.getCurrentFormattedDateTime()}"
        dialog.setContentView(ComposeView(dialog.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ClockTheme {
                    ExportForm(
                        initialFilename = initialFilename,
                        submitting = submitting,
                        onCancel = dialog::dismiss,
                        onConfirm = confirm@{ input ->
                            if (submitting || !dialog.isShowing) return@confirm
                            val filename = input.trim()
                            when {
                                filename.isEmpty() -> activity.toast(CommonsR.string.empty_name)
                                !filename.isAValidFilename() -> activity.toast(CommonsR.string.invalid_name)
                                else -> {
                                    submitting = true
                                    val file = File(realPath, "$filename$DATA_EXPORT_EXTENSION")
                                    validationJob = activity.lifecycleScope.launch {
                                        val exists = withContext(Dispatchers.IO) { file.exists() }
                                        submitting = false
                                        if (!dialog.isShowing || activity.isFinishing ||
                                            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                                        ) return@launch

                                        if (exists) {
                                            activity.toast(CommonsR.string.name_taken)
                                        } else {
                                            activity.config.lastDataExportPath = file.absolutePath.getParentPath()
                                            dialog.dismiss()
                                            callback(file)
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        })
        dialog.show()
    }
}

@Composable
private fun ExportForm(
    initialFilename: String,
    submitting: Boolean,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val filenameState = rememberTextFieldState(initialFilename, TextRange(initialFilename.length))
    val focusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.first { it }
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val title = stringResource(R.string.settings_export_data)
    Surface(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().semantics { paneTitle = title },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.semantics { heading() },
                )
                OutlinedTextField(
                    state = filenameState,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    label = { Text(stringResource(CommonsR.string.filename_without_json)) },
                    suffix = { Text(DATA_EXPORT_EXTENSION) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    readOnly = submitting,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(CommonsR.string.cancel))
                }
                Button(
                    onClick = { onConfirm(filenameState.text.toString()) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !submitting,
                ) {
                    Text(stringResource(CommonsR.string.ok))
                }
            }
        }
    }
}
