package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,     // single-row settings table
    val isDarkTheme: Boolean = false,
    val pomodoroDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val selectedWorkspaceId: Long? = null,
    val isRecoveryMode: Boolean = false,
    val currentStudyState: String = "FOCUS",
    val studyStateOverride: String = "AUTO"
)
