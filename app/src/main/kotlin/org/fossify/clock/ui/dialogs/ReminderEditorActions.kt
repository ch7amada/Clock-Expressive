package org.fossify.clock.ui.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.commons.R as CommonsR

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReminderEditorTicket(
    value: String,
    supportingText: String?,
    clickLabel: String,
    @DrawableRes icon: Int,
    color: Color,
    contentColor: Color,
    graphicColor: Color,
    graphicContentColor: Color,
    fontFamily: FontFamily? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 16.dp, bottomEnd = 36.dp, bottomStart = 16.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
                .padding(start = 18.dp, top = 16.dp, end = 14.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = value,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 54.sp),
                    style = MaterialTheme.typography.displayLargeEmphasized.copy(
                        fontFamily = fontFamily ?: MaterialTheme.typography.displayLargeEmphasized.fontFamily,
                        fontFeatureSettings = "tnum",
                    ),
                )
                supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = graphicColor,
                contentColor = graphicContentColor,
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReminderEditorActions(
    color: Color,
    contentColor: Color,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val pressedShape = RoundedCornerShape(12.dp)
    val contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            enabled = enabled,
            shapes = ButtonDefaults.shapes(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 28.dp),
                pressedShape = pressedShape,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceContainerHigh,
                contentColor = colors.onSurface,
            ),
            contentPadding = contentPadding,
        ) {
            Text(stringResource(CommonsR.string.cancel), textAlign = TextAlign.Center)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            enabled = enabled,
            shapes = ButtonDefaults.shapes(
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 8.dp),
                pressedShape = pressedShape,
            ),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor),
            contentPadding = contentPadding,
        ) {
            Text(stringResource(CommonsR.string.ok), textAlign = TextAlign.Center)
        }
    }
}
