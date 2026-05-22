package com.studyflow.app.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.components.SectionHeader
import com.studyflow.app.ui.theme.StudyFlowTheme
import java.text.SimpleDateFormat
import java.util.*

fun parseHexColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF8B5CF6) // Fallback violet
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToTasksTab: () -> Unit,
    onNavigateToHabitsTab: () -> Unit,
    onNavigateToPomodoroTab: () -> Unit,
    onNavigateToResourcesTab: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var showAddWorkspaceDialog by remember { mutableStateOf(false) }
    var showReflectionDialog by remember { mutableStateOf(false) }
    var workspaceToDelete by remember { mutableStateOf<WorkspaceEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showWorkspaceMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = uiState.selectedWorkspace?.let { "${it.iconEmoji} ${it.name}" } ?: "All Spaces",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Workspace"
                            )
                        }
                        DropdownMenu(
                            expanded = showWorkspaceMenu,
                            onDismissRequest = { showWorkspaceMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Spaces") },
                                onClick = {
                                    viewModel.selectWorkspace(null)
                                    showWorkspaceMenu = false
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                                }
                            )
                            uiState.workspaces.forEach { workspace ->
                                DropdownMenuItem(
                                    text = { Text("${workspace.iconEmoji} ${workspace.name}") },
                                    onClick = {
                                        viewModel.selectWorkspace(workspace.id)
                                        showWorkspaceMenu = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(workspace.colorHex))
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                workspaceToDelete = workspace
                                                showWorkspaceMenu = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete ${workspace.name}",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                            Divider()
                            DropdownMenuItem(
                                text = { Text("+ Create Space") },
                                onClick = {
                                    showAddWorkspaceDialog = true
                                    showWorkspaceMenu = false
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToInsights) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Insights",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else if (uiState.selectedWorkspace == null) {
            AllSpacesContent(
                uiState = uiState,
                onWorkspaceClick = { viewModel.selectWorkspace(it) },
                onCreateWorkspaceClick = { showAddWorkspaceDialog = true },
                onDeleteWorkspace = { workspaceToDelete = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            DashboardContent(
                uiState = uiState,
                onTaskToggle = viewModel::toggleTaskCompletion,
                onHabitToggle = viewModel::toggleHabit,
                onTaskClick = onNavigateToTaskDetail,
                onAddTaskClick = { onNavigateToTaskDetail(-1L) },
                onStartFocusClick = onNavigateToPomodoroTab,
                onAddResourceClick = onNavigateToResourcesTab,
                onAddNoteClick = onNavigateToNotes,
                onSeeAllTasksClick = onNavigateToTasksTab,
                onSeeAllHabitsClick = onNavigateToHabitsTab,
                onNavigateToTimeline = onNavigateToTimeline,
                onStartReflectionClick = { showReflectionDialog = true },
                onEnableRecoveryMode = viewModel::turnOnRecoveryMode,
                onUpdateStudyStateOverride = viewModel::updateStudyStateOverride,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    // Add Workspace Dialog
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

    // Delete Workspace Confirmation Dialog
    workspaceToDelete?.let { ws ->
        AlertDialog(
            onDismissRequest = { workspaceToDelete = null },
            title = { Text("Delete \"${ws.iconEmoji} ${ws.name}\"?") },
            text = {
                Text(
                    "This will permanently delete the workspace. Tasks assigned to it will remain but will no longer be linked to this space.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkspace(ws)
                        workspaceToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { workspaceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Daily Reflection Dialog
    if (showReflectionDialog) {
        var accomplishments by remember { mutableStateOf("") }
        var energyLevel by remember { mutableStateOf(3) }
        var selectedMood by remember { mutableStateOf("Focused") }
        var distraction by remember { mutableStateOf("None") }
        var productivityRating by remember { mutableStateOf(3) }
        var stressLevel by remember { mutableStateOf(3) }
        var oneWord by remember { mutableStateOf("") }
        var madeProductive by remember { mutableStateOf("") }

        val moodOptions = listOf("Focused", "Happy", "Calm", "Excited", "Stressed", "Tired")
        val distractionOptions = listOf("None", "Social Media", "Phone", "Noise", "Procrastn.", "Fatigue")
        val stressLabels = listOf("Relaxed", "Mild", "Normal", "Stressed", "Overwhelmed")

        // Hoist scroll state outside the text lambda to avoid recreation on each recompose
        val scrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showReflectionDialog = false },
            title = { Text("Daily Reflection") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = accomplishments,
                        onValueChange = { accomplishments = it },
                        label = { Text("What did you accomplish today?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Column {
                        Text("Energy Level: $energyLevel/5", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = energyLevel.toFloat(),
                            onValueChange = { energyLevel = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    Column {
                        Text("Productivity Rating: $productivityRating/5", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = productivityRating.toFloat(),
                            onValueChange = { productivityRating = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    Column {
                        val stressText = stressLabels.getOrNull(stressLevel - 1) ?: "Normal"
                        Text("Stress Level: $stressLevel/5 ($stressText)", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = stressLevel.toFloat(),
                            onValueChange = { stressLevel = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    OutlinedTextField(
                        value = oneWord,
                        onValueChange = { oneWord = it },
                        label = { Text("One word describing today") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = madeProductive,
                        onValueChange = { madeProductive = it },
                        label = { Text("What made today productive?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Column {
                        Text("Current Mood", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        // FlowRow wraps chips instead of overflowing
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            moodOptions.forEach { mood ->
                                FilterChip(
                                    selected = selectedMood == mood,
                                    onClick = { selectedMood = mood },
                                    label = { Text(mood) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Biggest Distraction", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            distractionOptions.forEach { dist ->
                                FilterChip(
                                    selected = distraction == dist,
                                    onClick = { distraction = dist },
                                    label = { Text(dist) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addReflection(
                            accomplishments,
                            energyLevel,
                            selectedMood,
                            distraction,
                            productivityRating,
                            stressLevel,
                            oneWord,
                            madeProductive
                        )
                        showReflectionDialog = false
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReflectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onTaskToggle: (Long) -> Unit,
    onHabitToggle: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onAddTaskClick: () -> Unit,
    onStartFocusClick: () -> Unit,
    onAddResourceClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onSeeAllTasksClick: () -> Unit,
    onSeeAllHabitsClick: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onStartReflectionClick: () -> Unit,
    onEnableRecoveryMode: () -> Unit,
    onUpdateStudyStateOverride: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateStr = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }
    val scrollState = rememberScrollState()

    val completedTasks = uiState.tasksCompletedTodayCount
    val totalTasks = uiState.tasksTotalTodayCount
    val momentumScore = uiState.momentumScore
    val momentumState = uiState.momentumState

    // Premium styling config based on study state
    val stateGradient = when (uiState.currentStudyState) {
        "RECOVERY" -> listOf(Color(0xFF2E7D32).copy(alpha = 0.18f), Color(0xFF1B5E20).copy(alpha = 0.02f))
        "BURNOUT" -> listOf(Color(0xFFC62828).copy(alpha = 0.18f), Color(0xFFB71C1C).copy(alpha = 0.02f))
        "PEAK" -> listOf(Color(0xFFEF6C00).copy(alpha = 0.18f), Color(0xFFE65100).copy(alpha = 0.02f))
        else -> listOf(Color(0xFF673AB7).copy(alpha = 0.18f), Color(0xFF311B92).copy(alpha = 0.02f))
    }
    val stateBorderColor = when (uiState.currentStudyState) {
        "RECOVERY" -> Color(0xFF81C784).copy(alpha = 0.25f)
        "BURNOUT" -> Color(0xFFE57373).copy(alpha = 0.25f)
        "PEAK" -> Color(0xFFFFB74D).copy(alpha = 0.25f)
        else -> Color(0xFFB39DDB).copy(alpha = 0.25f)
    }
    val stateTextColor = when (uiState.currentStudyState) {
        "RECOVERY" -> Color(0xFF81C784)
        "BURNOUT" -> Color(0xFFE57373)
        "PEAK" -> Color(0xFFFFB74D)
        else -> Color(0xFFB39DDB)
    }
    val stateLabel = when (uiState.currentStudyState) {
        "RECOVERY" -> "🌿 Recovery Mode"
        "BURNOUT" -> "⚠️ Burnout Risk"
        "PEAK" -> "🔥 Peak Momentum"
        else -> "⚡ Focus Mode"
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Greeting Header
        Column {
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = currentDateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // ONE GIANT ADAPTIVE COGNITIVE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.linearGradient(colors = stateGradient))
                    .border(width = 1.dp, color = stateBorderColor, shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left side: circular productivity ring (colored dynamically to match studyState)
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { momentumScore.toFloat() / 100f },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp,
                                color = stateTextColor,
                                trackColor = stateTextColor.copy(alpha = 0.1f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$momentumScore",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "MOMENTUM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right side: state advice & momentum details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stateLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = stateTextColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(stateTextColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = momentumState,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = stateTextColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.stateAdvice.ifEmpty { "StudyFlow OS: Optimize your cognitive states." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.correlationInsight,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Consistency Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "🔥 ${uiState.reflectionStreak} days",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = "Tasks Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "✅ $completedTasks/$totalTasks",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = stateBorderColor.copy(alpha = 0.3f))

                    // Dynamic State override controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Override Control:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val options = listOf("AUTO", "FOCUS", "RECOVERY", "BURNOUT", "PEAK")
                            options.forEach { opt ->
                                val isSelected = uiState.studyStateOverride == opt
                                val activeBg = when (opt) {
                                    "AUTO" -> MaterialTheme.colorScheme.primary
                                    "RECOVERY" -> Color(0xFF2E7D32)
                                    "BURNOUT" -> Color(0xFFC62828)
                                    "PEAK" -> Color(0xFFEF6C00)
                                    else -> Color(0xFF673AB7)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) activeBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { onUpdateStudyStateOverride(opt) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Reflection Prompt Card
        if (!uiState.hasReflectedToday) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "Daily Reflection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log accomplishments, energy, and mood to build premium weekly insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onStartReflectionClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Reflect")
                    }
                }
            }
        }

        // Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Due Today",
                value = uiState.tasksDueTodayCount.toString(),
                icon = Icons.Default.CheckCircle,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Focus Time",
                value = "${uiState.focusMinutesToday}m",
                icon = Icons.Default.Timer,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Signals",
                value = "${uiState.weeklyResourceTotal.toInt()} units",
                icon = Icons.Default.Category,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1.2f)
            )
        }

        // Quick Actions Row
        Column {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    label = "+ Task",
                    icon = Icons.Default.Assignment,
                    onClick = onAddTaskClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Focus",
                    icon = Icons.Default.PlayArrow,
                    onClick = onStartFocusClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "+ Signal",
                    icon = Icons.Default.Category,
                    onClick = onAddResourceClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "+ Note",
                    icon = Icons.Default.EditNote,
                    onClick = onAddNoteClick,
                    modifier = Modifier.weight(1f)
                )
            }   // end Row
        }   // end Column (Quick Actions)

        // Active Habits Section
        if (uiState.activeHabits.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = "Daily Habits",
                    actionLabel = "See All",
                    onActionClick = onSeeAllHabitsClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.activeHabits) { habitStatus ->
                        HabitCircle(
                            emoji = habitStatus.habit.iconEmoji,
                            name = habitStatus.habit.name,
                            isDone = habitStatus.isCompletedToday,
                            onClick = { onHabitToggle(habitStatus.habit.id) }
                        )
                    }
                }
            }
        }

        // Today's Tasks Section
        Column {
            SectionHeader(
                title = "Today's Tasks",
                actionLabel = "See All",
                onActionClick = onSeeAllTasksClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.todayTasks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks scheduled for today. Rest up!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.todayTasks) { task ->
                        DashboardTaskCard(
                            task = task,
                            onToggleComplete = { onTaskToggle(task.id) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }

        // Timeline / Upcoming Deadlines Section
        Column {
            SectionHeader(
                title = "Productivity Timeline",
                actionLabel = "Open Calendar",
                onActionClick = onNavigateToTimeline
            )
            Spacer(modifier = Modifier.height(8.dp))
            val timelineTasks = uiState.upcomingDeadlines
            if (timelineTasks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No upcoming deadlines.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        timelineTasks.forEachIndexed { index, task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskClick(task.id) },
                                verticalAlignment = Alignment.Top
                            ) {
                                // Draw visual timeline node line & dot
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (task.priority == 2) MaterialTheme.colorScheme.error
                                                else if (task.priority == 1) MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                    if (index < timelineTasks.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(40.dp)
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = task.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                task.dueDateMillis?.let { due ->
                                    val calendar = Calendar.getInstance().apply { timeInMillis = due }
                                    val timeLabel = if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0) {
                                        "All day"
                                    } else {
                                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(due))
                                    }
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HabitCircle(
    emoji: String,
    name: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DashboardTaskCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = task.category,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

sealed class WorkspaceCardItem {
    data class Workspace(val entity: WorkspaceEntity) : WorkspaceCardItem()
    object CreateNew : WorkspaceCardItem()
}

@Composable
private fun AllSpacesContent(
    uiState: DashboardUiState,
    onWorkspaceClick: (Long) -> Unit,
    onCreateWorkspaceClick: () -> Unit,
    onDeleteWorkspace: (WorkspaceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateStr = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Greeting Header
        Column {
            Text(
                text = uiState.greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = currentDateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Welcome Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                Color(0xFF8B5CF6).copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Welcome to StudyFlow OS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Workspaces let you group tasks, habits, timers, and budgets. Select a workspace below to enter your customized space, or create a new one to get organized.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section Title
        Text(
            text = "Your Workspaces",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Workspaces Grid
        val cardItems = uiState.workspaces.map { WorkspaceCardItem.Workspace(it) } + WorkspaceCardItem.CreateNew
        val chunked = cardItems.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { item ->
                        when (item) {
                            is WorkspaceCardItem.Workspace -> {
                                WorkspaceCard(
                                    workspace = item.entity,
                                    onClick = { onWorkspaceClick(item.entity.id) },
                                    onDelete = { onDeleteWorkspace(item.entity) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            WorkspaceCardItem.CreateNew -> {
                                CreateWorkspaceCard(
                                    onClick = onCreateWorkspaceClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCard(
    workspace: WorkspaceEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = remember(workspace.colorHex) { parseHexColor(workspace.colorHex) }
    val gradientBrush = remember(themeColor) {
        Brush.linearGradient(
            colors = listOf(
                themeColor.copy(alpha = 0.15f),
                themeColor.copy(alpha = 0.02f)
            )
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .border(
                    width = 1.dp,
                    color = themeColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(themeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = workspace.iconEmoji, fontSize = 20.sp)
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${workspace.name}",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = workspace.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Open Space →",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateWorkspaceCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create Space",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
