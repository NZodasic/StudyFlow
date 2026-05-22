package com.studyflow.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.UserSettingsEntity
import com.studyflow.app.data.repository.ResourceRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getUserSettings()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { settings ->
                    val finalSettings = settings ?: UserSettingsEntity() // Default settings if null
                    if (settings == null) {
                        // Persist default settings
                        settingsRepository.updateSettings(finalSettings)
                    }
                    _uiState.update { it.copy(userSettings = finalSettings, isLoading = false) }
                }
        }
    }

    fun updateTheme(isDarkTheme: Boolean) {
        val current = _uiState.value.userSettings ?: UserSettingsEntity()
        val updated = current.copy(isDarkTheme = isDarkTheme)
        viewModelScope.launch {
            settingsRepository.updateSettings(updated)
        }
    }

    fun updatePomodoroDuration(minutes: Int) {
        val current = _uiState.value.userSettings ?: UserSettingsEntity()
        val updated = current.copy(pomodoroDurationMinutes = minutes)
        viewModelScope.launch {
            settingsRepository.updateSettings(updated)
        }
    }

    fun updateShortBreak(minutes: Int) {
        val current = _uiState.value.userSettings ?: UserSettingsEntity()
        val updated = current.copy(shortBreakMinutes = minutes)
        viewModelScope.launch {
            settingsRepository.updateSettings(updated)
        }
    }

    fun updateLongBreak(minutes: Int) {
        val current = _uiState.value.userSettings ?: UserSettingsEntity()
        val updated = current.copy(longBreakMinutes = minutes)
        viewModelScope.launch {
            settingsRepository.updateSettings(updated)
        }
    }

    fun clearAllTasks(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                taskRepository.deleteAllTasks()
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun updateRecoveryMode(isRecoveryMode: Boolean) {
        val current = _uiState.value.userSettings ?: UserSettingsEntity()
        val updated = current.copy(
            isRecoveryMode = isRecoveryMode,
            currentStudyState = if (isRecoveryMode) "RECOVERY" else "FOCUS",
            studyStateOverride = if (isRecoveryMode) "RECOVERY" else "AUTO"
        )
        viewModelScope.launch {
            settingsRepository.updateSettings(updated)
        }
    }

    fun clearAllResources(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                resourceRepository.deleteAllResources()
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
