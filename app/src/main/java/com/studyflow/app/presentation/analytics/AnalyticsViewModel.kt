package com.studyflow.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.dao.ResourceCategoryTotal
import com.studyflow.app.data.repository.ResourceRepository
import com.studyflow.app.data.repository.HabitRepository
import com.studyflow.app.data.repository.PomodoroRepository
import com.studyflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.studyflow.app.data.repository.SettingsRepository
import kotlinx.coroutines.Job

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val resourceRepository: ResourceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var habitsJob: Job? = null
    private var pomodoroJob: Job? = null
    private var expensesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                val workspaceId = settings?.selectedWorkspaceId
                loadTaskAnalytics(workspaceId)
                loadHabitAnalytics(workspaceId)
                loadPomodoroAnalytics(workspaceId)
                loadResourceAnalytics(workspaceId)
            }
        }
    }

    private fun loadTaskAnalytics(workspaceId: Long?) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            taskRepository.getAllTasks(workspaceId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage) }
                }
                .collect { tasks ->
                    val completed = tasks.count { it.isCompleted }
                    val total = tasks.size
                    val rate = if (total > 0) completed.toFloat() / total.toFloat() else 0f
                    val categories = tasks.groupBy { it.category }.mapValues { it.value.size }

                    // Weekly tasks completed (Sunday to Saturday)
                    val startOfWeek = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endOfWeek = (startOfWeek.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_WEEK, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    val weeklyCompletions = IntArray(7) { 0 }
                    val cal = Calendar.getInstance()
                    tasks.forEach { task ->
                        if (task.isCompleted && task.dueDateMillis != null && task.dueDateMillis in startOfWeek.timeInMillis..endOfWeek.timeInMillis) {
                            cal.timeInMillis = task.dueDateMillis
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 (Sunday) to 7 (Saturday)
                            weeklyCompletions[dayOfWeek - 1]++
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            completedTaskCount = completed,
                            totalTaskCount = total,
                            taskCompletionRate = rate,
                            taskCategoryCounts = categories,
                            weeklyTaskCompletions = weeklyCompletions.toList()
                        )
                    }
                }
        }
    }

    private fun loadHabitAnalytics(workspaceId: Long?) {
        val startOf30Days = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -29)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOf30Days = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        habitsJob?.cancel()
        habitsJob = viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(workspaceId),
                habitRepository.getLogsForPeriod(startOf30Days.timeInMillis, endOf30Days.timeInMillis, workspaceId)
            ) { habits, logs ->
                Pair(habits, logs)
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
            .collect { (habits, logs) ->
                val heatmap = logs.groupBy { log ->
                    getStartOfDayMillis(log.dateMillis)
                }.mapValues { it.value.size }

                _uiState.update { state ->
                    state.copy(
                        habits = habits,
                        habitHeatmapData = heatmap
                    )
                }
            }
        }
    }

    private fun loadPomodoroAnalytics(workspaceId: Long?) {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            pomodoroRepository.getAllSessions(workspaceId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage) }
                }
                .collect { sessions ->
                    val startOfWeek = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endOfWeek = (startOfWeek.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_WEEK, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    val weeklySessions = IntArray(7) { 0 }
                    var totalMinutes = 0
                    val cal = Calendar.getInstance()

                    sessions.forEach { session ->
                        if (session.completedAtMillis in startOfWeek.timeInMillis..endOfWeek.timeInMillis) {
                            cal.timeInMillis = session.completedAtMillis
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            weeklySessions[dayOfWeek - 1]++
                            totalMinutes += session.durationMinutes
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            totalFocusHours = totalMinutes / 60.0,
                            dailyAverageFocusMinutes = totalMinutes / 7.0,
                            weeklyPomodoroSessions = weeklySessions.toList()
                        )
                    }
                }
        }
    }

    private fun loadResourceAnalytics(workspaceId: Long?) {
        expensesJob?.cancel()
        expensesJob = viewModelScope.launch {
            resourceRepository.getAllResources(workspaceId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage) }
                }
                .collect { allResources ->
                    val now = Calendar.getInstance()
                    val currentMonthStart = (now.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val currentMonthEnd = (now.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    // Current month calculations
                    val currentMonthResources = allResources.filter { it.dateMillis in currentMonthStart..currentMonthEnd }
                    val totalAmount = currentMonthResources.sumOf { it.amount }

                    val categoryGroups = currentMonthResources.groupBy { it.category }
                    val catTotals = categoryGroups.map { (cat, list) ->
                        ResourceCategoryTotal(cat, list.sumOf { it.amount })
                    }
                    val biggestCat = catTotals.maxByOrNull { it.total }?.category ?: "None"

                    // Last 6 months trend
                    val last6MonthsTrend = mutableListOf<Pair<String, Double>>()
                    val formatter = SimpleDateFormat("MMM", Locale.getDefault())

                    for (i in 5 downTo 0) {
                        val monthCal = Calendar.getInstance().apply {
                            add(Calendar.MONTH, -i)
                        }
                        val start = (monthCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val end = (monthCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val monthResources = allResources.filter { it.dateMillis in start..end }
                        val monthTotal = monthResources.sumOf { it.amount }
                        val label = formatter.format(monthCal.time)
                        last6MonthsTrend.add(Pair(label, monthTotal))
                    }

                    _uiState.update { state ->
                        state.copy(
                            monthlyTotalResources = totalAmount,
                            resourceCategoryTotals = catTotals,
                            biggestResourceCategory = biggestCat,
                            last6MonthsResourceTrend = last6MonthsTrend,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun getStartOfDayMillis(timeMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
