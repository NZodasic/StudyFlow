package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.*
import com.studyflow.app.data.local.entity.AIRecommendationEntity
import com.studyflow.app.data.local.entity.ResourceEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRecommendationRepository @Inject constructor(
    private val aiRecommendationDao: AIRecommendationDao,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val pomodoroDao: PomodoroDao,
    private val resourceDao: ResourceDao,
    private val reflectionDao: ReflectionDao
) {
    fun getActiveRecommendations(): Flow<List<AIRecommendationEntity>> {
        val currentTime = System.currentTimeMillis()
        return aiRecommendationDao.getActiveRecommendations(currentTime)
    }

    suspend fun setReadStatus(id: Long, isRead: Boolean) = withContext(Dispatchers.IO) {
        aiRecommendationDao.setReadStatus(id, isRead)
    }

    suspend fun deleteRecommendation(id: Long) = withContext(Dispatchers.IO) {
        aiRecommendationDao.deleteRecommendationById(id)
    }

    suspend fun clearAllRecommendations() = withContext(Dispatchers.IO) {
        aiRecommendationDao.deleteAllRecommendations()
    }

    /**
     * Runs the rule-based Study Coach Engine to analyze the database context
     * and generate fresh recommendations for the user.
     */
    suspend fun runStudyCoachEngine(workspaceId: Long?): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Fetch historical context data
        // Last 7 days threshold
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.timeInMillis

        // Get resources, tasks, focus sessions, and reflections
        val resources = resourceDao.getAllResourcesSuspended(workspaceId)
        val recentResources = resources.filter { it.dateMillis >= sevenDaysAgo }
        val tasks = taskDao.getAllTasksSuspended(workspaceId)
        val activeTasks = tasks.filter { !it.isCompleted }
        val focusSessions = pomodoroDao.getAllSessionsSuspended(workspaceId)
        val recentSessions = focusSessions.filter { it.completedAtMillis >= sevenDaysAgo }
        val reflections = reflectionDao.getAllReflectionsSuspended()
        val recentReflections = reflections.filter { it.dateMillis >= sevenDaysAgo }

        // Start day boundaries
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        val endOfToday = startOfToday + 86399999L

        val todayResources = resources.filter { it.dateMillis in startOfToday..endOfToday }
        val todaySessions = focusSessions.filter { it.completedAtMillis in startOfToday..endOfToday }

        val newRecs = mutableListOf<AIRecommendationEntity>()

        // ─── RULE 1: SLEEP DEPRIVATION & FOCUS RECOVERY ───
        val sleepLogs = todayResources.filter { it.category.equals("Sleep", ignoreCase = true) }
        val sleepAmount = sleepLogs.map { it.amount }.firstOrNull() ?: 7.5 // default if none logged
        if (sleepLogs.isNotEmpty() && sleepAmount < 6.0) {
            newRecs.add(
                AIRecommendationEntity(
                    type = "health",
                    priority = "high",
                    message = "Sleep deprivation detected (${sleepAmount}h). Cognitive performance might be reduced. Take shorter study sprints (25/5 Pomodoro) and prioritize review tasks today.",
                    action = "START_POMODORO",
                    confidence = 0.90,
                    expiresAtMillis = now + 12 * 60 * 60 * 1000L // 12 hours expiry
                )
            )
        } else if (sleepLogs.isNotEmpty() && sleepAmount >= 7.0 && sleepAmount <= 9.0) {
            newRecs.add(
                AIRecommendationEntity(
                    type = "health",
                    priority = "low",
                    message = "Excellent sleep rest logged (${sleepAmount}h)! Today is ideal for deep cognitive challenge sessions. Schedule your hardest topic now.",
                    confidence = 0.85,
                    expiresAtMillis = now + 16 * 60 * 60 * 1000L
                )
            )
        }

        // ─── RULE 2: CAFFEINE OVERDOSING / MODERATION ───
        val caffeineLogs = todayResources.filter { it.category.equals("Caffeine", ignoreCase = true) }
        val totalCaffeine = caffeineLogs.sumOf { it.amount }
        if (totalCaffeine > 350.0) {
            newRecs.add(
                AIRecommendationEntity(
                    type = "health",
                    priority = "high",
                    message = "Caffeine intake is high (${totalCaffeine.toInt()}mg). To prevent nervous jitters and crash risk, avoid additional stimulants and switch to hydration.",
                    action = "SHOW_BREATHING_EXERCISE",
                    confidence = 0.95,
                    expiresAtMillis = now + 8 * 60 * 60 * 1000L
                )
            )
        }

        // ─── RULE 3: LOW HYDRATION ───
        val waterLogs = todayResources.filter { it.category.equals("Hydration", ignoreCase = true) }
        val totalWater = waterLogs.sumOf { it.amount }
        if (waterLogs.isNotEmpty() && totalWater < 4.0) {
            newRecs.add(
                AIRecommendationEntity(
                    type = "health",
                    priority = "medium",
                    message = "Hydration level is low ($totalWater cups). Proper hydration improves concentration by up to 15%. Drink a glass of water now.",
                    confidence = 0.80,
                    expiresAtMillis = now + 6 * 60 * 60 * 1000L
                )
            )
        }

        // ─── RULE 4: BURNOUT RISK DETECTION ───
        val highStressReflections = recentReflections.filter { it.stressLevel >= 4 }
        val totalRecentSessions = recentSessions.size
        val highStressRatio = if (recentReflections.isNotEmpty()) highStressReflections.size.toFloat() / recentReflections.size else 0f
        
        if (highStressRatio >= 0.4f || (totalRecentSessions >= 15 && highStressReflections.isNotEmpty())) {
            newRecs.add(
                AIRecommendationEntity(
                    type = "health",
                    priority = "high",
                    message = "Burnout Risk Alert! High stress logs coupled with heavy focus history detected. Reduce study load, take a full recovery break, and log reflections.",
                    action = "SHOW_BREATHING_EXERCISE",
                    confidence = 0.92,
                    expiresAtMillis = now + 24 * 60 * 60 * 1000L
                )
            )
        }

        // ─── RULE 5: OPTIMAL BREAK SUGGESTIONS FROM DISTRACTIONS ───
        if (recentSessions.isNotEmpty()) {
            val totalExits = recentSessions.sumOf { it.appExitsCount }
            val avgExits = totalExits.toDouble() / recentSessions.size
            if (avgExits >= 2.0) {
                newRecs.add(
                    AIRecommendationEntity(
                        type = "break",
                        priority = "medium",
                        message = "Frequent app-exit distractions detected (avg ${String.format("%.1f", avgExits)} per session). Try leaving your phone in another room or shorten focus duration to 15-20 minutes.",
                        action = "START_POMODORO",
                        confidence = 0.88,
                        expiresAtMillis = now + 18 * 60 * 60 * 1000L
                    )
                )
            }
        }

        // ─── RULE 6: ENERGY-BASED SCHEDULING ───
        val energyLogs = todayResources.filter { it.category.equals("Energy", ignoreCase = true) }
        val energyLevel = energyLogs.map { it.amount }.firstOrNull() ?: 3.0
        if (energyLogs.isNotEmpty() && energyLevel <= 2.0) {
            newRecs.add(
                AIRecommendationEntity(
                    type = "schedule",
                    priority = "high",
                    message = "Energy is low (${energyLevel.toInt()}/5). Consider a 20-min power nap or light exercise before tackling difficult topics.",
                    action = "SHOW_BREATHING_EXERCISE",
                    confidence = 0.85,
                    expiresAtMillis = now + 4 * 60 * 60 * 1000L
                )
            )
        }

        // ─── RULE 7: TASK URGENCY AND GOALS ───
        val highPriorityPending = activeTasks.filter { it.priority >= 2 }
        if (highPriorityPending.isNotEmpty()) {
            val urgentTask = highPriorityPending.first()
            newRecs.add(
                AIRecommendationEntity(
                    type = "goal",
                    priority = "medium",
                    message = "Priority focus target: '${urgentTask.title}'. Dedicate your next Pomodoro session entirely to resolving this task.",
                    action = "START_POMODORO",
                    confidence = 0.82,
                    expiresAtMillis = now + 12 * 60 * 60 * 1000L
                )
            )
        }

        // ─── RULE 8: OPTIMAL STUDY HOURS CORRELATION ───
        if (recentReflections.isNotEmpty()) {
            val highProdReflections = recentReflections.filter { it.productivityRating >= 4 }
            if (highProdReflections.isNotEmpty()) {
                val cal = Calendar.getInstance()
                // Map reflections to times of day
                val timeGroups = highProdReflections.groupBy { reflection ->
                    cal.timeInMillis = reflection.dateMillis
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    when {
                        hour in 5..11 -> "morning"
                        hour in 12..16 -> "afternoon"
                        hour in 17..21 -> "evening"
                        else -> "night"
                    }
                }
                val optimalTime = timeGroups.maxByOrNull { it.value.size }?.key
                if (optimalTime != null) {
                    newRecs.add(
                        AIRecommendationEntity(
                            type = "schedule",
                            priority = "low",
                            message = "Historical reflections indicate peak productivity in the $optimalTime. Plan your highest priority study blocks during this window.",
                            confidence = 0.78,
                            expiresAtMillis = now + 48 * 60 * 60 * 1000L
                        )
                    )
                }
            }
        }

        // ─── Persist recommendations ───
        // First delete expired recommendations, then insert new ones
        aiRecommendationDao.deleteExpiredRecommendations(now)
        
        if (newRecs.isNotEmpty()) {
            aiRecommendationDao.insertRecommendations(newRecs)
        }
    }
}
