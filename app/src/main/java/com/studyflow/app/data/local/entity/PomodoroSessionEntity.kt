package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationMinutes: Int,
    val taskLabel: String = "",
    val workspaceId: Long? = null,
    val completedAtMillis: Long = System.currentTimeMillis(),
    val focusRating: Int = 0,
    val reflectionNote: String = "",
    val distractionsCount: Int = 0,
    val appExitsCount: Int = 0
)
