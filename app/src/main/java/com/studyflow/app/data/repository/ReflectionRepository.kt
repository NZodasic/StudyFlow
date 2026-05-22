package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.ReflectionDao
import com.studyflow.app.data.local.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionRepository @Inject constructor(
    private val reflectionDao: ReflectionDao
) {
    fun getAllReflections(): Flow<List<ReflectionEntity>> = reflectionDao.getAllReflections()

    fun getReflectionByDate(dateStart: Long, dateEnd: Long): Flow<ReflectionEntity?> =
        reflectionDao.getReflectionByDate(dateStart, dateEnd)

    suspend fun getReflectionByDateSuspended(dateStart: Long, dateEnd: Long): ReflectionEntity? =
        withContext(Dispatchers.IO) {
            reflectionDao.getReflectionByDateSuspended(dateStart, dateEnd)
        }

    fun getLatestReflection(): Flow<ReflectionEntity?> = reflectionDao.getLatestReflection()

    suspend fun insertReflection(reflection: ReflectionEntity): Long = withContext(Dispatchers.IO) {
        reflectionDao.insertReflection(reflection)
    }

    suspend fun updateReflection(reflection: ReflectionEntity): Int = withContext(Dispatchers.IO) {
        reflectionDao.updateReflection(reflection)
    }

    suspend fun deleteReflection(reflection: ReflectionEntity): Int = withContext(Dispatchers.IO) {
        reflectionDao.deleteReflection(reflection)
    }
}
