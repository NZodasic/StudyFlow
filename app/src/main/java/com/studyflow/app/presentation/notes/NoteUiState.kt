package com.studyflow.app.presentation.notes

import com.studyflow.app.data.local.entity.NoteEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity

data class NoteUiState(
    val notes: List<NoteEntity> = emptyList(),
    val pinnedNotes: List<NoteEntity> = emptyList(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val searchQuery: String = "",
    val currentNote: NoteEntity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedSubject: String = "All",
    val subjects: List<String> = emptyList(),
    val backlinks: List<NoteEntity> = emptyList()
)
