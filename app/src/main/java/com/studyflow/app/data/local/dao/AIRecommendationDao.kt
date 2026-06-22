package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.AIRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIRecommendationDao {
    @Query("SELECT * FROM ai_recommendations ORDER BY priority = 'high' DESC, priority = 'medium' DESC, generatedAtMillis DESC")
    fun getAllRecommendations(): Flow<List<AIRecommendationEntity>>

    @Query("SELECT * FROM ai_recommendations WHERE expiresAtMillis > :currentTime ORDER BY priority = 'high' DESC, priority = 'medium' DESC, generatedAtMillis DESC")
    fun getActiveRecommendations(currentTime: Long): Flow<List<AIRecommendationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecommendations(recommendations: List<AIRecommendationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecommendation(recommendation: AIRecommendationEntity): Long

    @Query("UPDATE ai_recommendations SET isRead = :isRead WHERE id = :id")
    fun setReadStatus(id: Long, isRead: Boolean): Int

    @Query("DELETE FROM ai_recommendations WHERE id = :id")
    fun deleteRecommendationById(id: Long): Int

    @Query("DELETE FROM ai_recommendations")
    fun deleteAllRecommendations(): Int

    @Query("DELETE FROM ai_recommendations WHERE expiresAtMillis <= :currentTime")
    fun deleteExpiredRecommendations(currentTime: Long): Int
}
