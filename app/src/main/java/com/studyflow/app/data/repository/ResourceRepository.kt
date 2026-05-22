package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.ResourceDao
import com.studyflow.app.data.local.dao.ResourceCategoryTotal
import com.studyflow.app.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepository @Inject constructor(
    private val resourceDao: ResourceDao
) {
    fun getAllResources(workspaceId: Long?): Flow<List<ResourceEntity>> = resourceDao.getAllResources(workspaceId)

    fun getResourcesForMonth(start: Long, end: Long, workspaceId: Long?): Flow<List<ResourceEntity>> =
        resourceDao.getResourcesForMonth(start, end, workspaceId)

    fun getTotalForMonth(start: Long, end: Long, workspaceId: Long?): Flow<Double> =
        resourceDao.getTotalForMonth(start, end, workspaceId)

    fun getTotalByCategory(start: Long, end: Long, workspaceId: Long?): Flow<List<ResourceCategoryTotal>> =
        resourceDao.getTotalByCategory(start, end, workspaceId)

    fun getWeeklyTotalSpending(weekStart: Long, workspaceId: Long?): Flow<Double> =
        resourceDao.getWeeklyTotalSpending(weekStart, workspaceId)

    fun getWeeklyTotal(weekStart: Long, workspaceId: Long?): Flow<Double> =
        resourceDao.getWeeklyTotal(weekStart, workspaceId)

    suspend fun insertResource(resource: ResourceEntity): Long = withContext(Dispatchers.IO) {
        resourceDao.insertResource(resource)
    }

    suspend fun updateResource(resource: ResourceEntity): Int = withContext(Dispatchers.IO) {
        resourceDao.updateResource(resource)
    }

    suspend fun deleteResource(resource: ResourceEntity): Int = withContext(Dispatchers.IO) {
        resourceDao.deleteResource(resource)
    }

    suspend fun deleteAllResources(): Int = withContext(Dispatchers.IO) {
        resourceDao.deleteAllResources()
    }
}
