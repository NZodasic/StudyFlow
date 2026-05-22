package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.WorkspaceDao
import com.studyflow.app.data.local.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository @Inject constructor(
    private val workspaceDao: WorkspaceDao
) {
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces()

    fun getWorkspaceById(id: Long): Flow<WorkspaceEntity?> = workspaceDao.getWorkspaceById(id)

    suspend fun insertWorkspace(workspace: WorkspaceEntity): Long = withContext(Dispatchers.IO) {
        workspaceDao.insertWorkspace(workspace)
    }

    suspend fun updateWorkspace(workspace: WorkspaceEntity): Int = withContext(Dispatchers.IO) {
        workspaceDao.updateWorkspace(workspace)
    }

    suspend fun deleteWorkspace(workspace: WorkspaceEntity): Int = withContext(Dispatchers.IO) {
        workspaceDao.deleteWorkspace(workspace)
    }
}
