package com.studyflow.app.presentation.tasks

import com.studyflow.app.data.local.entity.TaskEntity

import com.studyflow.app.data.local.entity.WorkspaceEntity

enum class TaskFilter {
    ALL, TODAY, PENDING, COMPLETED, HIGH_PRIORITY
}

data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val detailTask: TaskEntity? = null,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val hasActiveWorkspace: Boolean = false,
    val activeWorkspaceId: Long? = null
)
