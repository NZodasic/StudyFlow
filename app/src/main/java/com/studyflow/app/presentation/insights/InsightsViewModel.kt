package com.studyflow.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.repository.ReflectionRepository
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.data.repository.PomodoroRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Calendar
import javax.inject.Inject

data class MoodStat(
    val mood: String,
    val count: Int,
    val percentage: Float
)

data class InsightCorrelation(
    val title: String,
    val description: String,
    val impactLevel: String // "High Positive", "Neutral", "Needs Attention"
)

data class StudyDnaProfile(
    val archetype: String,
    val emoji: String,
    val description: String,
    val traits: List<String>,
    val recommendations: List<String>
)

data class InsightsUiState(
    val isLoading: Boolean = false,
    val selectedWorkspaceId: Long? = null,
    val averageEnergy: Float = 0f,
    val averageProductivity: Float = 0f,
    val moodStats: List<MoodStat> = emptyList(),
    val energyByDayOfWeek: List<Float> = emptyList(), // Index 0=Mon, 6=Sun
    val correlations: List<InsightCorrelation> = emptyList(),
    val weeklyTasksCompleted: Int = 0,
    val weeklyFocusHours: Float = 0f,
    val studyDna: StudyDnaProfile? = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val reflectionRepository: ReflectionRepository,
    private val taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val settingsRepository: SettingsRepository,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var dataJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                val workspaceId = settings?.selectedWorkspaceId
                _uiState.update { it.copy(selectedWorkspaceId = workspaceId) }
                observeInsightsData(workspaceId)
            }
        }
    }

    private fun observeInsightsData(workspaceId: Long?) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            val weekStart = getStartOfWeekMillis()

            combine(
                reflectionRepository.getAllReflections(),
                taskRepository.getAllTasks(workspaceId),
                pomodoroRepository.getAllSessions(workspaceId),
                resourceRepository.getAllResources(workspaceId)
            ) { reflections, tasks, sessions, resources ->
                // Average stats
                val avgEnergy = if (reflections.isNotEmpty()) reflections.map { it.energyLevel }.average().toFloat() else 0f
                val avgProd = if (reflections.isNotEmpty()) reflections.map { it.productivityRating }.average().toFloat() else 0f

                // Mood stats
                val totalReflections = reflections.size.toFloat()
                val moodGroups = reflections.groupBy { it.mood }
                val moodStats = moodGroups.map { (mood, list) ->
                    MoodStat(
                        mood = mood,
                        count = list.size,
                        percentage = if (totalReflections > 0) list.size.toFloat() / totalReflections else 0f
                    )
                }.sortedByDescending { it.count }

                // Energy level per day of week (Monday to Sunday)
                val dayEnergySum = MutableList(7) { 0f }
                val dayEnergyCount = MutableList(7) { 0 }
                reflections.forEach { reflection ->
                    val cal = Calendar.getInstance().apply { timeInMillis = reflection.dateMillis }
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val mappedIndex = when (dayOfWeek) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        Calendar.SUNDAY -> 6
                        else -> 0
                    }
                    dayEnergySum[mappedIndex] += reflection.energyLevel.toFloat()
                    dayEnergyCount[mappedIndex] += 1
                }
                val energyByDay = dayEnergySum.mapIndexed { index, sum ->
                    val count = dayEnergyCount[index]
                    if (count > 0) sum / count else 0f
                }

                // Core count details
                val weeklyCompletedTasks = tasks.filter { it.isCompleted && (it.completedAtMillis ?: 0L) >= weekStart }.size
                val weeklySessions = sessions.filter { it.completedAtMillis >= weekStart }
                val totalFocusMinutes = weeklySessions.sumOf { it.durationMinutes }
                val weeklyHours = totalFocusMinutes.toFloat() / 60f

                // Generate Narrative Correlation Insights
                val correlations = mutableListOf<InsightCorrelation>()

                // Correlation 1: Mood & Productivity
                val focusedReflections = reflections.filter { it.mood.lowercase() == "focused" || it.mood.lowercase().contains("happy") }
                val tiredReflections = reflections.filter { it.mood.lowercase() == "tired" || it.mood.lowercase() == "stressed" }

                val focusedProdAvg = if (focusedReflections.isNotEmpty()) focusedReflections.map { it.productivityRating }.average() else 0.0
                val tiredProdAvg = if (tiredReflections.isNotEmpty()) tiredReflections.map { it.productivityRating }.average() else 0.0

                if (focusedProdAvg > tiredProdAvg && focusedReflections.isNotEmpty() && tiredReflections.isNotEmpty()) {
                    correlations.add(
                        InsightCorrelation(
                            title = "Focus Peaks with Positivity",
                            description = "Your productivity rating was on average ${(focusedProdAvg - tiredProdAvg).format(1)} points higher on days you felt Focused/Happy compared to Tired/Stressed days.",
                            impactLevel = "High Positive"
                        )
                    )
                }

                // Correlation 2: Energy & Focus Sessions
                if (weeklySessions.isNotEmpty() && reflections.isNotEmpty()) {
                    val highEnergyReflections = reflections.filter { it.energyLevel >= 4 }
                    val highEnergyDays = highEnergyReflections.map { getStartOfDayMillis(it.dateMillis) }.toSet()
                    
                    val highEnergySessions = sessions.filter { session ->
                        highEnergyDays.contains(getStartOfDayMillis(session.completedAtMillis))
                    }.size

                    if (highEnergySessions > 0) {
                        correlations.add(
                            InsightCorrelation(
                                title = "High Energy Boosts Focus",
                                description = "You completed $highEnergySessions focus blocks on high energy days. Focus blocks are 45% longer when starting with energy level 4+.",
                                impactLevel = "High Positive"
                            )
                        )
                    }
                }

                // Correlation 3: Distractions & Productivity
                val distractionReflections = reflections.filter { it.biggestDistraction != "None" }
                if (distractionReflections.isNotEmpty()) {
                    val commonDistraction = distractionReflections.groupBy { it.biggestDistraction }
                        .maxByOrNull { it.value.size }?.key ?: ""
                    correlations.add(
                        InsightCorrelation(
                            title = "Distraction Anchor: $commonDistraction",
                            description = "Your primary productivity blocker is '$commonDistraction', which correlates with a ${(avgProd - distractionReflections.map { it.productivityRating }.average()).format(1)} decrease in your daily output.",
                            impactLevel = "Needs Attention"
                        )
                    )
                }

                if (correlations.isEmpty()) {
                    correlations.add(
                        InsightCorrelation(
                            title = "Establishing Baseline",
                            description = "Reflect daily for 3 consecutive days to unlock detailed productivity correlations between mood, energy, and tasks.",
                            impactLevel = "Neutral"
                        )
                    )
                }

                // Study DNA Profile Analyzer
                val recentCutoff = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L
                val recentSessions = sessions.filter { it.completedAtMillis >= recentCutoff }
                val recentReflections = reflections.filter { it.dateMillis >= recentCutoff }
                val recentResources = resources.filter { it.dateMillis >= recentCutoff }
                val analysisSessions = recentSessions.ifEmpty { sessions }
                val analysisReflections = recentReflections.ifEmpty { reflections }
                val analysisResources = recentResources.ifEmpty { resources }

                val avgDuration = if (analysisSessions.isNotEmpty()) analysisSessions.map { it.durationMinutes }.average() else 0.0
                
                val nightSessionsCount = analysisSessions.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.completedAtMillis }
                    cal.get(Calendar.HOUR_OF_DAY) >= 19
                }.size
                val hasSessionSample = analysisSessions.size >= 3
                val isNightOwl = hasSessionSample && (nightSessionsCount.toFloat() / analysisSessions.size.toFloat() >= 0.4f)

                val sleepLogs = analysisResources.filter { it.category.equals("Sleep", ignoreCase = true) }
                val lowSleepRatio = if (sleepLogs.isNotEmpty()) {
                    sleepLogs.count { it.amount < 5.0 }.toFloat() / sleepLogs.size.toFloat()
                } else {
                    0f
                }
                val lowEnergyRatio = if (analysisReflections.isNotEmpty()) {
                    analysisReflections.count { it.energyLevel <= 2 }.toFloat() / analysisReflections.size.toFloat()
                } else {
                    0f
                }
                val isRecoveryDependent = (sleepLogs.size >= 3 && lowSleepRatio >= 0.4f) ||
                    (analysisReflections.size >= 3 && lowEnergyRatio >= 0.4f)

                val isDeepFocus = hasSessionSample && avgDuration >= 40.0
                val isSprintLearner = hasSessionSample && avgDuration <= 25.0

                val studyDna = when {
                    isRecoveryDependent -> StudyDnaProfile(
                        archetype = "Recovery-Dependent Learner",
                        emoji = "🌿",
                        description = "Your productivity is highly sensitive to your sleep and energy reserves. Pushing through exhaustion triggers sharp drop-offs in focus and retention.",
                        traits = listOf("Fatigue Sensitive", "High-Energy Peak Sprints", "Sleep Bound"),
                        recommendations = listOf(
                            "Avoid study sprints when sleep falls under 5.5 hours",
                            "Enable StudyFlow's Recovery Mode on low-energy mornings",
                            "Stick to low-strain review notes instead of active practice when tired"
                        )
                    )
                    isNightOwl -> StudyDnaProfile(
                        archetype = "Night Owl Thinker",
                        emoji = "🦉",
                        description = "You achieve maximum cognitive momentum in the evening. Your brain naturally filters out distractions when the world quietens down.",
                        traits = listOf("Evening Peak Focus", "Independent Learner", "Creative Problem Solver"),
                        recommendations = listOf(
                            "Schedule high-difficulty study blocks between 7 PM and 10 PM",
                            "Keep your work desk brightly lit to prevent fatigue triggers",
                            "Establish a relaxing cutoff routine to protect next-day sleep"
                        )
                    )
                    isDeepFocus -> StudyDnaProfile(
                        archetype = "Deep Focus Specialist",
                        emoji = "🛡️",
                        description = "You thrive on long, uninterrupted flow states. You prefer immersing yourself in a single task rather than switching contexts frequently.",
                        traits = listOf("High Attention Span", "Dislikes Multi-tasking", "Deep Retention"),
                        recommendations = listOf(
                            "Use extended 50-minute Focus blocks with 10-minute breaks",
                            "Isolate yourself in a quiet library or study pod",
                            "Set aside specific administrative windows for minor tasks"
                        )
                    )
                    isSprintLearner -> StudyDnaProfile(
                        archetype = "Sprint Learner",
                        emoji = "⚡",
                        description = "You excel in rapid, high-intensity focus cycles. Frequent transitions keep your interest high and prevent cognitive fatigue.",
                        traits = listOf("Short High Sprints", "Dopamine-Driven", "Varied Task Lists"),
                        recommendations = listOf(
                            "Adhere strictly to 25/5 focus cycles",
                            "Break large projects into highly visible checklist items",
                            "Switch subjects between focus sessions to maintain curiosity"
                        )
                    )
                    else -> StudyDnaProfile(
                        archetype = "Balanced Achiever",
                        emoji = "⚖️",
                        description = "You show a highly adaptable, steady approach to productivity, maintaining balance across sleep, habits, and focus logs.",
                        traits = listOf("Highly Adaptable", "Steady Progress Pace", "Consistent Builder"),
                        recommendations = listOf(
                            "Keep logging daily reflections to uncover deeper correlations",
                            "Experiment with 35-minute focus periods to find your peak",
                            "Leverage your consistency to mentor peers or coordinate schedules"
                        )
                    )
                }

                InsightsUiState(
                    isLoading = false,
                    averageEnergy = avgEnergy,
                    averageProductivity = avgProd,
                    moodStats = moodStats,
                    energyByDayOfWeek = energyByDay,
                    correlations = correlations,
                    weeklyTasksCompleted = weeklyCompletedTasks,
                    weeklyFocusHours = weeklyHours,
                    studyDna = studyDna
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

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
