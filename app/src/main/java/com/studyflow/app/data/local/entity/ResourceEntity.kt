package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,            // Caffeine, Sleep, Energy, Stress, Hydration, Environment
    val note: String = "",
    val workspaceId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val productivityImpact: Int = 0, // -1 (negative), 0 (neutral), 1 (positive)
    val studyEnvironment: String = "" // Home, Library, Cafe, School
)
