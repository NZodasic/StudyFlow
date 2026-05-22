package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.PomodoroDao
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroRepository @Inject constructor(
    private val pomodoroDao: PomodoroDao
) {
    fun getAllSessions(workspaceId: Long?): Flow<List<PomodoroSessionEntity>> = pomodoroDao.getAllSessions(workspaceId)

    fun getSessionsToday(dayStart: Long, workspaceId: Long?): Flow<Int> = pomodoroDao.getSessionsToday(dayStart, workspaceId)

    suspend fun insertSession(session: PomodoroSessionEntity): Long = withContext(Dispatchers.IO) {
        pomodoroDao.insertSession(session)
    }

    suspend fun deleteSession(session: PomodoroSessionEntity): Int = withContext(Dispatchers.IO) {
        pomodoroDao.deleteSession(session)
    }
}
