package com.studyflow.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import com.studyflow.app.data.local.entity.ReflectionEntity
import com.studyflow.app.data.local.entity.ResourceEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.UserSettingsEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.data.repository.HabitRepository
import com.studyflow.app.data.repository.PomodoroRepository
import com.studyflow.app.data.repository.ReflectionRepository
import com.studyflow.app.data.repository.ResourceRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class DashboardInnerData(
    val reflections: List<ReflectionEntity>,
    val resources: List<ResourceEntity>,
    val settings: UserSettingsEntity?,
    val habitsWithStatus: List<HabitWithStatus>
)

private data class DashboardComputedData(
    val activeTodayTasks: List<TaskEntity>,
    val completedTodayCount: Int,
    val totalTodayCount: Int,
    val dueTodayCount: Int,
    val upcomingDeadlines: List<TaskEntity>,
    val sessionsCount: Int,
    val focusMinutesToday: Int,
    val weeklyTotal: Double,
    val habitsWithStatus: List<HabitWithStatus>,
    val todayReflection: ReflectionEntity?,
    val reflectionStreak: Int,
    val momentumScore: Int,
    val momentumState: String,
    val showRecoverySuggestion: Boolean,
    val currentStudyState: String,
    val studyStateOverride: String,
    val stateAdvice: String,
    val correlationInsight: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val resourceRepository: ResourceRepository,
    private val habitRepository: HabitRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val reflectionRepository: ReflectionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var dataJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.getUserSettings(),
                workspaceRepository.getAllWorkspaces()
            ) { settings, workspaces ->
                val selectedId = settings?.selectedWorkspaceId
                val selected = workspaces.find { it.id == selectedId }
                Pair(selected, workspaces)
            }.collect { (selected, workspacesList) ->
                _uiState.update {
                    it.copy(
                        selectedWorkspace = selected,
                        workspaces = workspacesList
                    )
                }
                observeDashboardData(selected?.id)
            }
        }
    }

    private fun observeDashboardData(workspaceId: Long?) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            val todayStart = getStartOfTodayMillis()
            val todayEnd = getEndOfTodayMillis()
            val weekStart = getStartOfWeekMillis()

            val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Good morning, Student!"
                in 12..16 -> "Good afternoon, Student!"
                else -> "Good evening, Student!"
            }
            _uiState.update { it.copy(greeting = greeting) }

            val innerFlow = combine(
                reflectionRepository.getAllReflections(),
                resourceRepository.getAllResources(workspaceId),
                settingsRepository.getUserSettings(),
                combine(
                    habitRepository.getAllHabits(workspaceId),
                    habitRepository.getLogsForPeriod(todayStart, todayEnd, workspaceId)
                ) { habits, logs ->
                    habits.map { habit ->
                        HabitWithStatus(
                            habit = habit,
                            isCompletedToday = logs.any { it.habitId == habit.id }
                        )
                    }
                }
            ) { reflections, resources, settings, habitsWithStatus ->
                DashboardInnerData(reflections, resources, settings, habitsWithStatus)
            }

            combine(
                taskRepository.getAllTasks(workspaceId),
                pomodoroRepository.getAllSessions(workspaceId),
                resourceRepository.getWeeklyTotal(weekStart, workspaceId),
                innerFlow
            ) { tasks, allSessions, weeklyTotal, inner ->
                val reflections = inner.reflections
                val resources = inner.resources
                val settings = inner.settings
                val habitsWithStatus = inner.habitsWithStatus

                val todaySessions = allSessions.filter { it.completedAtMillis in todayStart..todayEnd }
                val sessionsCount = todaySessions.size
                val focusMinutesToday = todaySessions.sumOf { it.durationMinutes }

                val todaySleepLogs = resources.filter {
                    it.category.equals("Sleep", ignoreCase = true) && it.dateMillis in todayStart..todayEnd
                }
                val todaySleep = todaySleepLogs.sumOf { it.amount }
                val hasSleepLog = todaySleepLogs.isNotEmpty()
                val todayCaffeine = resources.filter {
                    it.category.equals("Caffeine", ignoreCase = true) && it.dateMillis in todayStart..todayEnd
                }.sumOf { it.amount }
                val overdueTasksCount = tasks.count { task ->
                    task.dueDateMillis != null && task.dueDateMillis < todayStart && !task.isCompleted
                }

                val activeTodayTasks = tasks.filter { task ->
                    !task.isCompleted && (task.dueDateMillis == null || task.dueDateMillis <= todayEnd)
                }
                val completedToday = tasks.filter { task ->
                    task.isCompleted && task.completedAtMillis != null && task.completedAtMillis in todayStart..todayEnd
                }
                val tasksCompletedCount = completedToday.size
                val tasksTotalCount = activeTodayTasks.size + completedToday.size

                val upcomingDeadlines = tasks.filter { task ->
                    task.dueDateMillis != null && task.dueDateMillis > todayEnd && !task.isCompleted
                }.sortedBy { task -> task.dueDateMillis ?: Long.MAX_VALUE }.take(3)

                val todayReflection = reflections.firstOrNull { it.dateMillis in todayStart..todayEnd }
                val energyLevel = todayReflection?.energyLevel ?: 3
                val stressLevel = todayReflection?.stressLevel ?: 3

                val habitScore = if (habitsWithStatus.isNotEmpty()) {
                    (habitsWithStatus.count { it.isCompletedToday }.toFloat() / habitsWithStatus.size) * 30f
                } else {
                    15f
                }
                val sessionCountScore = (sessionsCount * 8).coerceAtMost(25)
                val ratedTodaySessions = todaySessions.filter { it.focusRating > 0 }
                val avgRating = if (ratedTodaySessions.isNotEmpty()) {
                    ratedTodaySessions.map { session -> session.focusRating }.average()
                } else {
                    0.0
                }
                val ratingScore = if (avgRating > 0.0) (avgRating / 5.0 * 10.0).toFloat() else 5f
                val focusScore = sessionCountScore + ratingScore
                val taskScore = if (tasksTotalCount > 0) {
                    (tasksCompletedCount.toFloat() / tasksTotalCount) * 25f
                } else {
                    12.5f
                }
                var wellnessBonus = 0
                if (hasSleepLog && todaySleep >= 7.0) wellnessBonus += 10
                if (todayReflection != null && stressLevel <= 2) wellnessBonus += 5

                val momentumScore = (habitScore + focusScore + taskScore + wellnessBonus).toInt().coerceIn(0, 100)
                val momentumState = when {
                    momentumScore >= 80 -> "Peak Momentum"
                    momentumScore >= 60 -> "Momentum Rising"
                    momentumScore >= 40 -> "Stable"
                    momentumScore >= 20 -> "Recovery Phase"
                    else -> "Burnout Risk"
                }

                val currentOverride = settings?.studyStateOverride ?: "AUTO"
                val computedState = if (currentOverride != "AUTO") {
                    currentOverride
                } else {
                    val isConfirmedLowSleep = hasSleepLog && todaySleep < 5.0
                    val isBurnout = (overdueTasksCount >= 3 && stressLevel >= 4) ||
                        (todayCaffeine > 300.0 && isConfirmedLowSleep)
                    val isRecovery = isConfirmedLowSleep || energyLevel <= 2
                    val isPeak = momentumScore >= 80
                    when {
                        isBurnout -> "BURNOUT"
                        isRecovery -> "RECOVERY"
                        isPeak -> "PEAK"
                        else -> "FOCUS"
                    }
                }

                val stateAdvice = when (computedState) {
                    "RECOVERY" -> if (hasSleepLog && todaySleep < 5.0) {
                        "Low sleep logged today. Prioritize light review, hydration, and shorter sessions."
                    } else {
                        "Low energy detected. Choose easy tasks and protect recovery time today."
                    }
                    "BURNOUT" -> if (todayCaffeine > 300.0 && hasSleepLog && todaySleep < 5.0) {
                        "High caffeine plus low sleep is a burnout risk. Pause deep work and reset."
                    } else {
                        "High stress and overdue work detected. Shrink the list and take a lighter day."
                    }
                    "PEAK" -> "Momentum is strong. This is a good window for deep focus or hard tasks."
                    else -> "StudyFlow OS is tracking your signals. Log sleep, caffeine, focus, and reflection to improve recommendations."
                }
                val correlationInsight = buildCorrelationInsight(resources, allSessions, reflections)

                val settingsUpdate = settings?.copy(
                    currentStudyState = computedState,
                    isRecoveryMode = computedState == "RECOVERY"
                )
                if (settingsUpdate != null && (settings.currentStudyState != computedState || settings.isRecoveryMode != settingsUpdate.isRecoveryMode)) {
                    viewModelScope.launch {
                        settingsRepository.updateSettings(settingsUpdate)
                    }
                }

                DashboardComputedData(
                    activeTodayTasks = activeTodayTasks,
                    completedTodayCount = tasksCompletedCount,
                    totalTodayCount = tasksTotalCount,
                    dueTodayCount = activeTodayTasks.count { task ->
                        task.dueDateMillis != null && task.dueDateMillis <= todayEnd
                    },
                    upcomingDeadlines = upcomingDeadlines,
                    sessionsCount = sessionsCount,
                    focusMinutesToday = focusMinutesToday,
                    weeklyTotal = weeklyTotal,
                    habitsWithStatus = habitsWithStatus,
                    todayReflection = todayReflection,
                    reflectionStreak = calculateReflectionStreak(reflections),
                    momentumScore = momentumScore,
                    momentumState = momentumState,
                    showRecoverySuggestion = computedState == "RECOVERY" || computedState == "BURNOUT",
                    currentStudyState = computedState,
                    studyStateOverride = currentOverride,
                    stateAdvice = stateAdvice,
                    correlationInsight = correlationInsight
                )
            }.collect { data ->
                _uiState.update { state ->
                    state.copy(
                        todayTasks = data.activeTodayTasks.filter { !it.isCompleted }.take(5),
                        tasksDueTodayCount = data.dueTodayCount,
                        tasksCompletedTodayCount = data.completedTodayCount,
                        tasksTotalTodayCount = data.totalTodayCount,
                        upcomingDeadlines = data.upcomingDeadlines,
                        pomodoroSessionsTodayCount = data.sessionsCount,
                        focusMinutesToday = data.focusMinutesToday,
                        weeklyResourceTotal = data.weeklyTotal,
                        activeHabits = data.habitsWithStatus,
                        hasReflectedToday = data.todayReflection != null,
                        todayReflection = data.todayReflection,
                        reflectionStreak = data.reflectionStreak,
                        allReflections = emptyList(),
                        momentumScore = data.momentumScore,
                        momentumState = data.momentumState,
                        showRecoverySuggestion = data.showRecoverySuggestion,
                        currentStudyState = data.currentStudyState,
                        studyStateOverride = data.studyStateOverride,
                        stateAdvice = data.stateAdvice,
                        correlationInsight = data.correlationInsight,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectWorkspace(workspaceId: Long?) {
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettingsSuspended() ?: UserSettingsEntity()
            settingsRepository.updateSettings(settings.copy(selectedWorkspaceId = workspaceId))
        }
    }

    fun deleteWorkspace(workspace: WorkspaceEntity) {
        viewModelScope.launch {
            if (_uiState.value.selectedWorkspace?.id == workspace.id) {
                selectWorkspace(null)
            }
            workspaceRepository.deleteWorkspace(workspace)
        }
    }

    fun addWorkspace(name: String, emoji: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = workspaceRepository.insertWorkspace(
                WorkspaceEntity(
                    name = name.trim(),
                    iconEmoji = emoji,
                    colorHex = colorHex
                )
            )
            selectWorkspace(newId)
        }
    }

    fun addReflection(
        accomplishments: String,
        energyLevel: Int,
        mood: String,
        distraction: String,
        rating: Int,
        stressLevel: Int,
        oneWord: String,
        madeProductive: String
    ) {
        viewModelScope.launch {
            val todayStart = getStartOfTodayMillis()
            val existing = reflectionRepository.getReflectionByDateSuspended(todayStart, todayStart + 86399999L)
            if (existing != null) {
                reflectionRepository.updateReflection(
                    existing.copy(
                        accomplishments = accomplishments,
                        energyLevel = energyLevel,
                        mood = mood,
                        biggestDistraction = distraction,
                        productivityRating = rating,
                        stressLevel = stressLevel,
                        oneWord = oneWord,
                        madeProductive = madeProductive
                    )
                )
            } else {
                reflectionRepository.insertReflection(
                    ReflectionEntity(
                        dateMillis = todayStart,
                        accomplishments = accomplishments,
                        energyLevel = energyLevel,
                        mood = mood,
                        biggestDistraction = distraction,
                        productivityRating = rating,
                        stressLevel = stressLevel,
                        oneWord = oneWord,
                        madeProductive = madeProductive
                    )
                )
            }
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabit(habitId, System.currentTimeMillis())
        }
    }

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            val tasks = _uiState.value.todayTasks
            val currentTask = tasks.find { it.id == taskId }
            if (currentTask != null) {
                taskRepository.toggleTaskCompletion(taskId, !currentTask.isCompleted)
            } else {
                val allTasks = taskRepository.getAllTasksSuspended(_uiState.value.selectedWorkspace?.id)
                val task = allTasks.find { it.id == taskId } ?: return@launch
                taskRepository.toggleTaskCompletion(taskId, !task.isCompleted)
            }
        }
    }

    fun turnOnRecoveryMode() {
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettingsSuspended() ?: UserSettingsEntity()
            settingsRepository.updateSettings(
                settings.copy(
                    isRecoveryMode = true,
                    currentStudyState = "RECOVERY",
                    studyStateOverride = "RECOVERY"
                )
            )
        }
    }

    fun updateStudyStateOverride(state: String) {
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettingsSuspended() ?: UserSettingsEntity()
            val newState = if (state == "AUTO") {
                settings.copy(studyStateOverride = "AUTO", isRecoveryMode = false)
            } else {
                settings.copy(
                    studyStateOverride = state,
                    currentStudyState = state,
                    isRecoveryMode = state == "RECOVERY"
                )
            }
            settingsRepository.updateSettings(newState)
        }
    }

    private fun buildCorrelationInsight(
        resources: List<ResourceEntity>,
        sessions: List<PomodoroSessionEntity>,
        reflections: List<ReflectionEntity>
    ): String {
        val sleepByDay = resources
            .filter { it.category.equals("Sleep", ignoreCase = true) }
            .groupBy { getStartOfDayMillis(it.dateMillis) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val focusMinutesByDay = sessions
            .groupBy { getStartOfDayMillis(it.completedAtMillis) }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
        val daysWithSleepAndFocus = sleepByDay.keys.intersect(focusMinutesByDay.keys)

        if (daysWithSleepAndFocus.size >= 3) {
            val highSleepFocus = daysWithSleepAndFocus
                .filter { day -> (sleepByDay[day] ?: 0.0) >= 7.0 }
                .map { day -> focusMinutesByDay[day] ?: 0 }
            val lowSleepFocus = daysWithSleepAndFocus
                .filter { day -> (sleepByDay[day] ?: 0.0) < 7.0 }
                .map { day -> focusMinutesByDay[day] ?: 0 }
            if (highSleepFocus.isNotEmpty() && lowSleepFocus.isNotEmpty()) {
                val difference = highSleepFocus.average() - lowSleepFocus.average()
                if (abs(difference) >= 10.0) {
                    return if (difference > 0) {
                        "Your focus time trends higher on 7h+ sleep days."
                    } else {
                        "Your recent focus time has not improved on 7h+ sleep days yet."
                    }
                }
            }
        }

        val ratedSessions = sessions.filter { it.focusRating > 0 }
        if (ratedSessions.size >= 3) {
            val avgRating = ratedSessions.map { it.focusRating }.average()
            return "Your recent focus quality average is ${String.format(Locale.getDefault(), "%.1f", avgRating)}/5."
        }

        if (reflections.size >= 3) {
            val avgEnergy = reflections.takeLast(7).map { it.energyLevel }.average()
            return "Your recent reflection baseline is ${String.format(Locale.getDefault(), "%.1f", avgEnergy)}/5 energy."
        }

        return "Log Sleep, Caffeine, Focus, and Reflection for a few days to unlock real correlations."
    }

    private fun calculateReflectionStreak(reflections: List<ReflectionEntity>): Int {
        if (reflections.isEmpty()) return 0
        val sortedDates = reflections.map { getStartOfDayMillis(it.dateMillis) }.distinct().sortedDescending()
        val todayStart = getStartOfTodayMillis()
        val yesterdayStart = todayStart - 86400000L

        val firstDate = sortedDates.firstOrNull() ?: return 0
        if (firstDate != todayStart && firstDate != yesterdayStart) {
            return 0
        }

        var streak = 1
        var currentDay = firstDate
        for (i in 1 until sortedDates.size) {
            val prevDay = sortedDates[i]
            val diff = currentDay - prevDay
            if (diff in 80000000L..92000000L) {
                streak++
                currentDay = prevDay
            } else if (diff < 80000000L) {
                continue
            } else {
                break
            }
        }
        return streak
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

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getStartOfWeekMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
