package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY name ASC")
    fun getAllHabits(workspaceId: Long?): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateMillis >= :weekStart")
    fun getLogsForHabit(habitId: Long, weekStart: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_logs WHERE habitId = :habitId AND dateMillis >= :todayStart AND dateMillis <= :todayEnd)")
    fun isLoggedToday(habitId: Long, todayStart: Long, todayEnd: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_logs WHERE habitId = :habitId AND dateMillis >= :todayStart AND dateMillis <= :todayEnd)")
    fun isLoggedTodaySuspended(habitId: Long, todayStart: Long, todayEnd: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabit(habit: HabitEntity): Long

    @Update
    fun updateHabit(habit: HabitEntity): Int

    @Delete
    fun deleteHabit(habit: HabitEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateMillis >= :todayStart AND dateMillis <= :todayEnd")
    fun deleteLogForToday(habitId: Long, todayStart: Long, todayEnd: Long): Int

    @Query("SELECT dateMillis FROM habit_logs WHERE habitId = :habitId ORDER BY dateMillis DESC")
    fun getAllLogsForHabit(habitId: Long): List<Long>

    @Query("SELECT hl.* FROM habit_logs hl INNER JOIN habits h ON hl.habitId = h.id WHERE hl.dateMillis >= :start AND hl.dateMillis <= :end AND (:workspaceId IS NULL OR h.workspaceId = :workspaceId)")
    fun getLogsForPeriod(start: Long, end: Long, workspaceId: Long?): Flow<List<HabitLogEntity>>
}

