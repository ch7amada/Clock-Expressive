package org.fossify.clock.ui.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fossify.clock.R
import org.fossify.commons.R as CommonsR
import org.fossify.commons.extensions.performHapticFeedback
import kotlin.math.abs

private const val DragActionThresholdPx = 50f
private const val SwipeGuideDurationMillis = 2_000L
private val HandleSize = 64.dp

@Composable
fun RingingAlarmScreen(
    label: String,
    time: String,
    actionsEnabled: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            val sideBySide = maxWidth >= 620.dp && maxWidth > maxHeight
            val gutter = if (maxWidth < 360.dp) 16.dp else 24.dp
            val viewportHeight = maxHeight
            val contentModifier = Modifier
                .widthIn(max = if (sideBySide) 880.dp else 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = gutter, vertical = 24.dp)

            if (sideBySide) {
                Row(
                    modifier = contentModifier,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlarmTimeCard(label, time, Modifier.weight(1f))
                    RingingAlarmActions(actionsEnabled, onSnooze, onDismiss, Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                ) {
                    AlarmTimeCard(label, time, Modifier.fillMaxWidth())
                    RingingAlarmActions(actionsEnabled, onSnooze, onDismiss, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AlarmTimeCard(label: String, time: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 56.dp, topEnd = 24.dp, bottomEnd = 52.dp, bottomStart = 16.dp),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 212.dp)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            val timeStyle = MaterialTheme.typography.displayLargeEmphasized.copy(
                fontSize = 88.sp,
                lineHeight = 96.sp,
                fontFeatureSettings = "tnum",
            )
            Text(
                text = time,
                style = timeStyle,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = timeStyle.fontSize),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
        }
    }
}

@Composable
private fun RingingAlarmActions(
    enabled: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    val snooze = stringResource(CommonsR.string.snooze)
    val dismiss = stringResource(CommonsR.string.dismiss)
    val guide = stringResource(R.string.swipe_right_to_dismiss)
    var guideVisible by remember { mutableStateOf(false) }
    var guideRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(guideRequest, enabled) {
        guideVisible = enabled && guideRequest != 0
        if (guideVisible) {
            delay(SwipeGuideDurationMillis)
            guideVisible = false
        }
    }
    val guideAlpha by animateFloatAsState(
        targetValue = if (guideVisible) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "Swipe guidance",
    )

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AlarmSwipeTrack(enabled, guide, { guideRequest++ }, onSnooze, onDismiss)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Keep the physical action placement consistent with the translated swipe guide.
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            AlarmActionButton(
                text = if (rtl) dismiss else snooze,
                onClick = if (rtl) onDismiss else onSnooze,
                enabled = enabled,
                emphasized = rtl,
                leading = true,
                modifier = Modifier.weight(1f),
            )
            AlarmActionButton(
                text = if (rtl) snooze else dismiss,
                onClick = if (rtl) onSnooze else onDismiss,
                enabled = enabled,
                emphasized = !rtl,
                leading = false,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = guide,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = guideAlpha }
                .semantics {
                    if (guideVisible) liveRegion = LiveRegionMode.Polite else hideFromAccessibility()
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AlarmActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasized: Boolean,
    leading: Boolean,
    modifier: Modifier,
) {
    val start = if (leading) 28.dp else 6.dp
    val end = if (leading) 6.dp else 28.dp
    Button(
        shapes = ButtonDefaults.shapes(
            shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
            pressedShape = RoundedCornerShape(12.dp),
        ),
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        colors = if (emphasized) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
    ) {
        Text(text, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmSwipeTrack(
    enabled: Boolean,
    guide: String,
    onIncomplete: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val returnSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    var progress by remember { mutableFloatStateOf(0f) }
    var returnJob by remember { mutableStateOf<Job?>(null) }
    var dragDispatched by remember { mutableStateOf(false) }
    val handleMorph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Circle) }
    val handleColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(HandleSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val travelPx = with(LocalDensity.current) { ((maxWidth - HandleSize) / 2).toPx() }
        val dragState = rememberDraggableState { delta ->
            if (enabled && !dragDispatched && travelPx > 0f) {
                progress = (progress + delta / travelPx).coerceIn(-1f, 1f)
                // Keep the end zone in physical pixels, leaving the center neutral.
                val threshold = (travelPx - DragActionThresholdPx).coerceAtLeast(1f) / travelPx
                if (progress >= threshold || progress <= -threshold) {
                    dragDispatched = true
                    view.performHapticFeedback()
                    if (progress > 0f) onDismiss() else onSnooze()
                }
            }
        }
        Icon(
            painter = painterResource(CommonsR.drawable.ic_snooze_vector),
            contentDescription = null,
            modifier = Modifier.align(AbsoluteAlignment.CenterLeft).absolutePadding(left = 20.dp).size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            painter = painterResource(R.drawable.ic_alarm_off_vector),
            contentDescription = null,
            modifier = Modifier.align(AbsoluteAlignment.CenterRight).absolutePadding(right = 20.dp).size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier
                .size(HandleSize)
                .draggable(
                    state = dragState,
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        returnJob?.cancel()
                        dragDispatched = false
                    },
                    onDragStopped = {
                        if (!dragDispatched && enabled) {
                            onIncomplete()
                            returnJob = scope.launch {
                                Animatable(progress).animateTo(0f, returnSpec) {
                                    progress = value.coerceIn(-1f, 1f)
                                }
                                progress = 0f
                            }
                        }
                    },
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = progress * travelPx }
                    .drawWithCache {
                        val path = Path()
                        val scale = Matrix().apply { scale(x = size.width, y = size.height) }
                        onDrawWithContent {
                            handleMorph.toPath(abs(progress), path)
                            path.transform(scale)
                            path.translate(center - path.getBounds().center)
                            drawPath(path, handleColor)
                            clipPath(path) { this@onDrawWithContent.drawContent() }
                        }
                    }
                    .clickable(enabled = enabled, role = Role.Button, onClick = onIncomplete)
                    .semantics { contentDescription = guide },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_alarm_vector),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
