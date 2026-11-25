package com.pixelvault.app.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PinEntryComposable(
    pinLength: Int = 6,
    hasError: Boolean = false,
    onPinComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(hasError) {
        if (hasError) {
            pin = ""
            // Trigger horizontal error shake
            for (i in 0..2) {
                shakeOffset.animateTo(25f, tween(50))
                shakeOffset.animateTo(-25f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN dot indicators (filled or empty)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Liquid Glass Numpad (3x4 grid: 1-9, empty, 0, backspace)
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            buttons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (key.isNotEmpty()) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "DEL" -> {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        }
                                        else -> {
                                            if (pin.length < pinLength) {
                                                pin += key
                                                if (pin.length == pinLength) {
                                                    onPinComplete(pin)
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (key.isNotEmpty()) {
                                Text(
                                    text = key,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
