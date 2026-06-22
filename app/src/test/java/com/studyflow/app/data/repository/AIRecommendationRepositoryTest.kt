package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.*
import com.studyflow.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AIRecommendationRepositoryTest {

    private lateinit var aiRecommendationDao: FakeAIRecommendationDao
    private lateinit var taskDao: FakeTaskDao
    private lateinit var habitDao: FakeHabitDao
    private lateinit var pomodoroDao: FakePomodoroDao
    private lateinit var resourceDao: FakeResourceDao
    private lateinit var reflectionDao: FakeReflectionDao
    private lateinit var repository: AIRecommendationRepository

    @Before
    fun setUp() {
        aiRecommendationDao = FakeAIRecommendationDao()
        taskDao = FakeTaskDao()
        habitDao = FakeHabitDao()
        pomodoroDao = FakePomodoroDao()
        resourceDao = FakeResourceDao()
        reflectionDao = FakeReflectionDao()
        repository = AIRecommendationRepository(
            aiRecommendationDao,
            taskDao,
            habitDao,
            pomodoroDao,
            resourceDao,
            reflectionDao
        )
    }

    @Test
    fun testLowEnergyRecommendation() = runTest {
        // Given: Log energy of 2.0 (low)
        val now = System.currentTimeMillis()
        val lowEnergyResource = ResourceEntity(
            amount = 2.0,
            category = "Energy",
            dateMillis = now
        )
        resourceDao.insertResource(lowEnergyResource)

        // When: Run engine
        repository.runStudyCoachEngine(null)

        // Then: Should generate low energy recommendation
        val recs = repository.getActiveRecommendations().first()
        assertTrue(recs.any { it.type == "schedule" && it.priority == "high" && it.message.contains("Energy is low") })
    }

    @Test
    fun testSleepDeprivationRecommendation() = runTest {
        // Given: Log sleep of 5.0 hours
        val now = System.currentTimeMillis()
        val lowSleepResource = ResourceEntity(
            amount = 5.0,
            category = "Sleep",
            dateMillis = now
        )
        resourceDao.insertResource(lowSleepResource)

        // When: Run engine
        repository.runStudyCoachEngine(null)

        // Then: Should generate sleep deprivation recommendation
        val recs = repository.getActiveRecommendations().first()
        assertTrue(recs.any { it.type == "health" && it.priority == "high" && it.message.contains("Sleep deprivation detected") })
    }

    @Test
    fun testExcessiveCaffeineRecommendation() = runTest {
        // Given: Log caffeine intake totaling 400mg
        val now = System.currentTimeMillis()
        resourceDao.insertResource(ResourceEntity(amount = 200.0, category = "Caffeine", dateMillis = now))
        resourceDao.insertResource(ResourceEntity(amount = 200.0, category = "Caffeine", dateMillis = now))

        // When: Run engine
        repository.runStudyCoachEngine(null)

        // Then: Should generate caffeine warning
        val recs = repository.getActiveRecommendations().first()
        assertTrue(recs.any { it.type == "health" && it.priority == "high" && it.message.contains("Caffeine intake is high") })
    }

    @Test
    fun testTaskUrgencyRecommendation() = runTest {
        // Given: Pending high priority task
        val task = TaskEntity(
            title = "Final Exam Prep",
            priority = 2, // High priority
            isCompleted = false
        )
        taskDao.insertTask(task)

        // When: Run engine
        repository.runStudyCoachEngine(null)

        // Then: Should suggest working on this task
        val recs = repository.getActiveRecommendations().first()
        assertTrue(recs.any { it.type == "goal" && it.message.contains("Final Exam Prep") })
    }

    // ─── FAKE DAO IMPLEMENTATIONS ───

    class FakeAIRecommendationDao : AIRecommendationDao {
        private val recommendations = mutableMapOf<Long, AIRecommendationEntity>()

        override fun getAllRecommendations(): Flow<List<AIRecommendationEntity>> =
            flowOf(recommendations.values.toList())

        override fun getActiveRecommendations(currentTime: Long): Flow<List<AIRecommendationEntity>> =
            flowOf(recommendations.values.filter { it.expiresAtMillis > currentTime })

        override fun insertRecommendations(recs: List<AIRecommendationEntity>) {
            var idCounter = recommendations.size.toLong() + 1
            recs.forEach {
                val toInsert = if (it.id == 0L) it.copy(id = idCounter++) else it
                recommendations[toInsert.id] = toInsert
            }
        }

        override fun insertRecommendation(recommendation: AIRecommendationEntity): Long {
            val id = if (recommendation.id == 0L) (recommendations.size + 1).toLong() else recommendation.id
            recommendations[id] = recommendation.copy(id = id)
            return id
        }

        override fun setReadStatus(id: Long, isRead: Boolean): Int {
            val existing = recommendations[id] ?: return 0
            recommendations[id] = existing.copy(isRead = isRead)
            return 1
        }

        override fun deleteRecommendationById(id: Long): Int {
            recommendations.remove(id)
            return 1
        }

        override fun deleteAllRecommendations(): Int {
            val size = recommendations.size
            recommendations.clear()
            return size
        }

        override fun deleteExpiredRecommendations(currentTime: Long): Int {
            val sizeBefore = recommendations.size
            recommendations.values.removeIf { it.expiresAtMillis <= currentTime }
            return sizeBefore - recommendations.size
        }
    }

    class FakeTaskDao : TaskDao {
        private val tasks = mutableMapOf<Long, TaskEntity>()

        override fun getAllTasks(workspaceId: Long?): Flow<List<TaskEntity>> =
            flowOf(tasks.values.toList())

        override fun getTaskById(id: Long): Flow<TaskEntity?> =
            flowOf(tasks[id])

        override fun getTasksByFilter(completed: Boolean, workspaceId: Long?): Flow<List<TaskEntity>> =
            flowOf(tasks.values.filter { it.isCompleted == completed })

        override fun searchTasks(query: String, workspaceId: Long?): Flow<List<TaskEntity>> =
            flowOf(tasks.values.filter { it.title.contains(query) || it.description.contains(query) })

        override fun getTasksDueToday(todayEnd: Long, workspaceId: Long?): Flow<List<TaskEntity>> =
            flowOf(tasks.values.filter { !it.isCompleted && (it.dueDateMillis ?: 0) <= todayEnd })

        override fun getCompletedTaskCount(workspaceId: Long?): Flow<Int> =
            flowOf(tasks.values.count { it.isCompleted })

        override fun insertTask(task: TaskEntity): Long {
            val id = if (task.id == 0L) (tasks.size + 1).toLong() else task.id
            tasks[id] = task.copy(id = id)
            return id
        }

        override fun updateTask(task: TaskEntity): Int {
            tasks[task.id] = task
            return 1
        }

        override fun deleteTask(task: TaskEntity): Int {
            tasks.remove(task.id)
            return 1
        }

        override fun setTaskCompleted(id: Long, completed: Boolean, completedAtMillis: Long?): Int {
            val existing = tasks[id] ?: return 0
            tasks[id] = existing.copy(isCompleted = completed, completedAtMillis = completedAtMillis)
            return 1
        }

        override fun getAllTasksSuspended(workspaceId: Long?): List<TaskEntity> =
            tasks.values.toList()

        override fun deleteAllTasks(): Int {
            val size = tasks.size
            tasks.clear()
            return size
        }
    }

    class FakeHabitDao : HabitDao {
        override fun getAllHabits(workspaceId: Long?): Flow<List<HabitEntity>> = flowOf(emptyList())
        override fun getHabitById(id: Long): Flow<HabitEntity?> = flowOf(null)
        override fun getLogsForHabit(habitId: Long, weekStart: Long): Flow<List<HabitLogEntity>> = flowOf(emptyList())
        override fun isLoggedToday(habitId: Long, todayStart: Long, todayEnd: Long): Flow<Boolean> = flowOf(false)
        override fun isLoggedTodaySuspended(habitId: Long, todayStart: Long, todayEnd: Long): Boolean = false
        override fun insertHabit(habit: HabitEntity): Long = 0
        override fun updateHabit(habit: HabitEntity): Int = 0
        override fun deleteHabit(habit: HabitEntity): Int = 0
        override fun insertLog(log: HabitLogEntity): Long = 0
        override fun deleteLogForToday(habitId: Long, todayStart: Long, todayEnd: Long): Int = 0
        override fun getAllLogsForHabit(habitId: Long): List<Long> = emptyList()
        override fun getLogsForPeriod(start: Long, end: Long, workspaceId: Long?): Flow<List<HabitLogEntity>> = flowOf(emptyList())
    }

    class FakePomodoroDao : PomodoroDao {
        private val sessions = mutableMapOf<Long, PomodoroSessionEntity>()

        override fun getAllSessions(workspaceId: Long?): Flow<List<PomodoroSessionEntity>> =
            flowOf(sessions.values.toList())

        override fun getSessionsToday(dayStart: Long, workspaceId: Long?): Flow<Int> =
            flowOf(sessions.values.count { it.completedAtMillis >= dayStart })

        override fun insertSession(session: PomodoroSessionEntity): Long {
            val id = if (session.id == 0L) (sessions.size + 1).toLong() else session.id
            sessions[id] = session.copy(id = id)
            return id
        }

        override fun deleteSession(session: PomodoroSessionEntity): Int {
            sessions.remove(session.id)
            return 1
        }

        override fun getAllSessionsSuspended(workspaceId: Long?): List<PomodoroSessionEntity> =
            sessions.values.toList()
    }

    class FakeResourceDao : ResourceDao {
        private val resources = mutableMapOf<Long, ResourceEntity>()

        override fun getAllResources(workspaceId: Long?): Flow<List<ResourceEntity>> =
            flowOf(resources.values.toList())

        override fun getResourcesForMonth(start: Long, end: Long, workspaceId: Long?): Flow<List<ResourceEntity>> =
            flowOf(resources.values.filter { it.dateMillis in start..end })

        override fun getTotalForMonth(start: Long, end: Long, workspaceId: Long?): Flow<Double> =
            flowOf(resources.values.filter { it.dateMillis in start..end }.sumOf { it.amount })

        override fun getTotalByCategory(start: Long, end: Long, workspaceId: Long?): Flow<List<ResourceCategoryTotal>> =
            flowOf(emptyList())

        override fun getWeeklyTotalSpending(weekStart: Long, workspaceId: Long?): Flow<Double> =
            flowOf(resources.values.filter { it.dateMillis >= weekStart && it.category == "Spending" }.sumOf { it.amount })

        override fun getWeeklyTotal(weekStart: Long, workspaceId: Long?): Flow<Double> =
            flowOf(resources.values.filter { it.dateMillis >= weekStart }.sumOf { it.amount })

        override fun insertResource(resource: ResourceEntity): Long {
            val id = if (resource.id == 0L) (resources.size + 1).toLong() else resource.id
            resources[id] = resource.copy(id = id)
            return id
        }

        override fun updateResource(resource: ResourceEntity): Int {
            resources[resource.id] = resource
            return 1
        }

        override fun deleteResource(resource: ResourceEntity): Int {
            resources.remove(resource.id)
            return 1
        }

        override fun deleteAllResources(): Int {
            val size = resources.size
            resources.clear()
            return size
        }

        override fun getAllResourcesSuspended(workspaceId: Long?): List<ResourceEntity> =
            resources.values.toList()
    }

    class FakeReflectionDao : ReflectionDao {
        private val reflections = mutableMapOf<Long, ReflectionEntity>()

        override fun getAllReflections(): Flow<List<ReflectionEntity>> =
            flowOf(reflections.values.toList())

        override fun getReflectionByDate(dateStart: Long, dateEnd: Long): Flow<ReflectionEntity?> =
            flowOf(reflections.values.firstOrNull { it.dateMillis in dateStart..dateEnd })

        override fun getReflectionByDateSuspended(dateStart: Long, dateEnd: Long): ReflectionEntity? =
            reflections.values.firstOrNull { it.dateMillis in dateStart..dateEnd }

        override fun getLatestReflection(): Flow<ReflectionEntity?> =
            flowOf(reflections.values.maxByOrNull { it.dateMillis })

        override fun insertReflection(reflection: ReflectionEntity): Long {
            val id = if (reflection.id == 0L) (reflections.size + 1).toLong() else reflection.id
            reflections[id] = reflection.copy(id = id)
            return id
        }

        override fun updateReflection(reflection: ReflectionEntity): Int {
            reflections[reflection.id] = reflection
            return 1
        }

        override fun deleteReflection(reflection: ReflectionEntity): Int {
            reflections.remove(reflection.id)
            return 1
        }

        override fun getAllReflectionsSuspended(): List<ReflectionEntity> =
            reflections.values.toList()
    }
}
