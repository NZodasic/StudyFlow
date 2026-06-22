package com.studyflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.studyflow.app.data.local.dao.*
import com.studyflow.app.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        NoteEntity::class,
        ResourceEntity::class,
        PomodoroSessionEntity::class,
        UserSettingsEntity::class,
        WorkspaceEntity::class,
        ReflectionEntity::class,
        AIRecommendationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao
    abstract fun resourceDao(): ResourceDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun settingsDao(): SettingsDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun aiRecommendationDao(): AIRecommendationDao
}
