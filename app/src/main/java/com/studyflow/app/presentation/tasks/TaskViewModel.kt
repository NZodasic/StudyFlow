package com.studyflow.app.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
 
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {
 
    private val _uiState = MutableStateFlow(TaskUiState(isLoading = true))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
 
    private var allTasks: List<TaskEntity> = emptyList()
    private var tasksJob: Job? = null
    private var currentWorkspaceId: Long? = null
 
    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                currentWorkspaceId = settings?.selectedWorkspaceId
                _uiState.update { it.copy(
                    hasActiveWorkspace = currentWorkspaceId != null,
                    activeWorkspaceId = currentWorkspaceId
                ) }
                observeTasks(currentWorkspaceId)
            }
        }
        viewModelScope.launch {
            workspaceRepository.getAllWorkspaces().collect { workspaces ->
                _uiState.update { it.copy(workspaces = workspaces) }
            }
        }
    }

    private fun observeTasks(workspaceId: Long?) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.getAllTasks(workspaceId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage, isLoading = false) }
                }
                .collect { tasks ->
                    allTasks = tasks
                    updateFilteredTasks()
                }
        }
    }

    private fun updateFilteredTasks() {
        val query = _uiState.value.searchQuery
        val filter = _uiState.value.selectedFilter
        val filtered = filterTasks(allTasks, query, filter)
        _uiState.update { it.copy(tasks = filtered, isLoading = false) }
    }

    private fun filterTasks(tasks: List<TaskEntity>, query: String, filter: TaskFilter): List<TaskEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        return tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.TODAY -> {
                    task.dueDateMillis != null && task.dueDateMillis <= endOfToday && !task.isCompleted
                }
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
                TaskFilter.HIGH_PRIORITY -> task.priority == 2
            }

            matchesQuery && matchesFilter
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateFilteredTasks()
    }

    fun updateFilter(filter: TaskFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        updateFilteredTasks()
    }

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            val currentTask = allTasks.find { it.id == taskId } ?: return@launch
            taskRepository.toggleTaskCompletion(taskId, !currentTask.isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            onComplete()
        }
    }

    fun loadTaskForEdit(taskId: Long) {
        if (taskId <= 0) {
            _uiState.update { it.copy(detailTask = null) }
            return
        }
        viewModelScope.launch {
            taskRepository.getTaskById(taskId).collect { task ->
                _uiState.update { it.copy(detailTask = task) }
            }
        }
    }

    fun saveTask(
        id: Long,
        title: String,
        description: String,
        priority: Int,
        category: String,
        dueDateMillis: Long?,
        workspaceId: Long? = null,
        onComplete: () -> Unit
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val existing = _uiState.value.detailTask
                // Use explicit workspaceId if provided, else keep existing or fall back to current
                val resolvedWorkspaceId = workspaceId ?: existing?.workspaceId ?: currentWorkspaceId
                val task = TaskEntity(
                    id = if (id <= 0) 0 else id,
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    category = category,
                    dueDateMillis = dueDateMillis,
                    isCompleted = existing?.isCompleted ?: false,
                    workspaceId = resolvedWorkspaceId,
                    createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
                )
                if (id <= 0) {
                    taskRepository.insertTask(task)
                } else {
                    taskRepository.updateTask(task)
                }
                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage, isSaving = false) }
            }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(isSaveSuccess = false) }
    }

    fun selectWorkspace(workspaceId: Long) {
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettingsSuspended() ?: com.studyflow.app.data.local.entity.UserSettingsEntity()
            settingsRepository.updateSettings(settings.copy(selectedWorkspaceId = workspaceId))
        }
    }

    fun addWorkspace(name: String, emoji: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = workspaceRepository.insertWorkspace(
                com.studyflow.app.data.local.entity.WorkspaceEntity(
                    name = name.trim(),
                    iconEmoji = emoji,
                    colorHex = colorHex
                )
            )
            selectWorkspace(newId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
