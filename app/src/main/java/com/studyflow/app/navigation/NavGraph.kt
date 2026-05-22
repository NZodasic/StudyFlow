package com.studyflow.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.studyflow.app.presentation.dashboard.DashboardScreen
import com.studyflow.app.presentation.tasks.TaskListScreen
import com.studyflow.app.presentation.tasks.TaskDetailScreen
import com.studyflow.app.presentation.habits.HabitScreen
import com.studyflow.app.presentation.pomodoro.PomodoroScreen
import com.studyflow.app.presentation.resources.ResourceScreen
import com.studyflow.app.presentation.notes.NoteListScreen
import com.studyflow.app.presentation.notes.NoteDetailScreen
import com.studyflow.app.presentation.analytics.AnalyticsScreen
import com.studyflow.app.presentation.settings.SettingsScreen
import com.studyflow.app.presentation.timeline.TimelineScreen
import com.studyflow.app.presentation.insights.InsightsScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard)
    data object Tasks : BottomNavItem(Screen.Tasks.route, "Tasks", Icons.Default.TaskAlt)
    data object Habits : BottomNavItem(Screen.Habits.route, "Habits", Icons.Default.Autorenew)
    data object Pomodoro : BottomNavItem(Screen.Pomodoro.route, "Focus", Icons.Default.Timer)
    data object Resources : BottomNavItem(Screen.Resources.route, "Signals", Icons.Default.Speed)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Tasks,
    BottomNavItem.Habits,
    BottomNavItem.Pomodoro,
    BottomNavItem.Resources
)

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToTimeline = { navController.navigate(Screen.Timeline.route) },
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToTasksTab = {
                    navController.navigate(Screen.Tasks.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToHabitsTab = {
                    navController.navigate(Screen.Habits.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPomodoroTab = {
                    navController.navigate(Screen.Pomodoro.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToResourcesTab = {
                    navController.navigate(Screen.Resources.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }
        composable(Screen.Tasks.route) {
            TaskListScreen(
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Habits.route) {
            HabitScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.Pomodoro.route) {
            PomodoroScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Resources.route) {
            ResourceScreen()
        }
        composable(Screen.Notes.route) {
            NoteListScreen(
                onNavigateToDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNote = { targetNoteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(targetNoteId))
                }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Timeline.route) {
            TimelineScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Insights.route) {
            InsightsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

