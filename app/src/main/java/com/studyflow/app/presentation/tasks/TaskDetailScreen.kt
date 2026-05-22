package com.studyflow.app.presentation.tasks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.theme.StudyFlowTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.loadTaskForEdit(taskId)
    }

    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) } // 0=Low, 1=Medium, 2=High
    var category by remember { mutableStateOf("General") }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var scheduledHour by remember { mutableIntStateOf(9) }
    var selectedWorkspaceId by remember { mutableStateOf<Long?>(null) }
    var hasInitializedWorkspace by remember { mutableStateOf(false) }

    // Pre-fill fields when detailTask is loaded
    val detailTask = uiState.detailTask
    LaunchedEffect(detailTask) {
        if (detailTask != null && taskId > 0) {
            title = detailTask.title
            description = detailTask.description
            priority = detailTask.priority
            category = detailTask.category
            dueDateMillis = detailTask.dueDateMillis
            scheduledHour = detailTask.dueDateMillis?.let { extractHourOfDay(it) } ?: 9
            selectedWorkspaceId = detailTask.workspaceId
        } else if (taskId <= 0) {
            title = ""
            description = ""
            priority = 1
            category = "General"
            dueDateMillis = null
            scheduledHour = 9
        }
    }

    LaunchedEffect(uiState.activeWorkspaceId) {
        if (taskId <= 0 && !hasInitializedWorkspace && uiState.activeWorkspaceId != null) {
            selectedWorkspaceId = uiState.activeWorkspaceId
            hasInitializedWorkspace = true
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && detailTask != null) {
        ConfirmDialog(
            title = "Delete Task",
            body = "Are you sure you want to delete this task?",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteTask(detailTask) {
                    Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            },
            onCancel = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId <= 0) "Add Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (taskId > 0) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && taskId > 0 && detailTask == null) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else {
            TaskFormContent(
                title = title,
                onTitleChange = { title = it },
                description = description,
                onDescriptionChange = { description = it },
                priority = priority,
                onPriorityChange = { priority = it },
                category = category,
                onCategoryChange = { category = it },
                dueDateMillis = dueDateMillis,
                onDueDateClick = { showDatePicker = true },
                scheduledHour = scheduledHour,
                onScheduleTimeClick = {
                    if (dueDateMillis != null) {
                        showTimePicker = true
                    } else {
                        Toast.makeText(context, "Pick a due date first", Toast.LENGTH_SHORT).show()
                    }
                },
                workspaces = uiState.workspaces,
                selectedWorkspaceId = selectedWorkspaceId,
                onWorkspaceSelect = { selectedWorkspaceId = it },
                onSaveClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val scheduledDueDateMillis = buildScheduledDateTimeMillis(dueDateMillis, scheduledHour)
                        viewModel.saveTask(
                            id = taskId,
                            title = title,
                            description = description,
                            priority = priority,
                            category = category,
                            dueDateMillis = scheduledDueDateMillis,
                            workspaceId = selectedWorkspaceId
                        ) {
                            Toast.makeText(context, "Task saved successfully", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                },
                isSaving = uiState.isSaving,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Schedule Time") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (7..22).forEach { hour ->
                        val isSelected = scheduledHour == hour
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scheduledHour = hour
                                    showTimePicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = formatHourLabel(hour),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormContent(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    dueDateMillis: Long?,
    onDueDateClick: () -> Unit,
    scheduledHour: Int,
    onScheduleTimeClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaving: Boolean,
    workspaces: List<WorkspaceEntity> = emptyList(),
    selectedWorkspaceId: Long? = null,
    onWorkspaceSelect: (Long?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categories = listOf("General", "Study", "Math", "Science", "Personal", "Work")
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        TextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Priority Selector
        Column {
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Low", "Medium", "High").forEachIndexed { index, name ->
                    val isSelected = priority == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPriorityChange(index) },
                        label = { Text(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Category Dropdown
        Column {
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    readOnly = true,
                    value = category,
                    onValueChange = {},
                    label = { Text("Select Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onCategoryChange(selectionOption)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Due Date Picker Button
        Column {
            Text("Due Date", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                onClick = onDueDateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (dueDateMillis != null) {
                            val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                            formatter.format(Date(dueDateMillis))
                        } else {
                            "No due date selected"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                }
            }
        }

        // Schedule Time
        Column {
            Text("Schedule Time", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                onClick = onScheduleTimeClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (dueDateMillis != null) {
                            formatHourLabel(scheduledHour)
                        } else {
                            "Pick a due date first"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Select Time")
                }
            }
        }

        // Workspace Selector
        if (workspaces.isNotEmpty()) {
            var workspaceDropdownExpanded by remember { mutableStateOf(false) }
            val selectedWs = workspaces.find { it.id == selectedWorkspaceId }
            Column {
                Text("Workspace", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = workspaceDropdownExpanded,
                    onExpandedChange = { workspaceDropdownExpanded = !workspaceDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedWs?.let { "${it.iconEmoji} ${it.name}" } ?: "No Workspace",
                        onValueChange = {},
                        label = { Text("Assign to Workspace") },
                        leadingIcon = {
                            if (selectedWs != null) {
                                val wsColor = try {
                                    Color(android.graphics.Color.parseColor(selectedWs.colorHex))
                                } catch (e: Exception) { Color(0xFF8B5CF6) }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(wsColor)
                                )
                            } else {
                                Icon(Icons.Default.WorkOutline, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workspaceDropdownExpanded) },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = workspaceDropdownExpanded,
                        onDismissRequest = { workspaceDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Workspace") },
                            onClick = {
                                onWorkspaceSelect(null)
                                workspaceDropdownExpanded = false
                            }
                        )
                        workspaces.forEach { ws ->
                            val wsColor = try {
                                Color(android.graphics.Color.parseColor(ws.colorHex))
                            } catch (e: Exception) { Color(0xFF8B5CF6) }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(wsColor)
                                        )
                                        Text("${ws.iconEmoji} ${ws.name}")
                                    }
                                },
                                onClick = {
                                    onWorkspaceSelect(ws.id)
                                    workspaceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save Task")
            }
        }
    }
}

private fun buildScheduledDateTimeMillis(dateMillis: Long?, hour: Int): Long? {
    if (dateMillis == null) return null
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun extractHourOfDay(timeMillis: Long): Int {
    return Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.HOUR_OF_DAY)
}

private fun formatHourLabel(hour: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%02d:00 %s", displayHour, amPm)
}

@Preview(showBackground = true, name = "Task Form Light")
@Composable
private fun TaskFormPreviewLight() {
    StudyFlowTheme(darkTheme = false) {
        Surface {
            TaskFormContent(
                title = "Study Math",
                onTitleChange = {},
                description = "Do practice questions 1-10",
                onDescriptionChange = {},
                priority = 1,
                onPriorityChange = {},
                category = "Math",
                onCategoryChange = {},
                dueDateMillis = System.currentTimeMillis(),
                onDueDateClick = {},
                scheduledHour = 9,
                onScheduleTimeClick = {},
                onSaveClick = {},
                isSaving = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Task Form Dark")
@Composable
private fun TaskFormPreviewDark() {
    StudyFlowTheme(darkTheme = true) {
        Surface {
            TaskFormContent(
                title = "Study Math",
                onTitleChange = {},
                description = "Do practice questions 1-10",
                onDescriptionChange = {},
                priority = 2,
                onPriorityChange = {},
                category = "Math",
                onCategoryChange = {},
                dueDateMillis = null,
                onDueDateClick = {},
                scheduledHour = 9,
                onScheduleTimeClick = {},
                onSaveClick = {},
                isSaving = false
            )
        }
    }
}

