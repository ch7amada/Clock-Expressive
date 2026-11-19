package org.fossify.clock.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.fossify.clock.ui.theme.ClockTheme
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.R as CommonsR

internal fun showFirstAlarmWarning(activity: BaseSimpleActivity, onConfirm: () -> Unit) {
    showReminderPrompt(
        activity = activity,
        body = CommonsR.string.alarm_warning,
        confirmLabel = CommonsR.string.ok,
        onConfirm = { dialog ->
            dialog.dismiss()
            onConfirm()
        },
    )
}

internal fun showPermissionExplanation(
    activity: BaseSimpleActivity,
    @StringRes textId: Int,
    positiveActionCallback: () -> Unit,
    negativeActionCallback: (() -> Unit)? = null,
) {
    showReminderPrompt(
        activity = activity,
        title = CommonsR.string.permission_required,
        body = textId,
        confirmLabel = CommonsR.string.grant_permission,
        onCancel = { negativeActionCallback?.invoke() },
        onConfirm = { positiveActionCallback() },
    )
}

private fun showReminderPrompt(
    activity: BaseSimpleActivity,
    @StringRes body: Int,
    @StringRes confirmLabel: Int,
    @StringRes title: Int? = null,
    onCancel: (() -> Unit)? = null,
    onConfirm: (ComponentDialog) -> Unit,
) {
    if (activity.isFinishing || activity.isDestroyed) return

    val dialog = ComponentDialog(activity, android.R.style.Theme_DeviceDefault_Dialog)
    val dismissOnDestroy = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dialog.dismiss()
        }
    }
    activity.lifecycle.addObserver(dismissOnDestroy)
    dialog.setOnDismissListener { activity.lifecycle.removeObserver(dismissOnDestroy) }
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.setCanceledOnTouchOutside(true)

    var actionTaken = false
    fun runAction(action: () -> Unit) {
        if (actionTaken || !dialog.isShowing) return
        actionTaken = true
        try {
            action()
        } finally {
            if (dialog.isShowing) dialog.dismiss()
        }
    }

    dialog.setContentView(ComposeView(dialog.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ClockTheme {
                val titleText = title?.let { stringResource(it) }
                Surface(
                    modifier = Modifier
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                        .semantics { titleText?.let { paneTitle = it } },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (titleText != null) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    modifier = Modifier.semantics { heading() },
                                )
                            }
                            Text(
                                text = stringResource(body),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        ) {
                            if (onCancel != null) {
                                TextButton(
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    onClick = { runAction(onCancel) },
                                ) {
                                    Text(stringResource(CommonsR.string.cancel))
                                }
                            }
                            Button(
                                modifier = Modifier.heightIn(min = 48.dp),
                                onClick = { runAction { onConfirm(dialog) } },
                            ) {
                                Text(stringResource(confirmLabel))
                            }
                        }
                    }
                }
            }
        }
    })
    dialog.show()
}
