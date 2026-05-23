package com.studyflow.app.presentation.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import com.studyflow.app.data.local.entity.UserSettingsEntity
import com.studyflow.app.data.repository.PomodoroRepository
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val pomodoroRepository: PomodoroRepository,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var currentWorkspaceId: Long? = null
    private var historyJob: Job? = null

    private var tasksJob: Job? = null

    init {
        observeUserSettings()
    }

    private fun observeTasks(workspaceId: Long?) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            taskRepository.getAllTasks(workspaceId).collect { tasks ->
                _uiState.update { it.copy(tasks = tasks.filter { !it.isCompleted }) }
            }
        }
    }

    private fun observeUserSettings() {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                val s = settings ?: UserSettingsEntity()
                val workspaceId = s.selectedWorkspaceId
                if (currentWorkspaceId != workspaceId || historyJob == null) {
                    currentWorkspaceId = workspaceId
                    observeHistory(workspaceId)
                    observeTasks(workspaceId)
                }
                _uiState.update { state ->
                    val isFocus = state.mode == PomodoroMode.FOCUS
                    val isShort = state.mode == PomodoroMode.SHORT_BREAK

                    val currentModeDuration = when {
                        isFocus -> s.pomodoroDurationMinutes
                        isShort -> s.shortBreakMinutes
                        else -> s.longBreakMinutes
                    }

                    state.copy(
                        focusMinutes = s.pomodoroDurationMinutes,
                        shortBreakMinutes = s.shortBreakMinutes,
                        longBreakMinutes = s.longBreakMinutes,
                        secondsRemaining = if (!state.isRunning) currentModeDuration * 60 else state.secondsRemaining,
                        totalSeconds = if (!state.isRunning) currentModeDuration * 60 else state.totalSeconds,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeHistory(workspaceId: Long?) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            pomodoroRepository.getAllSessions(workspaceId).collect { list ->
                _uiState.update { it.copy(history = list) }
            }
        }
    }

    fun updateTaskLabel(label: String) {
        _uiState.update { it.copy(taskLabel = label) }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }
            }
            onTimerComplete()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        pauseTimer()
        _uiState.update { state ->
            val duration = when (state.mode) {
                PomodoroMode.FOCUS -> state.focusMinutes
                PomodoroMode.SHORT_BREAK -> state.shortBreakMinutes
                PomodoroMode.LONG_BREAK -> state.longBreakMinutes
            }
            state.copy(
                secondsRemaining = duration * 60,
                totalSeconds = duration * 60,
                appExitsCount = 0
            )
        }
    }

    fun skipSession() {
        pauseTimer()
        transitionMode()
    }

    private fun onTimerComplete() {
        pauseTimer()
        val state = _uiState.value
        if (state.mode == PomodoroMode.FOCUS) {
            _uiState.update { it.copy(
                showReflectionDialog = true,
                tempRating = 5,
                tempReflection = "",
                tempDistractions = 0
            ) }
        } else {
            transitionMode()
        }
    }

    fun setReflectionDialogVisible(visible: Boolean) {
        if (!visible && _uiState.value.showReflectionDialog) {
            saveSessionReflection(rating = 0, note = "", distractions = 0)
            return
        }
        _uiState.update { it.copy(showReflectionDialog = visible) }
    }

    fun updateTempRating(rating: Int) {
        _uiState.update { it.copy(tempRating = rating) }
    }

    fun updateTempReflection(reflection: String) {
        _uiState.update { it.copy(tempReflection = reflection) }
    }

    fun updateTempDistractions(distractions: Int) {
        _uiState.update { it.copy(tempDistractions = distractions) }
    }

    fun saveSessionReflection(rating: Int, note: String, distractions: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val newSession = PomodoroSessionEntity(
                durationMinutes = state.focusMinutes,
                taskLabel = state.taskLabel.trim(),
                workspaceId = currentWorkspaceId,
                focusRating = rating,
                reflectionNote = note.trim(),
                distractionsCount = distractions,
                appExitsCount = state.appExitsCount
            )
            pomodoroRepository.insertSession(newSession)
            _uiState.update { it.copy(
                showReflectionDialog = false,
                taskLabel = "",
                appExitsCount = 0
            ) }
            transitionMode()
        }
    }


    private fun transitionMode() {
        _uiState.update { state ->
            val isFocus = state.mode == PomodoroMode.FOCUS
            val isShort = state.mode == PomodoroMode.SHORT_BREAK

            val (nextMode, nextCount) = when {
                isFocus -> {
                    if (state.sessionCount >= 4) {
                        Pair(PomodoroMode.LONG_BREAK, 1)
                    } else {
                        Pair(PomodoroMode.SHORT_BREAK, state.sessionCount)
                    }
                }
                isShort -> Pair(PomodoroMode.FOCUS, state.sessionCount + 1)
                else -> Pair(PomodoroMode.FOCUS, 1) // After long break, reset to focus 1
            }

            val nextDuration = when (nextMode) {
                PomodoroMode.FOCUS -> state.focusMinutes
                PomodoroMode.SHORT_BREAK -> state.shortBreakMinutes
                PomodoroMode.LONG_BREAK -> state.longBreakMinutes
            }

            state.copy(
                mode = nextMode,
                sessionCount = nextCount,
                secondsRemaining = nextDuration * 60,
                totalSeconds = nextDuration * 60,
                appExitsCount = 0
            )
        }
    }

    fun incrementAppExits() {
        if (_uiState.value.isRunning && _uiState.value.mode == PomodoroMode.FOCUS) {
            _uiState.update { it.copy(appExitsCount = it.appExitsCount + 1) }
        }
    }

    fun debugCompleteSession() {
        timerJob?.cancel()
        _uiState.update { it.copy(
            mode = PomodoroMode.FOCUS,
            secondsRemaining = 5,
            isRunning = true
        ) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }
            }
            onTimerComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
