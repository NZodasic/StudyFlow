package com.studyflow.app.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.data.repository.HabitRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var habitsJob: Job? = null
    private var currentWorkspaceId: Long? = null

    init {
        val days = getDaysOfWeekMillis()
        _uiState.update { it.copy(daysOfWeek = days) }
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                currentWorkspaceId = settings?.selectedWorkspaceId
                _uiState.update { it.copy(hasActiveWorkspace = currentWorkspaceId != null) }
                observeHabits(currentWorkspaceId)
            }
        }
        viewModelScope.launch {
            workspaceRepository.getAllWorkspaces().collect { workspaces ->
                _uiState.update { it.copy(workspaces = workspaces) }
            }
        }
    }

    private fun observeHabits(workspaceId: Long?) {
        habitsJob?.cancel()
        habitsJob = viewModelScope.launch {
            val todayStart = getStartOfTodayMillis()
            val todayEnd = todayStart + 86399999L
            val weekStart = _uiState.value.daysOfWeek.firstOrNull() ?: todayStart
            val endOfWeek = weekStart + 7 * 86400000L - 1 // Sunday end

            combine(
                habitRepository.getAllHabits(workspaceId),
                habitRepository.getLogsForPeriod(weekStart, endOfWeek, workspaceId)
            ) { habits, logs ->
                habits.map { habit ->
                    val habitLogs = logs.filter { it.habitId == habit.id }
                    val isDoneToday = logs.any {
                        it.habitId == habit.id && it.dateMillis >= todayStart && it.dateMillis <= todayEnd
                    }
                    HabitWithWeeklyLogs(
                        habit = habit,
                        logs = habitLogs,
                        isCompletedToday = isDoneToday
                    )
                }
            }.collect { habitWithLogsList ->
                _uiState.update { it.copy(habits = habitWithLogsList, isLoading = false) }
            }
        }
    }

    fun addHabit(name: String, iconEmoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newHabit = HabitEntity(
                name = name.trim(),
                iconEmoji = iconEmoji.ifBlank { "⭐" },
                workspaceId = currentWorkspaceId
            )
            habitRepository.insertHabit(newHabit)
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabit(habitId, System.currentTimeMillis())
        }
    }

    fun deleteHabit(habit: HabitEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
            onComplete()
        }
    }

    fun selectWorkspace(workspaceId: Long) {
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettingsSuspended() ?: com.studyflow.app.data.local.entity.UserSettingsEntity()
            settingsRepository.updateSettings(settings.copy(selectedWorkspaceId = workspaceId))
        }
    }

    fun addWorkspace(name: String, emoji: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = workspaceRepository.insertWorkspace(
                com.studyflow.app.data.local.entity.WorkspaceEntity(
                    name = name.trim(),
                    iconEmoji = emoji,
                    colorHex = colorHex
                )
            )
            selectWorkspace(newId)
        }
    }

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDaysOfWeekMillis(): List<Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val days = mutableListOf<Long>()
        repeat(7) {
            days.add(calendar.timeInMillis)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }
}
