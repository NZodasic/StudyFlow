package com.studyflow.app.presentation.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTaskToPlace by remember { mutableStateOf<TaskEntity?>(null) }
    var showQuickAddDialog by remember { mutableStateOf<Pair<Boolean, Int>>(Pair(false, 9)) } // (show, hour)
    var newTaskTitle by remember { mutableStateOf("") }
    var taskOptionsTarget by remember { mutableStateOf<TaskEntity?>(null) }
    val isDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Daily Schedule",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = uiState.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Hourly Timeline List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.slots) { slot ->
                            TimelineHourRow(
                                slot = slot,
                                isPlaceSelected = selectedTaskToPlace != null,
                                onPlaceClick = {
                                    selectedTaskToPlace?.let { task ->
                                        viewModel.rescheduleTask(task.id, slot.hour)
                                        selectedTaskToPlace = null
                                    } ?: run {
                                        showQuickAddDialog = Pair(true, slot.hour)
                                    }
                                },
                                onTaskClick = { task ->
                                    taskOptionsTarget = task
                                }
                            )
                        }
                    }
                }

                // Bottom Panel for Unscheduled Tasks (Place System)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.65f else 0.9f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.75f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (selectedTaskToPlace != null) "📍 Tap any hour's '+' slot to schedule" else "Unscheduled & Overdue Tasks",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedTaskToPlace != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )

                                if (uiState.unscheduledTasks.isEmpty()) {
                                    Text(
                                        text = "All tasks scheduled! 🎉",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(uiState.unscheduledTasks) { task ->
                                            val isSelected = selectedTaskToPlace?.id == task.id
                                            val priorityColor = when (task.priority) {
                                                0 -> Color(0xFF10B981) // Low (Emerald)
                                                1 -> Color(0xFF00D8F6) // Medium (Cyan)
                                                else -> Color(0xFFFF5E5E) // High (Red)
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .width(160.dp)
                                                    .clickable {
                                                        selectedTaskToPlace = if (isSelected) null else task
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            brush = Brush.verticalGradient(
                                                                colors = listOf(
                                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                                    else MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.6f else 0.9f),
                                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                                    else MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.3f else 0.7f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            brush = Brush.linearGradient(
                                                                colors = if (isSelected) {
                                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                                                } else {
                                                                    listOf(priorityColor.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))
                                                                }
                                                            ),
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                        .padding(12.dp)
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = task.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = task.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = priorityColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Dialog
    if (showQuickAddDialog.first) {
        AlertDialog(
            onDismissRequest = { showQuickAddDialog = Pair(false, 9) },
            title = { Text("Quick Add Task", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.quickAddTask(newTaskTitle, showQuickAddDialog.second)
                            newTaskTitle = ""
                            showQuickAddDialog = Pair(false, 9)
                        }
                    }
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddDialog = Pair(false, 9) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Scheduled Task Options Dialog
    taskOptionsTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { taskOptionsTarget = null },
            title = { Text("Task Options: ${task.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose an action for this scheduled task:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            selectedTaskToPlace = task
                            taskOptionsTarget = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Move / Reschedule", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.unscheduleTask(task.id)
                            taskOptionsTarget = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove from Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { taskOptionsTarget = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TimelineHourRow(
    slot: TimelineHourSlot,
    isPlaceSelected: Boolean,
    onPlaceClick: () -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Time label
        Text(
            text = slot.timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .width(72.dp)
                .padding(top = 8.dp)
        )

        // Middle Column: Timeline node line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        )
                    )
            )
        }

        // Right Column: Content items
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Render Pomodoro sessions in this hour
            slot.pomodoros.forEach { pomodoro ->
                val focusColor = Color(0xFF9061F9) // Amethyst Violet
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        focusColor.copy(alpha = if (isDark) 0.15f else 0.12f),
                                        focusColor.copy(alpha = if (isDark) 0.05f else 0.04f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        focusColor.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Focus",
                                tint = focusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Focus: ${pomodoro.taskLabel.ifBlank { "Deep Work" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = focusColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${pomodoro.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = focusColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Render tasks scheduled for this hour
            slot.tasks.forEach { task ->
                val priorityColor = when (task.priority) {
                    0 -> Color(0xFF10B981) // Low (Emerald)
                    1 -> Color(0xFF00D8F6) // Medium (Cyan)
                    else -> Color(0xFFFF5E5E) // High (Red)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTaskClick(task) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.85f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.6f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        priorityColor.copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(priorityColor)
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // If empty slot or place system active
            if (slot.tasks.isEmpty() && slot.pomodoros.isEmpty()) {
                val baseColor = if (isPlaceSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isPlaceSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isPlaceSelected) {
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            } else {
                                Brush.linearGradient(colors = listOf(baseColor, baseColor))
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onPlaceClick() }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = baseColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPlaceSelected) "Place task here" else "Schedule task",
                            style = MaterialTheme.typography.bodySmall,
                            color = baseColor,
                            fontWeight = if (isPlaceSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

