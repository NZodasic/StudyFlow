package com.studyflow.app.data.local.dao

import androidx.room.*
import com.studyflow.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun getAllNotes(workspaceId: Long?): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%') AND (:workspaceId IS NULL OR workspaceId = :workspaceId) ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun searchNotes(query: String, workspaceId: Long?): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(note: NoteEntity): Long

    @Update
    fun updateNote(note: NoteEntity): Int

    @Delete
    fun deleteNote(note: NoteEntity): Int
}
