package com.studyflow.app.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.data.repository.PomodoroRepository
import com.studyflow.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Calendar
import javax.inject.Inject

data class TimelineHourSlot(
    val hour: Int, // 24h format
    val timeLabel: String,
    val tasks: List<TaskEntity> = emptyList(),
    val pomodoros: List<PomodoroSessionEntity> = emptyList()
)

data class TimelineUiState(
    val isLoading: Boolean = false,
    val dateLabel: String = "",
    val slots: List<TimelineHourSlot> = emptyList(),
    val unscheduledTasks: List<TaskEntity> = emptyList(),
    val selectedWorkspaceId: Long? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var dataJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                val workspaceId = settings?.selectedWorkspaceId
                _uiState.update { it.copy(selectedWorkspaceId = workspaceId) }
                observeTimelineData(workspaceId)
            }
        }
    }

    private fun observeTimelineData(workspaceId: Long?) {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            val todayStart = getStartOfTodayMillis()
            val todayEnd = getEndOfTodayMillis()

            val calendar = Calendar.getInstance()
            val dateLabel = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(calendar.time)
            _uiState.update { it.copy(dateLabel = dateLabel) }

            combine(
                taskRepository.getAllTasks(workspaceId),
                pomodoroRepository.getAllSessions(workspaceId)
            ) { allTasks, allSessions ->
                // Filter tasks due today
                val todayTasks = allTasks.filter {
                    it.dueDateMillis != null && it.dueDateMillis >= todayStart && it.dueDateMillis <= todayEnd
                }

                // Filter pomodoros completed today
                val todayPomodoros = allSessions.filter {
                    it.completedAtMillis >= todayStart && it.completedAtMillis <= todayEnd
                }

                // Group tasks and pomodoros by hour
                val hourSlots = (7..22).map { hour ->
                    val slotTasks = todayTasks.filter { task ->
                        val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDateMillis ?: 0 }
                        taskCal.get(Calendar.HOUR_OF_DAY) == hour
                    }
                    val slotPomodoros = todayPomodoros.filter { pomodoro ->
                        val pomCal = Calendar.getInstance().apply { timeInMillis = pomodoro.completedAtMillis }
                        pomCal.get(Calendar.HOUR_OF_DAY) == hour
                    }
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val displayHour = when {
                        hour == 12 -> 12
                        hour > 12 -> hour - 12
                        else -> hour
                    }
                    TimelineHourSlot(
                        hour = hour,
                        timeLabel = String.format("%02d:00 %s", displayHour, amPm),
                        tasks = slotTasks,
                        pomodoros = slotPomodoros
                    )
                }

                // Keep tasks that cannot be placed into an hourly slot visible in the unscheduled queue.
                val unscheduled = allTasks.filter { task ->
                    if (task.isCompleted) return@filter false

                    val dueDateMillis = task.dueDateMillis ?: return@filter true
                    if (dueDateMillis < todayStart || dueDateMillis > todayEnd) return@filter true

                    val taskHour = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
                        .get(Calendar.HOUR_OF_DAY)
                    taskHour !in 7..22
                }

                Pair(hourSlots, unscheduled)
            }.collect { (slots, unscheduled) ->
                _uiState.update {
                    it.copy(
                        slots = slots,
                        unscheduledTasks = unscheduled,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun rescheduleTask(taskId: Long, targetHour: Int) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId).first()
            if (task != null) {
                val cal = Calendar.getInstance()
                if (task.dueDateMillis != null) {
                    cal.timeInMillis = task.dueDateMillis
                }
                cal.set(Calendar.HOUR_OF_DAY, targetHour)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                taskRepository.updateTask(task.copy(dueDateMillis = cal.timeInMillis))
            }
        }
    }

    fun unscheduleTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId).first()
            if (task != null) {
                taskRepository.updateTask(task.copy(dueDateMillis = null))
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId).first()
            if (task != null) {
                taskRepository.deleteTask(task)
            }
        }
    }

    fun quickAddTask(title: String, hour: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            taskRepository.insertTask(
                TaskEntity(
                    title = title.trim(),
                    dueDateMillis = cal.timeInMillis,
                    workspaceId = _uiState.value.selectedWorkspaceId
                )
            )
        }
    }

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
