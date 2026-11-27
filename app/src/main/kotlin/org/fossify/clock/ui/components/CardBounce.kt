package org.fossify.clock.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.MutatorMutex
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal class CardBounceState {
    private val pulse = MutatorMutex()
    private var origin by mutableIntStateOf(0)
    private var progress by mutableFloatStateOf(0f)

    @Composable
    fun modifier(active: Boolean, index: Int, columns: Int): Modifier {
        val spring = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        val layoutDirection = LocalLayoutDirection.current
        var wasActive by remember { mutableStateOf(active) }

        LaunchedEffect(active, index, columns) {
            val activated = active && !wasActive
            wasActive = active
            if (!activated || !ValueAnimator.areAnimatorsEnabled()) return@LaunchedEffect

            pulse.mutate {
                origin = index
                val animation = AnimationState(0f)
                try {
                    animation.animateTo(1f, spring) {
                        progress = value
                        // Material button groups return as soon as a quick tap reaches 75% expansion.
                        if (value >= 0.75f) cancelAnimation()
                    }
                    animation.animateTo(0f, spring, sequentialAnimation = true) { progress = value }
                } finally {
                    progress = 0f
                }
            }
        }

        return Modifier.graphicsLayer {
            val distance = progress.coerceIn(-0.18f, 1f) * 8.dp.toPx()
            if (distance == 0f) return@graphicsLayer
            val columnDelta = index % columns - origin % columns
            val rowDelta = index / columns - origin / columns
            if (columnDelta == 0 && rowDelta == 0) {
                scaleX = 1f + 2f * distance / size.width.coerceAtLeast(1f)
                scaleY = 1f + 2f * distance / size.height.coerceAtLeast(1f)
            } else if (abs(columnDelta) <= 1 && abs(rowDelta) <= 1) {
                val horizontal = if (layoutDirection == LayoutDirection.Ltr) columnDelta else -columnDelta
                translationX = horizontal * distance
                translationY = rowDelta * distance
            }
        }
    }
}
