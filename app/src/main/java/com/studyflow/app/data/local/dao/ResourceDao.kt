package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

data class ResourceCategoryTotal(
    val category: String,
    val total: Double
)

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY dateMillis DESC")
    fun getAllResources(workspaceId: Long?): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE dateMillis >= :start AND dateMillis <= :end AND (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY dateMillis DESC")
    fun getResourcesForMonth(start: Long, end: Long, workspaceId: Long?): Flow<List<ResourceEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM resources WHERE dateMillis >= :start AND dateMillis <= :end AND (:workspaceId IS NULL OR workspaceId = :workspaceId)")
    fun getTotalForMonth(start: Long, end: Long, workspaceId: Long?): Flow<Double>

    @Query("SELECT category, SUM(amount) as total FROM resources WHERE dateMillis >= :start AND dateMillis <= :end AND (:workspaceId IS NULL OR workspaceId = :workspaceId) GROUP BY category")
    fun getTotalByCategory(start: Long, end: Long, workspaceId: Long?): Flow<List<ResourceCategoryTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM resources WHERE dateMillis >= :weekStart AND (:workspaceId IS NULL OR workspaceId = :workspaceId) AND category = 'Spending'")
    fun getWeeklyTotalSpending(weekStart: Long, workspaceId: Long?): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM resources WHERE dateMillis >= :weekStart AND (:workspaceId IS NULL OR workspaceId = :workspaceId)")
    fun getWeeklyTotal(weekStart: Long, workspaceId: Long?): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResource(resource: ResourceEntity): Long

    @Update
    fun updateResource(resource: ResourceEntity): Int

    @Delete
    fun deleteResource(resource: ResourceEntity): Int

    @Query("DELETE FROM resources")
    fun deleteAllResources(): Int

    @Query("SELECT * FROM resources WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY dateMillis DESC")
    fun getAllResourcesSuspended(workspaceId: Long?): List<ResourceEntity>
}
