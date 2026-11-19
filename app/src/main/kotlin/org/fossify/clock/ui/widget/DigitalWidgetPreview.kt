package org.fossify.clock.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.fossify.clock.helpers.FORMAT_12H
import org.fossify.clock.helpers.FORMAT_24H
import java.util.Calendar

@Composable
internal fun DigitalWidgetPreview(use24HourFormat: Boolean, textColor: Int) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    DisposableEffect(context) {
        val timeChanges = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                now = Calendar.getInstance()
            }
        }
        ContextCompat.registerReceiver(context, receiver, timeChanges, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val timeFormat = if (use24HourFormat) FORMAT_24H else FORMAT_12H
    val shadow = Shadow(Color.Black, Offset(0f, 1f), 1f)
    val timeStyle = MaterialTheme.typography.displayLargeEmphasized.copy(
        fontFamily = FontFamily.Default,
        fontFeatureSettings = "tnum",
        shadow = shadow,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = DateFormat.format(timeFormat, now).toString(),
            modifier = Modifier.fillMaxWidth(),
            color = Color(textColor),
            style = timeStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = timeStyle.fontSize),
        )
        Text(
            text = DateFormat.format("EEE, d MMM", now).toString(),
            color = Color(textColor),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Default, shadow = shadow),
            textAlign = TextAlign.Center,
        )
    }
}
