package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_recommendations")
data class AIRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // schedule, break, focus, health, goal
    val priority: String, // high, medium, low
    val message: String,
    val action: String? = null, // e.g. SHOW_BREATHING_EXERCISE, START_POMODORO, TAKE_NAP, etc.
    val confidence: Double,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L, // default 24h
    val isRead: Boolean = false
)
