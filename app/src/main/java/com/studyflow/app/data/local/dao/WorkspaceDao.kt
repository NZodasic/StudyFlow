package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY createdAtMillis ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    fun getWorkspaceById(id: Long): Flow<WorkspaceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkspace(workspace: WorkspaceEntity): Long

    @Update
    fun updateWorkspace(workspace: WorkspaceEntity): Int

    @Delete
    fun deleteWorkspace(workspace: WorkspaceEntity): Int
}
