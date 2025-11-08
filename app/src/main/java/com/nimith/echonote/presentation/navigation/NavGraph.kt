package com.nimith.echonote.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nimith.echonote.presentation.features.dashboard.DashboardScreen
import com.nimith.echonote.presentation.features.recording.RecordingScreen

@Composable
fun EchoNoteNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard
    ) {
        composable<Screen.Dashboard> {
            DashboardScreen {
                navController.navigate(Screen.Recording)
            }
        }
        composable<Screen.Recording> {
            RecordingScreen {
                navController.popBackStack()
            }
        }
    }
}