package com.studyflow.app.presentation.habits

import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.HabitLogEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity

data class HabitWithWeeklyLogs(
    val habit: HabitEntity,
    val logs: List<HabitLogEntity>,
    val isCompletedToday: Boolean
)

data class HabitUiState(
    val habits: List<HabitWithWeeklyLogs> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val daysOfWeek: List<Long> = emptyList(), // Start of day milliseconds for Mon-Sun of current week
    val hasActiveWorkspace: Boolean = false,
    val workspaces: List<WorkspaceEntity> = emptyList()
)
