package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY priority DESC, dueDateMillis ASC")
    fun getAllTasks(workspaceId: Long?): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE isCompleted = :completed AND (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY priority DESC, dueDateMillis ASC")
    fun getTasksByFilter(completed: Boolean, workspaceId: Long?): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY priority DESC, dueDateMillis ASC")
    fun searchTasks(query: String, workspaceId: Long?): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDateMillis <= :todayEnd AND (:workspaceId IS NULL OR workspaceId = :workspaceId)")
    fun getTasksDueToday(todayEnd: Long, workspaceId: Long?): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND (:workspaceId IS NULL OR workspaceId = :workspaceId)")
    fun getCompletedTaskCount(workspaceId: Long?): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: TaskEntity): Long

    @Update
    fun updateTask(task: TaskEntity): Int

    @Delete
    fun deleteTask(task: TaskEntity): Int

    @Query("UPDATE tasks SET isCompleted = :completed, completedAtMillis = :completedAtMillis WHERE id = :id")
    fun setTaskCompleted(id: Long, completed: Boolean, completedAtMillis: Long?): Int

    @Query("SELECT * FROM tasks WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId)")
    fun getAllTasksSuspended(workspaceId: Long?): List<TaskEntity>

    @Query("DELETE FROM tasks")
    fun deleteAllTasks(): Int
}
