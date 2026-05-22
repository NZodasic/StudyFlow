package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long? = null,
    val priority: Int = 1,           // 0=Low, 1=Medium, 2=High
    val category: String = "General",
    val isCompleted: Boolean = false,
    val workspaceId: Long? = null,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
