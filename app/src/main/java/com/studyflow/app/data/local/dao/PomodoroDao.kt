package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY completedAtMillis DESC")
    fun getAllSessions(workspaceId: Long?): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE completedAtMillis >= :dayStart AND (:workspaceId IS NULL OR workspaceId = :workspaceId)")
    fun getSessionsToday(dayStart: Long, workspaceId: Long?): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: PomodoroSessionEntity): Long

    @Delete
    fun deleteSession(session: PomodoroSessionEntity): Int

    @Query("SELECT * FROM pomodoro_sessions WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY completedAtMillis DESC")
    fun getAllSessionsSuspended(workspaceId: Long?): List<PomodoroSessionEntity>
}
