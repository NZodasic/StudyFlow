package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.SettingsDao
import com.studyflow.app.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    fun getUserSettings(): Flow<UserSettingsEntity?> = settingsDao.getUserSettings()

    suspend fun getUserSettingsSuspended(): UserSettingsEntity? = withContext(Dispatchers.IO) {
        settingsDao.getUserSettingsSuspended()
    }

    suspend fun updateSettings(settings: UserSettingsEntity): Long = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdateSettings(settings)
    }
}
