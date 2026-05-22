package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettingsSuspended(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSettings(settings: UserSettingsEntity): Long
}
