package com.studyflow.app.presentation.analytics

import com.studyflow.app.data.local.dao.ResourceCategoryTotal
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.PomodoroSessionEntity

data class AnalyticsUiState(
    // Tasks Tab data
    val completedTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
    val taskCompletionRate: Float = 0f,
    val taskCategoryCounts: Map<String, Int> = emptyMap(),
    val weeklyTaskCompletions: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0), // Sun-Sat completions

    // Habits Tab data
    val habits: List<HabitEntity> = emptyList(),
    val habitHeatmapData: Map<Long, Int> = emptyMap(), // StartOfDay epoch mills -> count of habits done

    // Pomodoro Tab data
    val totalFocusHours: Double = 0.0,
    val dailyAverageFocusMinutes: Double = 0.0,
    val weeklyPomodoroSessions: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0), // Sun-Sat sessions count

    // Performance Signals tab data
    val monthlyTotalResources: Double = 0.0,
    val resourceCategoryTotals: List<ResourceCategoryTotal> = emptyList(),
    val biggestResourceCategory: String = "None",
    val last6MonthsResourceTrend: List<Pair<String, Double>> = emptyList(), // Month name -> total amount

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
