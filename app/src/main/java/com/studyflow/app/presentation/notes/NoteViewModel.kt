package com.studyflow.app.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.NoteEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.repository.NoteRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.WorkspaceRepository
import com.studyflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState(isLoading = true))
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _currentWorkspaceId = MutableStateFlow<Long?>(null)
    private val _selectedSubject = MutableStateFlow("All")
    private var saveJob: Job? = null
    private var selectedNoteJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                _currentWorkspaceId.value = settings?.selectedWorkspaceId
            }
        }
        viewModelScope.launch {
            workspaceRepository.getAllWorkspaces().collect { workspaces ->
                _uiState.update { it.copy(workspaces = workspaces) }
            }
        }
        observeNotes()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeNotes() {
        viewModelScope.launch {
            combine(_searchQuery, _currentWorkspaceId, _selectedSubject) { query, workspaceId, subject ->
                Triple(query, workspaceId, subject)
            }
            .flatMapLatest { (query, workspaceId, subject) ->
                noteRepository.getAllNotes(workspaceId).map { notes ->
                    // Extract unique non-empty subject tags
                    val subjects = listOf("All") + notes.map { it.subject }.filter { it.isNotBlank() }.distinct()

                    // Filter search query
                    val searchedNotes = if (query.isBlank()) {
                        notes
                    } else {
                        notes.filter {
                            it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true) ||
                            it.subject.contains(query, ignoreCase = true)
                        }
                    }

                    // Filter subject tag
                    val filteredNotes = if (subject == "All") {
                        searchedNotes
                    } else {
                        searchedNotes.filter { it.subject.equals(subject, ignoreCase = true) }
                    }

                    Triple(filteredNotes, notes, subjects)
                }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
            .collect { (filtered, all, subjects) ->
                _uiState.update { state ->
                    state.copy(
                         notes = filtered,
                         pinnedNotes = filtered.filter { it.isPinned },
                         subjects = subjects,
                         isLoading = false
                    )
                }
                // Refresh backlinks if viewing a note
                val current = _uiState.value.currentNote
                if (current != null && current.id != 0L) {
                    val backlinks = all.filter {
                        it.id != current.id && it.content.contains("[[${current.title}]]", ignoreCase = true)
                    }
                    _uiState.update { it.copy(backlinks = backlinks) }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    fun selectNote(noteId: Long) {
        saveJob?.cancel()
        selectedNoteJob?.cancel()
        if (noteId == -1L) {
            _uiState.update {
                it.copy(
                    currentNote = NoteEntity(
                        id = 0L,
                        title = "",
                        content = "",
                        subject = "",
                        workspaceId = _currentWorkspaceId.value
                    ),
                    backlinks = emptyList()
                )
            }
        } else {
            selectedNoteJob = viewModelScope.launch {
                noteRepository.getNoteById(noteId).collect { note ->
                    _uiState.update { it.copy(currentNote = note) }
                    if (note != null) {
                        // Gather backlinks
                        val allNotes = noteRepository.getAllNotes(_currentWorkspaceId.value).first()
                        val backlinks = allNotes.filter {
                            it.id != note.id && it.content.contains("[[${note.title}]]", ignoreCase = true)
                        }
                        _uiState.update { it.copy(backlinks = backlinks) }
                    }
                }
            }
        }
    }

    fun findOrCreateNoteByTitle(title: String, onFound: (Long) -> Unit) {
        viewModelScope.launch {
            val workspaceId = _currentWorkspaceId.value
            val notes = noteRepository.getAllNotes(workspaceId).first()
            val foundNote = notes.find { it.title.equals(title, ignoreCase = true) }
            if (foundNote != null) {
                onFound(foundNote.id)
            } else {
                val newNote = NoteEntity(
                    title = title,
                    content = "",
                    subject = "General",
                    workspaceId = workspaceId,
                    updatedAtMillis = System.currentTimeMillis()
                )
                val newId = noteRepository.insertNote(newNote)
                onFound(newId)
            }
        }
    }

    fun quickBrainDump(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val cleanContent = content.trim()
            val title = if (cleanContent.length > 25) {
                cleanContent.take(25) + "..."
            } else {
                cleanContent
            }
            val note = NoteEntity(
                title = title,
                content = cleanContent,
                subject = "Brain Dump",
                workspaceId = _currentWorkspaceId.value,
                updatedAtMillis = System.currentTimeMillis()
            )
            saveNote(note)
        }
    }

    fun convertToTask(note: NoteEntity) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = note.title.ifBlank { "Task from Note" },
                description = note.content,
                priority = 1, // Medium
                category = note.subject.ifBlank { "General" },
                workspaceId = note.workspaceId,
                createdAtMillis = System.currentTimeMillis()
            )
            taskRepository.insertTask(task)
            _uiState.update { it.copy(successMessage = "Converted note to task: '${task.title}'") }
        }
    }

    fun updateNoteDetails(title: String, content: String, subject: String) {
        val current = _uiState.value.currentNote ?: return
        val updated = current.copy(
            title = title,
            content = content,
            subject = subject,
            updatedAtMillis = System.currentTimeMillis()
        )
        _uiState.update { it.copy(currentNote = updated) }

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1000)
            saveNote(updated)
        }
    }

    fun toggleCurrentNotePin() {
        val current = _uiState.value.currentNote ?: return
        val updated = current.copy(
            isPinned = !current.isPinned,
            updatedAtMillis = System.currentTimeMillis()
        )
        _uiState.update { it.copy(currentNote = updated) }
        viewModelScope.launch {
            saveNote(updated)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                isPinned = !note.isPinned,
                updatedAtMillis = System.currentTimeMillis()
            )
            noteRepository.updateNote(updated)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }

    fun saveCurrentNoteImmediately() {
        saveJob?.cancel()
        val current = _uiState.value.currentNote ?: return
        if (current.id == 0L && current.title.isBlank() && current.content.isBlank() && current.subject.isBlank()) {
            return
        }
        viewModelScope.launch {
            saveNote(current)
        }
    }

    private suspend fun saveNote(note: NoteEntity) {
        if (note.id == 0L) {
            val noteWithWorkspace = if (note.workspaceId == null) {
                note.copy(workspaceId = _currentWorkspaceId.value)
            } else {
                note
            }
            val newId = noteRepository.insertNote(noteWithWorkspace)
            _uiState.update { state ->
                if (state.currentNote?.id == 0L) {
                    state.copy(currentNote = state.currentNote.copy(id = newId, workspaceId = noteWithWorkspace.workspaceId))
                } else {
                    state
                }
            }
        } else {
            noteRepository.updateNote(note)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
