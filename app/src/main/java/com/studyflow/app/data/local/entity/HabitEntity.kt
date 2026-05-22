package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconEmoji: String = "⭐",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val workspaceId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
