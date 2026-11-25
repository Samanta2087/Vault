package com.pixelvault.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalGlassBlurEnabled = compositionLocalOf { true }
val LocalGlassAlpha = compositionLocalOf { 0.85f }

private val DarkColorScheme = darkColorScheme(
    primary = BrandGold,
    secondary = BrandBlue,
    tertiary = BrandTeal,
    background = Color(0xFF0F1018),
    surface = Color(0xFF161824),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE6E6EA),
    onSurface = Color(0xFFE6E6EA),
    surfaceVariant = Color(0xFF222536),
    onSurfaceVariant = Color(0xFFC4C6D0)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGold,
    secondary = BrandBlue,
    tertiary = BrandTeal,
    background = Color(0xFFF8F9FE),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFF191C1E),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun PixelVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    photoColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        photoColor != null -> {
            // Seed dynamic scheme from active photo palette
            if (darkTheme) {
                DarkColorScheme.copy(primary = photoColor, secondary = photoColor.copy(alpha = 0.8f))
            } else {
                LightColorScheme.copy(primary = photoColor, secondary = photoColor.copy(alpha = 0.8f))
            }
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
