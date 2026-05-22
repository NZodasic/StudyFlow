package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reflections")
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val accomplishments: String = "",
    val energyLevel: Int = 3,
    val mood: String = "Calm",
    val biggestDistraction: String = "None",
    val productivityRating: Int = 3,
    val stressLevel: Int = 3,
    val oneWord: String = "",
    val madeProductive: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
