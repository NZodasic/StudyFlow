package com.studyflow.app.presentation.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.ui.components.EmptyStateView
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.components.PriorityChip
import com.studyflow.app.ui.theme.StudyFlowTheme
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowForward
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun parseHexColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF8B5CF6) // Fallback violet
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showAddWorkspaceDialog by remember { mutableStateOf(false) }

    // Detect scroll direction for FAB hide/show
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var isScrollingDown by remember { mutableStateOf(false) }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        isScrollingDown = listState.firstVisibleItemIndex > previousFirstVisibleItemIndex
        previousFirstVisibleItemIndex = listState.firstVisibleItemIndex
    }
    val showFab = !isScrollingDown || listState.firstVisibleItemIndex == 0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab && uiState.hasActiveWorkspace,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToDetail(-1L) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    expanded = listState.firstVisibleItemIndex == 0,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task") },
                    text = { Text("New Task") }
                )
            }
        }
    ) { paddingValues ->
        if (!uiState.hasActiveWorkspace) {
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
                    text = "Choose a workspace below to start viewing and managing your tasks:",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskFilter.values().forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateFilter(filter) },
                            label = {
                                Text(
                                    text = when (filter) {
                                        TaskFilter.ALL -> "All"
                                        TaskFilter.TODAY -> "Today"
                                        TaskFilter.PENDING -> "Pending"
                                        TaskFilter.COMPLETED -> "Completed"
                                        TaskFilter.HIGH_PRIORITY -> "High Priority"
                                    }
                                )
                            }
                        )
                    }
                }

                // Task List Content
                if (uiState.isLoading) {
                    LoadingIndicator(modifier = Modifier.weight(1f))
                } else if (uiState.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            message = "No Tasks Found\nTry searching for something else or add a new task to get started."
                        )
                    }
                } else {
                    TaskListContent(
                        tasks = uiState.tasks,
                        workspaces = uiState.workspaces,
                        listState = listState,
                        onTaskClick = onNavigateToDetail,
                        onToggleComplete = viewModel::toggleTaskCompletion,
                        onDelete = { task ->
                            viewModel.deleteTask(task)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListContent(
    tasks: List<TaskEntity>,
    workspaces: List<WorkspaceEntity>,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    onTaskClick: (Long) -> Unit,
    onToggleComplete: (Long) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(task)
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                },
                content = {
                    TaskCard(
                        task = task,
                        workspaces = workspaces,
                        onClick = { onTaskClick(task.id) },
                        onToggleComplete = { onToggleComplete(task.id) },
                        onDelete = { onDelete(task) }
                    )
                },
                enableDismissFromStartToEnd = false
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    workspaces: List<WorkspaceEntity>,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left priority indicator bar
            val indicatorColor = when (task.priority) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(indicatorColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleComplete()
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val workspace = workspaces.find { it.id == task.workspaceId }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityChip(priority = task.priority)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        workspace?.let { ws ->
                            val wsColor = try {
                                Color(android.graphics.Color.parseColor(ws.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(wsColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(wsColor)
                                )
                                Text(
                                    text = "${ws.iconEmoji} ${ws.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = wsColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        task.dueDateMillis?.let { due ->
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = formatDueDate(due),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (due < System.currentTimeMillis() && !task.isCompleted) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }

            // Visible delete button on the card
            if (onDelete != null) {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatDueDate(timeMillis: Long): String {
    val date = Date(timeMillis)
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return "Due: ${formatter.format(date)}"
}

@Preview(showBackground = true, name = "Task List Light")
@Composable
private fun TaskListPreviewLight() {
    StudyFlowTheme(darkTheme = false) {
        Surface {
            TaskListContent(
                tasks = listOf(
                    TaskEntity(id = 1, title = "Finish Homework", description = "Complete math section 4.2", priority = 2, category = "Math", dueDateMillis = System.currentTimeMillis() + 86400000),
                    TaskEntity(id = 2, title = "Clean Room", description = "", priority = 0, category = "Personal", isCompleted = true)
                ),
                workspaces = emptyList(),
                onTaskClick = {},
                onToggleComplete = {},
                onDelete = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Task List Dark")
@Composable
private fun TaskListPreviewDark() {
    StudyFlowTheme(darkTheme = true) {
        Surface {
            TaskListContent(
                tasks = listOf(
                    TaskEntity(id = 1, title = "Finish Homework", description = "Complete math section 4.2", priority = 2, category = "Math", dueDateMillis = System.currentTimeMillis() + 86400000),
                    TaskEntity(id = 2, title = "Clean Room", description = "", priority = 0, category = "Personal", isCompleted = true)
                ),
                workspaces = emptyList(),
                onTaskClick = {},
                onToggleComplete = {},
                onDelete = {}
            )
        }
    }
}
