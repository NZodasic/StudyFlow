package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.HabitDao
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao
) {
    fun getAllHabits(workspaceId: Long?): Flow<List<HabitEntity>> = habitDao.getAllHabits(workspaceId)

    fun getHabitById(id: Long): Flow<HabitEntity?> = habitDao.getHabitById(id)

    fun getLogsForHabit(habitId: Long, weekStart: Long): Flow<List<HabitLogEntity>> =
        habitDao.getLogsForHabit(habitId, weekStart)

    fun isLoggedToday(habitId: Long, todayStart: Long, todayEnd: Long): Flow<Boolean> =
        habitDao.isLoggedToday(habitId, todayStart, todayEnd)

    suspend fun insertHabit(habit: HabitEntity): Long = withContext(Dispatchers.IO) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity): Int = withContext(Dispatchers.IO) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity): Int = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleHabit(habitId: Long, dateMillis: Long) = withContext(Dispatchers.IO) {
        val todayStart = getStartOfDayMillis(dateMillis)
        val todayEnd = todayStart + 86399999L
        
        val isDone = habitDao.isLoggedTodaySuspended(habitId, todayStart, todayEnd)
        if (isDone) {
            habitDao.deleteLogForToday(habitId, todayStart, todayEnd)
        } else {
            habitDao.insertLog(HabitLogEntity(habitId = habitId, dateMillis = todayStart))
        }

        val habit = habitDao.getHabitById(habitId).firstOrNull() ?: return@withContext
        val allLogs = habitDao.getAllLogsForHabit(habitId)
        val newStreak = calculateStreak(allLogs, todayStart)
        
        val updatedHabit = habit.copy(
            currentStreak = newStreak,
            bestStreak = maxOf(habit.bestStreak, newStreak)
        )
        habitDao.updateHabit(updatedHabit)
    }

    private fun calculateStreak(logs: List<Long>, referenceDateMillis: Long): Int {
        if (logs.isEmpty()) return 0
        val sorted = logs.distinct().sortedDescending()

        val todayStart = getStartOfDayMillis(referenceDateMillis)
        val yesterdayStart = todayStart - 86400000L

        val firstLog = sorted.firstOrNull() ?: return 0
        if (firstLog != todayStart && firstLog != yesterdayStart) {
            return 0
        }

        var streak = 1
        var currentDay = firstLog
        for (i in 1 until sorted.size) {
            val prevDay = sorted[i]
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

    fun getLogsForPeriod(start: Long, end: Long, workspaceId: Long?): Flow<List<HabitLogEntity>> =
        habitDao.getLogsForPeriod(start, end, workspaceId)
}

