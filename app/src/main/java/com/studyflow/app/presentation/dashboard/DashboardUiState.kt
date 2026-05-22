package com.studyflow.app.presentation.dashboard

import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.data.local.entity.ReflectionEntity

data class HabitWithStatus(
    val habit: HabitEntity,
    val isCompletedToday: Boolean
)

data class DashboardUiState(
    val greeting: String = "Good morning!",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tasksDueTodayCount: Int = 0,
    val tasksCompletedTodayCount: Int = 0,
    val tasksTotalTodayCount: Int = 0,
    val pomodoroSessionsTodayCount: Int = 0,
    val focusMinutesToday: Int = 0,
    val weeklyResourceTotal: Double = 0.0,
    val todayTasks: List<TaskEntity> = emptyList(),
    val activeHabits: List<HabitWithStatus> = emptyList(),
    val upcomingDeadlines: List<TaskEntity> = emptyList(),
    val selectedWorkspace: WorkspaceEntity? = null,
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val hasReflectedToday: Boolean = false,
    val todayReflection: ReflectionEntity? = null,
    val reflectionStreak: Int = 0,
    val allReflections: List<ReflectionEntity> = emptyList(),
    val momentumScore: Int = 0,
    val momentumState: String = "Stable ⚡",
    val showRecoverySuggestion: Boolean = false,
    val currentStudyState: String = "FOCUS",
    val studyStateOverride: String = "AUTO",
    val stateAdvice: String = "",
    val correlationInsight: String = ""
)
