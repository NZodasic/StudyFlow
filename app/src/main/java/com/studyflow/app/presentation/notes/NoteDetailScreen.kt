package com.studyflow.app.presentation.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.app.data.local.entity.NoteEntity
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.components.StudyFlowTopBar
import com.studyflow.app.ui.theme.StudyFlowTheme

@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NoteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNote: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(noteId) {
        viewModel.selectNote(noteId)
    }

    // Auto-save when user presses system back button
    BackHandler {
        viewModel.saveCurrentNoteImmediately()
        onNavigateBack()
    }

    // Auto-save when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveCurrentNoteImmediately()
        }
    }

    val currentNote = uiState.currentNote

    if (currentNote == null) {
        LoadingIndicator()
    } else {
        NoteDetailContent(
            note = currentNote,
            backlinks = uiState.backlinks,
            successMessage = uiState.successMessage,
            onTitleChange = { title ->
                viewModel.updateNoteDetails(title, currentNote.content, currentNote.subject)
            },
            onContentChange = { content ->
                viewModel.updateNoteDetails(currentNote.title, content, currentNote.subject)
            },
            onSubjectChange = { subject ->
                viewModel.updateNoteDetails(currentNote.title, currentNote.content, subject)
            },
            onPinToggle = viewModel::toggleCurrentNotePin,
            onConvertToTask = viewModel::convertToTask,
            onWikiLinkClick = { title ->
                // Save current note first, then navigate to linked note (NavGraph resolves the ID)
                viewModel.saveCurrentNoteImmediately()
                viewModel.findOrCreateNoteByTitle(title) { newId ->
                    onNavigateToNote(newId)
                }
            },
            onBacklinkClick = { linkedNoteId ->
                viewModel.saveCurrentNoteImmediately()
                onNavigateToNote(linkedNoteId)
            },
            onClearSuccess = viewModel::clearSuccessMessage,
            onBackClick = {
                viewModel.saveCurrentNoteImmediately()
                onNavigateBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailContent(
    note: NoteEntity,
    backlinks: List<NoteEntity> = emptyList(),
    successMessage: String? = null,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onPinToggle: () -> Unit,
    onConvertToTask: (NoteEntity) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onBacklinkClick: (Long) -> Unit,
    onClearSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val characterCount = note.content.length
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearSuccess()
        }
    }

    Scaffold(
        topBar = {
            StudyFlowTopBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onConvertToTask(note) }) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Convert to Task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onPinToggle) {
                        Text(
                            text = if (note.isPinned) "📌" else "📍",
                            fontSize = 20.sp
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject: ${if (note.subject.isNotEmpty()) note.subject else "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$characterCount characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Edit vs Preview Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Edit", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Preview", fontWeight = FontWeight.Bold) }
                )
            }

            // Subject Tag Field (only in edit mode)
            if (selectedTab == 0) {
                val isDark = isSystemInDarkTheme()
                TextField(
                    value = note.subject,
                    onValueChange = onSubjectChange,
                    placeholder = {
                        Text(
                            text = "Add subject tag...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.25f),
                                    Color.White.copy(alpha = if (isDark) 0.02f else 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.5f else 0.75f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.3f else 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            } else {
                if (note.subject.isNotEmpty()) {
                    Text(
                        text = note.subject,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Title (Static in Preview, TextField in Edit)
            if (selectedTab == 0) {
                BasicTextField(
                    value = note.title,
                    onValueChange = onTitleChange,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (note.title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    singleLine = true
                )
            } else {
                Text(
                    text = if (note.title.isNotEmpty()) note.title else "Untitled Note",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            // Content Editor / Link Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    BasicTextField(
                        value = note.content,
                        onValueChange = onContentChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (note.content.isEmpty()) {
                                Text(
                                    text = "Start writing your notes here... Use [[Note Title]] to link pages.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (note.content.isEmpty()) {
                            Text(
                                text = "No content written yet.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            )
                        } else {
                            val content = note.content
                            val annotatedString = buildAnnotatedString {
                                val pattern = Regex("\\[\\[([^\\]]+)\\]\\]")
                                var lastIndex = 0
                                pattern.findAll(content).forEach { matchResult ->
                                    val start = matchResult.range.first
                                    val end = matchResult.range.last + 1
                                    val title = matchResult.groupValues[1]

                                    append(content.substring(lastIndex, start))

                                    pushStringAnnotation(tag = "WIKI_LINK", annotation = title)
                                    withStyle(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append(title)
                                    }
                                    pop()
                                    lastIndex = end
                                }
                                if (lastIndex < content.length) {
                                    append(content.substring(lastIndex))
                                }
                            }

                            ClickableText(
                                text = annotatedString,
                                onClick = { offset ->
                                    annotatedString.getStringAnnotations(tag = "WIKI_LINK", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            onWikiLinkClick(annotation.item)
                                        }
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 24.sp
                                )
                            )
                        }
                    }
                }
            }

            // Backlinks Section (only shown if backlinks exist)
            if (backlinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Backlinks (Notes linking here)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    items(backlinks) { backlink ->
                        val isDark = isSystemInDarkTheme()
                        Card(
                            onClick = { onBacklinkClick(backlink.id) },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            modifier = Modifier.width(160.dp)
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
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = backlink.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (backlink.subject.isNotEmpty()) {
                                        Text(
                                            text = backlink.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
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
