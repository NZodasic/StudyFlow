package com.studyflow.app.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Tasks : Screen("tasks")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long) = "task_detail/$taskId"
    }
    data object Habits : Screen("habits")
    data object Pomodoro : Screen("pomodoro")
    data object Notes : Screen("notes")
    data object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    data object Resources : Screen("resources")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
    data object Timeline : Screen("timeline")
    data object Insights : Screen("insights")
}
