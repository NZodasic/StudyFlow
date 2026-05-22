package com.studyflow.app.presentation.habits

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.HabitLogEntity
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.EmptyStateView
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.theme.StudyFlowTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ArrowForward

private fun parseHexColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF8B5CF6) // Fallback violet
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddHabitSheet by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var showAddWorkspaceDialog by remember { mutableStateOf(false) }

    if (showAddHabitSheet) {
        AddHabitBottomSheet(
            onDismiss = { showAddHabitSheet = false },
            onAddHabit = { name, emoji ->
                viewModel.addHabit(name, emoji)
                showAddHabitSheet = false
                Toast.makeText(context, "Habit added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (habitToDelete != null) {
        ConfirmDialog(
            title = "Delete Habit",
            body = "Are you sure you want to delete this habit and all its logs? This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                val target = habitToDelete
                if (target != null) {
                    viewModel.deleteHabit(target) {
                        Toast.makeText(context, "Habit deleted", Toast.LENGTH_SHORT).show()
                    }
                }
                habitToDelete = null
            },
            onCancel = { habitToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daily Habits", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (uiState.hasActiveWorkspace) {
                FloatingActionButton(
                    onClick = { showAddHabitSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else if (!uiState.hasActiveWorkspace) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select a Space",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Choose a workspace below to start viewing and managing your habits:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (uiState.workspaces.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Spaces Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a workspace to start organizing your studies, habits, and tasks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.workspaces) { workspace ->
                            val themeColor = remember(workspace.colorHex) { parseHexColor(workspace.colorHex) }
                            Card(
                                onClick = { viewModel.selectWorkspace(workspace.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = workspace.iconEmoji, fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = workspace.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Open Space",
                                        tint = themeColor
                                    )
                                }
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { showAddWorkspaceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Space")
                }
            }
        } else {
            HabitContent(
                uiState = uiState,
                onToggleHabit = viewModel::toggleHabit,
                onDeleteHabit = { habitToDelete = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    if (showAddWorkspaceDialog) {
        var workspaceName by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("📁") }
        var selectedColorHex by remember { mutableStateOf("#8B5CF6") }

        val emojiOptions = listOf("📁", "📚", "💻", "🎯", "🚀", "🎨", "🔬", "💼")
        val colorOptions = listOf("#8B5CF6", "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#EC4899")

        AlertDialog(
            onDismissRequest = { showAddWorkspaceDialog = false },
            title = { Text("Create New Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = workspaceName,
                        onValueChange = { workspaceName = it },
                        label = { Text("Workspace Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            emojiOptions.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedEmoji = emoji }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    Column {
                        Text("Select Color Theme", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colorOptions.forEach { hex ->
                                val color = parseHexColor(hex)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (workspaceName.isNotBlank()) {
                            viewModel.addWorkspace(workspaceName, selectedEmoji, selectedColorHex)
                            showAddWorkspaceDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkspaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HabitContent(
    uiState: HabitUiState,
    onToggleHabit: (Long) -> Unit,
    onDeleteHabit: (HabitEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Date Header
        Column {
            Text(
                text = "Today's Habits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentDateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Weekly mini calendar
        WeeklyCalendarOverview(daysOfWeek = uiState.daysOfWeek, habits = uiState.habits)

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Habit Lists
        if (uiState.habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    message = "No habits created yet. Click the + button to create one!"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = uiState.habits,
                    key = { it.habit.id }
                ) { habitLogs ->
                    HabitCard(
                        habitLogs = habitLogs,
                        onToggle = { onToggleHabit(habitLogs.habit.id) },
                        onDelete = { onDeleteHabit(habitLogs.habit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyCalendarOverview(
    daysOfWeek: List<Long>,
    habits: List<HabitWithWeeklyLogs>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                daysOfWeek.forEachIndexed { index, dayMillis ->
                    val totalHabits = habits.size
                    val completedCount = habits.count { habitWithLogs ->
                        habitWithLogs.logs.any { log ->
                            log.dateMillis == dayMillis
                        }
                    }

                    val percent = if (totalHabits > 0) completedCount.toFloat() / totalHabits else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dayLabels[index],
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        totalHabits == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                        percent >= 1f -> MaterialTheme.colorScheme.primary
                                        percent > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = percent)
                                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (percent > 0f) {
                                Text(
                                    text = completedCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (percent >= 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    habitLogs: HabitWithWeeklyLogs,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitLogs.habit
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habitLogs.isCompletedToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = habit.iconEmoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${habit.currentStreak}d streak (Best: ${habit.bestStreak}d)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Habit",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (!habitLogs.isCompletedToday) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onToggle()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (habitLogs.isCompletedToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text(
                        text = if (habitLogs.isCompletedToday) "Completed" else "Mark Done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitBottomSheet(
    onDismiss: () -> Unit,
    onAddHabit: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("⭐") }

    val emojiOptions = listOf("⭐", "💧", "📚", "🏃", "🍎", "🧘", "💻", "🛌", "🎹", "🎨", "⏰", "🧼")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add New Habit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Habit Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Select Emoji Icon",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emojiOptions.take(6).forEach { emoji ->
                    EmojiSelectButton(
                        emoji = emoji,
                        isSelected = selectedEmoji == emoji,
                        onClick = { selectedEmoji = emoji }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emojiOptions.drop(6).forEach { emoji ->
                    EmojiSelectButton(
                        emoji = emoji,
                        isSelected = selectedEmoji == emoji,
                        onClick = { selectedEmoji = emoji }
                    )
                }
            }

            Button(
                onClick = { onAddHabit(name, selectedEmoji) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Create Habit")
            }
        }
    }
}

@Composable
private fun EmojiSelectButton(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}

@Preview(showBackground = true, name = "Habit Content Light")
@Composable
private fun HabitContentLightPreview() {
    StudyFlowTheme(darkTheme = false) {
        Surface {
            HabitContent(
                uiState = HabitUiState(
                    habits = listOf(
                        HabitWithWeeklyLogs(
                            habit = HabitEntity(id = 1, name = "Drink Water", iconEmoji = "💧", currentStreak = 5, bestStreak = 10),
                            logs = listOf(HabitLogEntity(id = 1, habitId = 1, dateMillis = System.currentTimeMillis())),
                            isCompletedToday = true
                        ),
                        HabitWithWeeklyLogs(
                            habit = HabitEntity(id = 2, name = "Read Book", iconEmoji = "📚", currentStreak = 0, bestStreak = 3),
                            logs = emptyList(),
                            isCompletedToday = false
                        )
                    ),
                    daysOfWeek = List(7) { System.currentTimeMillis() }
                ),
                onToggleHabit = {},
                onDeleteHabit = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Habit Content Dark")
@Composable
private fun HabitContentDarkPreview() {
    StudyFlowTheme(darkTheme = true) {
        Surface {
            HabitContent(
                uiState = HabitUiState(
                    habits = listOf(
                        HabitWithWeeklyLogs(
                            habit = HabitEntity(id = 1, name = "Drink Water", iconEmoji = "💧", currentStreak = 5, bestStreak = 10),
                            logs = listOf(HabitLogEntity(id = 1, habitId = 1, dateMillis = System.currentTimeMillis())),
                            isCompletedToday = true
                        )
                    ),
                    daysOfWeek = List(7) { System.currentTimeMillis() }
                ),
                onToggleHabit = {},
                onDeleteHabit = {}
            )
        }
    }
}
