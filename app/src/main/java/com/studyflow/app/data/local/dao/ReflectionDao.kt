package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM reflections ORDER BY dateMillis DESC")
    fun getAllReflections(): Flow<List<ReflectionEntity>>

    @Query("SELECT * FROM reflections WHERE dateMillis >= :dateStart AND dateMillis <= :dateEnd LIMIT 1")
    fun getReflectionByDate(dateStart: Long, dateEnd: Long): Flow<ReflectionEntity?>

    @Query("SELECT * FROM reflections WHERE dateMillis >= :dateStart AND dateMillis <= :dateEnd LIMIT 1")
    fun getReflectionByDateSuspended(dateStart: Long, dateEnd: Long): ReflectionEntity?

    @Query("SELECT * FROM reflections ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestReflection(): Flow<ReflectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReflection(reflection: ReflectionEntity): Long

    @Update
    fun updateReflection(reflection: ReflectionEntity): Int

    @Delete
    fun deleteReflection(reflection: ReflectionEntity): Int

    @Query("SELECT * FROM reflections ORDER BY dateMillis DESC")
    fun getAllReflectionsSuspended(): List<ReflectionEntity>
}
