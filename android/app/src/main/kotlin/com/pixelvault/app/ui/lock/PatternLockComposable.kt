package com.pixelvault.app.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * 3x3 Grid Pattern Lock.
 * - Requires a minimum of 5 dots.
 * - Tracks smooth gesture path and renders animated connection lines.
 * - Error shake feedback when pattern is incorrect.
 */
@Composable
fun PatternLockComposable(
    minDots: Int = 5,
    hasError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(hasError) {
        if (hasError) {
            selectedDots.clear()
            currentTouchPoint = null
            for (i in 0..2) {
                shakeOffset.animateTo(20f, tween(50))
                shakeOffset.animateTo(-20f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    val dotRadius = 14f
    val hitRadius = 55f

    Box(
        modifier = modifier
            .size(320.dp)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots.clear()
                        currentTouchPoint = offset
                    },
                    onDragEnd = {
                        if (selectedDots.size >= minDots) {
                            onPatternComplete(selectedDots.toList())
                        } else {
                            selectedDots.clear()
                        }
                        currentTouchPoint = null
                    },
                    onDragCancel = {
                        selectedDots.clear()
                        currentTouchPoint = null
                    },
                    onDrag = { change, _ ->
                        currentTouchPoint = change.position
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val cellWidth = size.width / 3f
            val cellHeight = size.height / 3f

            // Calculate dot center positions (0 to 8)
            val dotCenters = (0..8).map { index ->
                val col = index % 3
                val row = index / 3
                Offset(
                    x = col * cellWidth + cellWidth / 2f,
                    y = row * cellHeight + cellHeight / 2f
                )
            }

            // Check if current touch position collides with any unselected dot
            currentTouchPoint?.let { touch ->
                dotCenters.forEachIndexed { index, center ->
                    if (!selectedDots.contains(index)) {
                        val distance = hypot(touch.x - center.x, touch.y - center.y)
                        if (distance <= hitRadius) {
                            selectedDots.add(index)
                        }
                    }
                }
            }

            // 1. Draw connection lines between selected dots
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val p1 = dotCenters[selectedDots[i]]
                    val p2 = dotCenters[selectedDots[i + 1]]
                    drawLine(
                        color = Color(0xFFF9AB00),
                        start = p1,
                        end = p2,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw live line from last selected dot to active touch point
            if (selectedDots.isNotEmpty() && currentTouchPoint != null) {
                val lastCenter = dotCenters[selectedDots.last()]
                drawLine(
                    color = Color(0xFFF9AB00).copy(alpha = 0.7f),
                    start = lastCenter,
                    end = currentTouchPoint!!,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw 9 dots (outer ring + inner core)
            dotCenters.forEachIndexed { index, center ->
                val isSelected = selectedDots.contains(index)
                val dotColor = if (isSelected) Color(0xFFF9AB00) else Color.White.copy(alpha = 0.45f)

                // Outer halo if selected
                if (isSelected) {
                    drawCircle(
                        color = Color(0xFFF9AB00).copy(alpha = 0.25f),
                        radius = dotRadius * 2.5f,
                        center = center
                    )
                }

                // Inner core
                drawCircle(
                    color = dotColor,
                    radius = if (isSelected) dotRadius * 1.3f else dotRadius,
                    center = center
                )
            }
        }
    }
}
