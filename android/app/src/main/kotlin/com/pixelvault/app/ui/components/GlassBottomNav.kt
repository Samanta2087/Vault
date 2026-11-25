package com.pixelvault.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelvault.app.ui.theme.BrandAmber
import com.pixelvault.app.ui.theme.BrandGold

sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Photos : NavItem("photos", "Photos", Icons.Default.PhotoLibrary)
    object Albums : NavItem("albums", "Albums", Icons.Default.PhotoAlbum)
    object Search : NavItem("search", "Search", Icons.Default.Search)
    object Vault : NavItem("vault", "Vault", Icons.Default.Lock)
}

/**
 * Floating Liquid Glass Bottom Navigation Bar.
 * - Exactly matches user screenshot reference: floating pill style with rounded corners.
 * - 4 destinations: Photos · Albums · Search · Vault.
 * - Morphing selection pill with spring physics.
 */
@Composable
fun GlassBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(NavItem.Photos, NavItem.Albums, NavItem.Search, NavItem.Vault)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp)),
            cornerRadius = 32.dp,
            blurRadius = 40f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val interactionSource = remember { MutableInteractionSource() }

                    val activeColor = if (item == NavItem.Photos) BrandGold else MaterialTheme.colorScheme.primary
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "navColor"
                    )

                    // Pill item container
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onNavigate(item.route) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
                            ) {
                                Text(
                                    text = item.title,
                                    color = contentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
