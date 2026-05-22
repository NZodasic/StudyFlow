package com.studyflow.app.data.repository

import com.studyflow.app.data.local.dao.NoteDao
import com.studyflow.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(workspaceId: Long?): Flow<List<NoteEntity>> = noteDao.getAllNotes(workspaceId)

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    fun searchNotes(query: String, workspaceId: Long?): Flow<List<NoteEntity>> = noteDao.searchNotes(query, workspaceId)

    suspend fun insertNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity): Int = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity): Int = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }
}
