package com.pixelvault.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Liquid Glass Palette Constants
val GlassWhite = Color(0xCCFFFFFF)           // 80% white for light glass
val GlassDark = Color(0xCC12131C)            // Deep slate dark glass
val GlassBorderLight = Color(0x40FFFFFF)      // 25% specular light border
val GlassBorderDark = Color(0x20FFFFFF)       // 12% border for dark mode
val GlassHighlight = Color(0x33FFFFFF)        // Top edge specular shine
val GlassInnerGlow = Color(0x14FFFFFF)        // 8% ambient inner reflection
val GlassSurfaceLight = Color(0xB3FFFFFF)     // 70% semi-opaque frosted layer
val GlassSurfaceDark = Color(0xB3161824)      // 70% dark frosted surface

// Brand Accents (Immich / Google Photos Inspired)
val BrandGold = Color(0xFFF9AB00)            // Photos tab accent (matches screenshot)
val BrandAmber = Color(0xFFFFA000)
val BrandBlue = Color(0xFF4285F4)            // Material Google Blue
val BrandTeal = Color(0xFF00BFA5)            // Vault Accent
val BrandDanger = Color(0xFFEA4335)          // Trash / Alert

// Gradient Brushes for Glass Surface Edges
val GlassBorderBrushLight = Brush.verticalGradient(
    colors = listOf(
        Color(0x80FFFFFF), // 50% white at top
        Color(0x1AFFFFFF), // 10% white in middle
        Color(0x33FFFFFF)  // 20% white at bottom
    )
)

val GlassBorderBrushDark = Brush.verticalGradient(
    colors = listOf(
        Color(0x40FFFFFF),
        Color(0x0DFFFFFF),
        Color(0x20FFFFFF)
    )
)

val GlassHighlightBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x40FFFFFF),
        Color(0x00FFFFFF)
    )
)
