package com.studyflow.app.presentation.pomodoro

import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import com.studyflow.app.data.local.entity.TaskEntity

enum class PomodoroMode {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

data class PomodoroUiState(
    val secondsRemaining: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val sessionCount: Int = 1, // 1 to 4 focus sessions before long break
    val taskLabel: String = "",
    val history: List<PomodoroSessionEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val isLoading: Boolean = false,
    val showReflectionDialog: Boolean = false,
    val tempRating: Int = 5,
    val tempReflection: String = "",
    val tempDistractions: Int = 0,
    val appExitsCount: Int = 0
)

