package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.HabitDao
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class HabitRepositoryTest {

    private lateinit var fakeDao: FakeHabitDao
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        fakeDao = FakeHabitDao()
        repository = HabitRepository(fakeDao)
    }

    @Test
    fun testToggleHabitOn_CreatesLogAndCalculatesStreak() = runTest {
        // Given
        val habit = HabitEntity(name = "Read Books", iconEmoji = "📚")
        val habitId = repository.insertHabit(habit)
        val today = System.currentTimeMillis()

        // When (toggle on)
        repository.toggleHabit(habitId, today)

        // Then
        assertTrue(fakeDao.isLoggedTodaySuspended(habitId, getStartOfDayMillis(today), getStartOfDayMillis(today) + 86399999L))
        val updatedHabit = repository.getHabitById(habitId).first()
        assertEquals(1, updatedHabit?.currentStreak)
        assertEquals(1, updatedHabit?.bestStreak)
    }

    @Test
    fun testToggleHabitOff_DeletesLogAndRecalculatesStreak() = runTest {
        // Given
        val habit = HabitEntity(name = "Read Books", iconEmoji = "📚")
        val habitId = repository.insertHabit(habit)
        val today = System.currentTimeMillis()

        // Toggle on first
        repository.toggleHabit(habitId, today)
        assertEquals(1, repository.getHabitById(habitId).first()?.currentStreak)

        // When (toggle off)
        repository.toggleHabit(habitId, today)

        // Then
        assertFalse(fakeDao.isLoggedTodaySuspended(habitId, getStartOfDayMillis(today), getStartOfDayMillis(today) + 86399999L))
        val updatedHabit = repository.getHabitById(habitId).first()
        assertEquals(0, updatedHabit?.currentStreak)
    }

    @Test
    fun testConsecutiveDaysStreakCalculation() = runTest {
        // Given
        val habit = HabitEntity(name = "Write Code", iconEmoji = "💻")
        val habitId = repository.insertHabit(habit)

        val calendar = Calendar.getInstance()
        
        // Log for 2 days ago
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val twoDaysAgo = calendar.timeInMillis
        repository.toggleHabit(habitId, twoDaysAgo)

        // Log for yesterday
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis
        repository.toggleHabit(habitId, yesterday)

        // Log for today
        val today = System.currentTimeMillis()
        repository.toggleHabit(habitId, today)

        // Then
        val updatedHabit = repository.getHabitById(habitId).first()
        assertEquals(3, updatedHabit?.currentStreak)
        assertEquals(3, updatedHabit?.bestStreak)
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

        // Fake DAO Implementation
        class FakeHabitDao : HabitDao {
            val habits = mutableMapOf<Long, HabitEntity>()
            val logs = mutableListOf<HabitLogEntity>()

            override fun getAllHabits(workspaceId: Long?): Flow<List<HabitEntity>> = flowOf(habits.values.toList())

            override fun getHabitById(id: Long): Flow<HabitEntity?> = flowOf(habits[id])

            override fun getLogsForHabit(habitId: Long, weekStart: Long): Flow<List<HabitLogEntity>> {
                return flowOf(logs.filter { it.habitId == habitId && it.dateMillis >= weekStart })
            }

            override fun isLoggedToday(habitId: Long, todayStart: Long, todayEnd: Long): Flow<Boolean> {
                return flowOf(isLoggedTodaySuspended(habitId, todayStart, todayEnd))
            }

            override fun isLoggedTodaySuspended(habitId: Long, todayStart: Long, todayEnd: Long): Boolean {
                return logs.any { it.habitId == habitId && it.dateMillis >= todayStart && it.dateMillis <= todayEnd }
            }

            override fun insertHabit(habit: HabitEntity): Long {
                val id = if (habit.id == 0L) (habits.size + 1).toLong() else habit.id
                val newHabit = habit.copy(id = id)
                habits[id] = newHabit
                return id
            }

            override fun updateHabit(habit: HabitEntity): Int {
                habits[habit.id] = habit
                return 1
            }

            override fun deleteHabit(habit: HabitEntity): Int {
                habits.remove(habit.id)
                return 1
            }

            override fun insertLog(log: HabitLogEntity): Long {
                val id = (logs.size + 1).toLong()
                logs.add(log.copy(id = id))
                return id
            }

            override fun deleteLogForToday(habitId: Long, todayStart: Long, todayEnd: Long): Int {
                val beforeSize = logs.size
                logs.removeAll { it.habitId == habitId && it.dateMillis >= todayStart && it.dateMillis <= todayEnd }
                return beforeSize - logs.size
            }

            override fun getAllLogsForHabit(habitId: Long): List<Long> {
                return logs.filter { it.habitId == habitId }.map { it.dateMillis }
            }

            override fun getLogsForPeriod(start: Long, end: Long, workspaceId: Long?): Flow<List<HabitLogEntity>> {
                return flowOf(logs.filter { it.dateMillis in start..end })
            }
        }
}
