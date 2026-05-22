package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.TaskDao
import com.studyflow.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(workspaceId: Long?): Flow<List<TaskEntity>> = taskDao.getAllTasks(workspaceId)
    
    fun getTaskById(id: Long): Flow<TaskEntity?> = taskDao.getTaskById(id)
    
    fun getTasksByFilter(completed: Boolean, workspaceId: Long?): Flow<List<TaskEntity>> = taskDao.getTasksByFilter(completed, workspaceId)
    
    fun searchTasks(query: String, workspaceId: Long?): Flow<List<TaskEntity>> = taskDao.searchTasks(query, workspaceId)
    
    fun getTasksDueToday(todayEnd: Long, workspaceId: Long?): Flow<List<TaskEntity>> = taskDao.getTasksDueToday(todayEnd, workspaceId)
    
    fun getCompletedTaskCount(workspaceId: Long?): Flow<Int> = taskDao.getCompletedTaskCount(workspaceId)

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }
    
    suspend fun updateTask(task: TaskEntity): Int = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }
    
    suspend fun deleteTask(task: TaskEntity): Int = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }
    
    suspend fun toggleTaskCompletion(taskId: Long, completed: Boolean): Int = withContext(Dispatchers.IO) {
        val completedAt = if (completed) System.currentTimeMillis() else null
        taskDao.setTaskCompleted(taskId, completed, completedAt)
    }

    suspend fun getAllTasksSuspended(workspaceId: Long?): List<TaskEntity> = withContext(Dispatchers.IO) {
        taskDao.getAllTasksSuspended(workspaceId)
    }

    suspend fun deleteAllTasks(): Int = withContext(Dispatchers.IO) {
        taskDao.deleteAllTasks()
    }
}

