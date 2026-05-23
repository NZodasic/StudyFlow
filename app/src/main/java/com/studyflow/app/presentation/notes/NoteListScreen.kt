package com.studyflow.app.presentation.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.app.data.local.entity.NoteEntity
import com.studyflow.app.data.local.entity.WorkspaceEntity
import com.studyflow.app.ui.components.EmptyStateView
import com.studyflow.app.ui.components.StudyFlowTopBar
import com.studyflow.app.ui.theme.StudyFlowTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteListScreen(
    viewModel: NoteViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    NoteListContent(
        uiState = uiState,
        onSearchChange = viewModel::updateSearchQuery,
        onSubjectSelect = viewModel::selectSubject,
        onQuickBrainDump = viewModel::quickBrainDump,
        onConvertToTask = viewModel::convertToTask,
        onPinToggle = viewModel::togglePin,
        onDelete = viewModel::deleteNote,
        onNoteClick = onNavigateToDetail,
        onAddNoteClick = { onNavigateToDetail(-1L) },
        onClearSuccess = viewModel::clearSuccessMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListContent(
    uiState: NoteUiState,
    onSearchChange: (String) -> Unit,
    onSubjectSelect: (String) -> Unit,
    onQuickBrainDump: (String) -> Unit,
    onConvertToTask: (NoteEntity) -> Unit,
    onPinToggle: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: () -> Unit,
    onClearSuccess: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearSuccess()
        }
    }

    Scaffold(
        topBar = {
            StudyFlowTopBar(title = "Study Notes")
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNoteClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            TextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = "Search title, subject, or content...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            // Subject Folder Chips (Horizontal Scroll)
            if (uiState.subjects.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(uiState.subjects) { subject ->
                        val isSelected = uiState.selectedSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubjectSelect(subject) },
                            label = { Text(text = subject, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Quick Brain Dump Panel
            var brainDumpText by remember { mutableStateOf("") }
            val isDark = isSystemInDarkTheme()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.6f else 0.85f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.6f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.5f),
                                    Color.White.copy(alpha = if (isDark) 0.05f else 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Quick Brain Dump",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp).size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = brainDumpText,
                            onValueChange = { brainDumpText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (brainDumpText.isEmpty()) {
                                    Text(
                                        text = "Capture a quick brain dump...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                        IconButton(
                            onClick = {
                                if (brainDumpText.isNotBlank()) {
                                    onQuickBrainDump(brainDumpText)
                                    brainDumpText = ""
                                }
                            },
                            enabled = brainDumpText.isNotBlank(),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Save Brain Dump",
                                tint = if (brainDumpText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pinned Notes Section
                if (uiState.pinnedNotes.isNotEmpty() && uiState.searchQuery.isEmpty() && uiState.selectedSubject == "All") {
                    item {
                        Text(
                            text = "Pinned Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.pinnedNotes, key = { it.id }) { note ->
                                PinnedNoteCard(
                                    note = note,
                                    workspaces = uiState.workspaces,
                                    onClick = { onNoteClick(note.id) },
                                    onConvertToTask = { onConvertToTask(note) },
                                    onPinToggle = { onPinToggle(note) },
                                    onDelete = { onDelete(note) }
                                )
                            }
                        }
                    }
                }

                // All Notes Title
                item {
                    val titleText = when {
                        uiState.searchQuery.isNotEmpty() -> "Search Results"
                        uiState.selectedSubject != "All" -> "Notes in '${uiState.selectedSubject}'"
                        else -> "All Notes"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // List Items
                if (uiState.notes.isEmpty()) {
                    item {
                        EmptyStateView(
                            message = "No notes found. Create your first subject note or capture a brain dump above!",
                            icon = Icons.Default.List
                        )
                    }
                } else {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteItemRow(
                            note = note,
                            workspaces = uiState.workspaces,
                            onClick = { onNoteClick(note.id) },
                            onConvertToTask = { onConvertToTask(note) },
                            onPinToggle = { onPinToggle(note) },
                            onDelete = { onDelete(note) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedNoteCard(
    note: NoteEntity,
    workspaces: List<WorkspaceEntity> = emptyList(),
    onClick: () -> Unit,
    onConvertToTask: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.22f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.85f else 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.5f),
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.25f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val workspace = workspaces.find { it.id == note.workspaceId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (note.subject.isNotEmpty()) {
                                Text(
                                    text = note.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Note options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Convert to Task") },
                                    onClick = {
                                        onConvertToTask()
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Unpin Note") },
                                    onClick = {
                                        onPinToggle()
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        onDelete()
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (note.title.isNotEmpty()) note.title else "Untitled Note",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "📌 Pinned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemRow(
    note: NoteEntity,
    workspaces: List<WorkspaceEntity> = emptyList(),
    onClick: () -> Unit,
    onConvertToTask: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val formattedDate = formatter.format(Date(note.updatedAtMillis))
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.5f else 0.75f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.2f else 0.4f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.35f),
                            Color.White.copy(alpha = if (isDark) 0.02f else 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorColor = if (note.isPinned) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(indicatorColor)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val workspace = workspaces.find { it.id == note.workspaceId }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (note.subject.isNotEmpty()) {
                                Text(
                                    text = note.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
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
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
                            if (note.isPinned) {
                                Text(
                                    text = "📌 Pinned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (note.title.isNotEmpty()) note.title else "Untitled Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (note.content.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Edited: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Convert to Task") },
                                onClick = {
                                    onConvertToTask()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin" else "Pin Note") },
                                onClick = {
                                    onPinToggle()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDelete()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteListContentLightPreview() {
    StudyFlowTheme(darkTheme = false) {
        NoteListContent(
            uiState = NoteUiState(
                notes = listOf(
                    NoteEntity(id = 1, title = "Linear Algebra Notes", content = "Matrices and Determinants formulas", subject = "Math", isPinned = true),
                    NoteEntity(id = 2, title = "Android Architecture", content = "Using Clean MVVM patterns", subject = "Computer Science"),
                    NoteEntity(id = 3, title = "Shopping list", content = "Milk, Bread, Fruits", subject = "General")
                ),
                pinnedNotes = listOf(
                    NoteEntity(id = 1, title = "Linear Algebra Notes", content = "Matrices and Determinants formulas", subject = "Math", isPinned = true)
                ),
                subjects = listOf("All", "Math", "Computer Science", "General")
            ),
            onSearchChange = {},
            onSubjectSelect = {},
            onQuickBrainDump = {},
            onConvertToTask = {},
            onPinToggle = {},
            onDelete = {},
            onNoteClick = {},
            onAddNoteClick = {},
            onClearSuccess = {}
        )
    }
}
