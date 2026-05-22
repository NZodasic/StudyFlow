package com.studyflow.app.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.local.entity.UserSettingsEntity
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.theme.StudyFlowTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showClearTasksDialog by remember { mutableStateOf(false) }
    var showClearResourcesDialog by remember { mutableStateOf(false) }

    if (showClearTasksDialog) {
        ConfirmDialog(
            title = "Clear All Tasks",
            body = "Are you sure you want to delete all tasks? This action cannot be undone.",
            confirmLabel = "Delete All",
            onConfirm = {
                showClearTasksDialog = false
                viewModel.clearAllTasks {
                    Toast.makeText(context, "All tasks cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onCancel = { showClearTasksDialog = false }
        )
    }

    if (showClearResourcesDialog) {
        ConfirmDialog(
            title = "Clear All Signals",
            body = "Are you sure you want to delete all logged performance signals? This action cannot be undone.",
            confirmLabel = "Delete All",
            onConfirm = {
                showClearResourcesDialog = false
                viewModel.clearAllResources {
                    Toast.makeText(context, "All logged signals cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onCancel = { showClearResourcesDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else {
            SettingsContent(
                userSettings = uiState.userSettings ?: UserSettingsEntity(),
                onThemeToggle = viewModel::updateTheme,
                onRecoveryThemeToggle = viewModel::updateRecoveryMode,
                onFocusSliderChange = viewModel::updatePomodoroDuration,
                onShortBreakSliderChange = viewModel::updateShortBreak,
                onLongBreakSliderChange = viewModel::updateLongBreak,
                onClearTasksClick = { showClearTasksDialog = true },
                onClearResourcesClick = { showClearResourcesDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun SettingsContent(
    userSettings: UserSettingsEntity,
    onThemeToggle: (Boolean) -> Unit,
    onRecoveryThemeToggle: (Boolean) -> Unit,
    onFocusSliderChange: (Int) -> Unit,
    onShortBreakSliderChange: (Int) -> Unit,
    onLongBreakSliderChange: (Int) -> Unit,
    onClearTasksClick: () -> Unit,
    onClearResourcesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appearance Section
        Text(
            text = "Appearance & Calm Mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Use a darker palette to save battery and ease strain",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userSettings.isDarkTheme,
                        onCheckedChange = onThemeToggle
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recovery Mode Theme",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Apply a calming Sage & Lavender theme to lower stress levels",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userSettings.isRecoveryMode,
                        onCheckedChange = onRecoveryThemeToggle
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Pomodoro Section
        Text(
            text = "Pomodoro Timer Customization",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Focus Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Focus Duration", style = MaterialTheme.typography.bodyLarge)
                        Text("${userSettings.pomodoroDurationMinutes} min", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = userSettings.pomodoroDurationMinutes.toFloat(),
                        onValueChange = { onFocusSliderChange(it.roundToInt()) },
                        valueRange = 15f..60f,
                        steps = 8
                    )
                }

                // Short Break Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Short Break", style = MaterialTheme.typography.bodyLarge)
                        Text("${userSettings.shortBreakMinutes} min", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = userSettings.shortBreakMinutes.toFloat(),
                        onValueChange = { onShortBreakSliderChange(it.roundToInt()) },
                        valueRange = 3f..10f,
                        steps = 7
                    )
                }

                // Long Break Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Long Break", style = MaterialTheme.typography.bodyLarge)
                        Text("${userSettings.longBreakMinutes} min", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = userSettings.longBreakMinutes.toFloat(),
                        onValueChange = { onLongBreakSliderChange(it.roundToInt()) },
                        valueRange = 10f..30f,
                        steps = 4
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Data Management Section
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClearTasksClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Tasks")
                }

                Button(
                    onClick = onClearResourcesClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Signals")
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // About Section
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("StudyFlow App", style = MaterialTheme.typography.bodyLarge)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Developer: Nguyen Vo Chi Dung", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen Light")
@Composable
private fun SettingsContentLightPreview() {
    StudyFlowTheme(darkTheme = false) {
        Surface {
            SettingsContent(
                userSettings = UserSettingsEntity(
                    isDarkTheme = false,
                    isRecoveryMode = false,
                    pomodoroDurationMinutes = 25,
                    shortBreakMinutes = 5,
                    longBreakMinutes = 15
                ),
                onThemeToggle = {},
                onRecoveryThemeToggle = {},
                onFocusSliderChange = {},
                onShortBreakSliderChange = {},
                onLongBreakSliderChange = {},
                onClearTasksClick = {},
                onClearResourcesClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen Dark")
@Composable
private fun SettingsContentDarkPreview() {
    StudyFlowTheme(darkTheme = true) {
        Surface {
            SettingsContent(
                userSettings = UserSettingsEntity(
                    isDarkTheme = true,
                    isRecoveryMode = false,
                    pomodoroDurationMinutes = 30,
                    shortBreakMinutes = 7,
                    longBreakMinutes = 20
                ),
                onThemeToggle = {},
                onRecoveryThemeToggle = {},
                onFocusSliderChange = {},
                onShortBreakSliderChange = {},
                onLongBreakSliderChange = {},
                onClearTasksClick = {},
                onClearResourcesClick = {}
            )
        }
    }
}
