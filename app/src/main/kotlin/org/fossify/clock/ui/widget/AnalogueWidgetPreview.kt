package org.fossify.clock.ui.widget

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AnalogueWidgetPreview(backgroundColor: Int) {
    val now = rememberWidgetPreviewTime()
    val minute = now.get(Calendar.MINUTE)
    val hour = now.get(Calendar.HOUR)
    val time = DateFormat.getTimeFormat(LocalContext.current).format(now.time)
    val background = Color(backgroundColor).compositeOver(MaterialTheme.colorScheme.surface)
    val dialColor = if (background.luminance() > 0.179f) Color.Black else Color.White

    Canvas(
        Modifier
            .size(128.dp)
            .semantics { contentDescription = time },
    ) {
        val radius = size.minDimension * 0.46f

        drawCircle(dialColor, radius, style = Stroke(width = radius * 0.03f))
        repeat(12) { index ->
            val innerRadius = if (index % 3 == 0) 0.77f else 0.83f
            val direction = dialDirection(index / 12f)
            drawLine(
                color = dialColor,
                start = center + direction * (radius * innerRadius),
                end = center + direction * (radius * 0.91f),
                strokeWidth = radius * 0.035f,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = dialColor,
            start = center,
            end = center + dialDirection((hour + minute / 60f) / 12f) * (radius * 0.49f),
            strokeWidth = radius * 0.085f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = dialColor,
            start = center,
            end = center + dialDirection(minute / 60f) * (radius * 0.70f),
            strokeWidth = radius * 0.055f,
            cap = StrokeCap.Round,
        )
        drawCircle(dialColor, radius * 0.065f)
    }
}

private fun dialDirection(turns: Float): Offset {
    val angle = turns * 2 * PI
    return Offset(sin(angle).toFloat(), -cos(angle).toFloat())
}
