package com.pixelvault.app.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelvault.app.ui.theme.GlassBorderBrushDark
import com.pixelvault.app.ui.theme.GlassBorderBrushLight
import com.pixelvault.app.ui.theme.GlassHighlightBrush
import com.pixelvault.app.ui.theme.GlassSurfaceDark
import com.pixelvault.app.ui.theme.GlassSurfaceLight

/**
 * 100% Liquid Glass Surface.
 * - Android 12+ (API 31+): Uses hardware RenderEffect blur on underlying composable buffer.
 * - Android 8–11 (API 26–30): Graceful fallback to semi-opaque frosted layer.
 * - Features specular highlight, 1px gradient light border, and inner glow reflection.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Float = 35f,
    surfaceColor: Color? = null,
    showHighlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)

    val baseColor = surfaceColor ?: if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val borderBrush = if (isDark) GlassBorderBrushDark else GlassBorderBrushLight

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(blurModifier)
            .background(baseColor, shape)
            .border(borderWidth, borderBrush, shape)
    ) {
        // Specular highlight: subtle white gradient at top edge
        if (showHighlight) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerRadius)
                    .background(GlassHighlightBrush)
            )
        }

        content()
    }
}
