package com.pixelvault.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pixelvault.app.security.AppLockManager
import com.pixelvault.app.security.LockState
import com.pixelvault.app.ui.admin.AdminScreen
import com.pixelvault.app.ui.admin.AdminViewModel
import com.pixelvault.app.ui.albums.AlbumsScreen
import com.pixelvault.app.ui.albums.AlbumsViewModel
import com.pixelvault.app.ui.lock.LockScreen
import com.pixelvault.app.ui.lock.LockViewModel
import com.pixelvault.app.ui.lock.VaultScreen
import com.pixelvault.app.ui.lock.VaultViewModel
import com.pixelvault.app.ui.search.SearchScreen
import com.pixelvault.app.ui.search.SearchViewModel
import com.pixelvault.app.ui.timeline.TimelineScreen
import com.pixelvault.app.ui.timeline.TimelineViewModel
import com.pixelvault.app.ui.viewer.ViewerScreen
import com.pixelvault.app.ui.viewer.ViewerViewModel

@Composable
fun PixelVaultNavigation(
    appLockManager: AppLockManager,
    modifier: Modifier = Modifier
) {
    val lockState by appLockManager.lockState.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as PixelVaultApp

    val lockViewModel: LockViewModel = viewModel {
        LockViewModel(
            appLockManager = app.appLockManager,
            biometricHelper = app.biometricHelper,
            bruteForceGuard = app.bruteForceGuard
        )
    }

    if (lockState == LockState.APP_LOCKED) {
        LockScreen(
            viewModel = lockViewModel,
            onUnlocked = { /* AppLockManager handles state change to UNLOCKED */ },
            isVault = false
        )
    } else {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "photos",
            modifier = modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(250)) }
        ) {
            composable("photos") {
                val timelineVm: TimelineViewModel = viewModel()
                TimelineScreen(
                    viewModel = timelineVm,
                    onNavigate = { route ->
                        if (route != "photos") {
                            navController.navigate(route) {
                                popUpTo("photos") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onMediaClick = { media ->
                        navController.navigate("viewer/${media.id}")
                    }
                )
            }

            composable("albums") {
                val albumsVm: AlbumsViewModel = viewModel()
                AlbumsScreen(
                    viewModel = albumsVm,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("photos") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("search") {
                val searchVm: SearchViewModel = viewModel()
                SearchScreen(
                    viewModel = searchVm,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("photos") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("vault") {
                val vaultVm: VaultViewModel = viewModel {
                    VaultViewModel(appLockManager = app.appLockManager)
                }
                VaultScreen(
                    vaultViewModel = vaultVm,
                    lockViewModel = lockViewModel,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("photos") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("settings") {
                val adminVm: AdminViewModel = viewModel()
                AdminScreen(
                    viewModel = adminVm,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("photos") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(
                route = "viewer/{mediaId}",
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                val viewerVm: ViewerViewModel = viewModel()
                ViewerScreen(
                    initialMediaId = mediaId,
                    viewModel = viewerVm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
