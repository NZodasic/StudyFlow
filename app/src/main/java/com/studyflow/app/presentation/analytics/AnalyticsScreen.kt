package com.studyflow.app.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.app.data.local.dao.ResourceCategoryTotal
import com.studyflow.app.data.local.entity.HabitEntity
import com.studyflow.app.ui.components.EmptyStateView
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.components.StudyFlowTopBar
import com.studyflow.app.ui.theme.StudyFlowTheme
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tasks", "Habits", "Pomodoro", "Signals")

    Scaffold(
        topBar = {
            StudyFlowTopBar(title = "Productivity Analytics")
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            val isDark = isSystemInDarkTheme()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Obsidian Glassmorphic Tab Selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF1E1E24).copy(alpha = 0.6f),
                                        Color(0xFF0F0F12).copy(alpha = 0.8f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFF2F4F7).copy(alpha = 0.8f),
                                        Color(0xFFE4E7EC).copy(alpha = 0.6f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                                } else {
                                    listOf(Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.02f))
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.verticalGradient(
                                                colors = if (isDark) {
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    )
                                                } else {
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                                    )
                                                }
                                            )
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = if (isSelected) {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                                )
                                            )
                                        } else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent)),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> TasksTabContent(uiState = uiState)
                    1 -> HabitsTabContent(uiState = uiState)
                    2 -> PomodoroTabContent(uiState = uiState)
                    3 -> ResourcesTabContent(uiState = uiState)
                }
            }
        }
    }
}

// 1. Tasks Tab Content
@Composable
fun TasksTabContent(uiState: AnalyticsUiState) {
    val isDark = isSystemInDarkTheme()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsStatCard(
                    title = "Completed",
                    value = "${uiState.completedTaskCount} tasks",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Completion Rate",
                    value = String.format(Locale.getDefault(), "%.1f%%", uiState.taskCompletionRate * 100),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Weekly Task Completions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AnalyticsBarChart(data = uiState.weeklyTaskCompletions)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Task Category Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.taskCategoryCounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No category data available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        DonutChart(
                            categoryValues = uiState.taskCategoryCounts.mapValues { it.value.toDouble() }
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// 2. Habits Tab Content
@Composable
fun HabitsTabContent(uiState: AnalyticsUiState) {
    val isDark = isSystemInDarkTheme()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "30-Day Completion Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HabitHeatmapGrid(heatmapData = uiState.habitHeatmapData)
                }
            }
        }

        item {
            Text(
                text = "Habit Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.habits.isEmpty()) {
            item {
                EmptyStateView(
                    message = "Track active habits to build streaks and view completion rates.",
                    icon = Icons.Default.Star
                )
            }
        } else {
            items(uiState.habits) { habit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                        )
                                    } else {
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.06f else 0.12f),
                                        Color.White.copy(alpha = if (isDark) 0.02f else 0.04f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = habit.iconEmoji, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔥 ${habit.currentStreak}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🏆 ${habit.bestStreak}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Best",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// 3. Pomodoro Tab Content
@Composable
fun PomodoroTabContent(uiState: AnalyticsUiState) {
    val isDark = isSystemInDarkTheme()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsStatCard(
                    title = "Hours Focused",
                    value = String.format(Locale.getDefault(), "%.1fh", uiState.totalFocusHours),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Daily Average",
                    value = String.format(Locale.getDefault(), "%.0f min", uiState.dailyAverageFocusMinutes),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Focus Sessions (Current Week)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AnalyticsBarChart(data = uiState.weeklyPomodoroSessions)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// 4. Performance Signals Tab Content
@Composable
fun ResourcesTabContent(uiState: AnalyticsUiState) {
    val isDark = isSystemInDarkTheme()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsStatCard(
                    title = "Monthly Signals",
                    value = String.format(Locale.getDefault(), "%.1f units", uiState.monthlyTotalResources),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    title = "Top Category",
                    value = uiState.biggestResourceCategory.ifBlank { "None" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Monthly Signal Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ResourceTrendLineChart(points = uiState.last6MonthsResourceTrend)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Signal Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.resourceCategoryTotals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No signals logged this month.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        DonutChart(
                            categoryValues = uiState.resourceCategoryTotals.associate { it.category to it.total }
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Subcomponents: Stat Card
@Composable
fun AnalyticsStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                            Color.White.copy(alpha = if (isDark) 0.03f else 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Subcomponents: Bar Chart
@Composable
fun AnalyticsBarChart(data: List<Int>) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)

    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxVal = data.maxOrNull() ?: 0
            val ceiling = if (maxVal <= 0) 5f else maxVal.toFloat() * 1.15f

            val availableWidth = size.width
            val availableHeight = size.height
            val barWidth = 20.dp.toPx()
            val spacing = (availableWidth - (barWidth * 7)) / 8

            // Draw tracks and bars
            for (i in 0 until 7) {
                val value = data.getOrElse(i) { 0 }.toFloat()
                val heightRatio = value / ceiling
                val barHeight = availableHeight * heightRatio
                val x = spacing + i * (barWidth + spacing)

                // Track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, availableHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Bar
                if (barHeight > 0f) {
                    val y = availableHeight - barHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                secondaryColor,
                                primaryColor
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = labelColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}

// Subcomponents: Line Chart
@Composable
fun ResourceTrendLineChart(points: List<Pair<String, Double>>) {
    if (points.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxAmount = points.maxOfOrNull { it.second } ?: 0.0
            val ceiling = if (maxAmount <= 0.0) 100.0 else maxAmount * 1.15

            val availableWidth = size.width
            val availableHeight = size.height

            val gridLines = 3
            for (i in 0 until gridLines) {
                val y = (availableHeight / gridLines) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(availableWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val steps = points.size
            val xGap = availableWidth / (steps - 1).coerceAtLeast(1)

            val coordinates = points.mapIndexed { idx, point ->
                val x = idx * xGap
                val y = (availableHeight - (availableHeight * (point.second / ceiling))).toFloat()
                Offset(x, y)
            }

            // Draw gradient fill under the line
            if (coordinates.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(coordinates.first().x, availableHeight)
                    lineTo(coordinates.first().x, coordinates.first().y)
                    for (i in 1 until coordinates.size) {
                        lineTo(coordinates[i].x, coordinates[i].y)
                    }
                    lineTo(coordinates.last().x, availableHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            secondaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Draw line connecting points
            val linePath = Path().apply {
                if (coordinates.isNotEmpty()) {
                    moveTo(coordinates.first().x, coordinates.first().y)
                    for (i in 1 until coordinates.size) {
                        lineTo(coordinates[i].x, coordinates[i].y)
                    }
                }
            }

            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor, secondaryColor)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw halo circles on points
            coordinates.forEach { coord ->
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 8.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 4.dp.toPx(),
                    center = coord
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = coord
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        points.forEach { point ->
            Text(
                text = point.first,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = labelColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(42.dp)
            )
        }
    }
}

// Subcomponents: Heatmap Grid
@Composable
fun HabitHeatmapGrid(heatmapData: Map<Long, Int>) {
    // Generate dates for the last 30 days
    val dates = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 0 until 30) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list.reverse()
        list
    }

    val gridRows = 5
    val gridCols = 6
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until gridRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until gridCols) {
                    val idx = row * gridCols + col
                    val dateMillis = dates.getOrNull(idx)

                    val count = if (dateMillis != null) heatmapData[dateMillis] ?: 0 else 0
                    val isDark = isSystemInDarkTheme()
                    val emptyColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

                    val brush = when {
                        count <= 0 -> Brush.linearGradient(listOf(emptyColor, emptyColor))
                        count == 1 -> Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.3f), primaryColor.copy(alpha = 0.3f)))
                        count == 2 -> Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.6f), secondaryColor.copy(alpha = 0.4f)))
                        else -> Brush.linearGradient(listOf(primaryColor, secondaryColor))
                    }

                    val borderModifier = if (count > 0) {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.5f), secondaryColor.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(6.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(brush = brush, shape = RoundedCornerShape(6.dp))
                            .then(borderModifier)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isDark = isSystemInDarkTheme()
            val emptyColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

            Text(text = "Less", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.size(14.dp).background(emptyColor, shape = RoundedCornerShape(3.dp)))
            Box(modifier = Modifier.size(14.dp).background(primaryColor.copy(alpha = 0.3f), shape = RoundedCornerShape(3.dp)).border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(3.dp)))
            Box(modifier = Modifier.size(14.dp).background(Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.6f), secondaryColor.copy(alpha = 0.4f))), shape = RoundedCornerShape(3.dp)).border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(3.dp)))
            Box(modifier = Modifier.size(14.dp).background(Brush.linearGradient(listOf(primaryColor, secondaryColor)), shape = RoundedCornerShape(3.dp)).border(1.dp, primaryColor, RoundedCornerShape(3.dp)))
            Text(text = "More", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Subcomponents: Donut Chart
@Composable
fun DonutChart(categoryValues: Map<String, Double>) {
    val total = categoryValues.values.sum()
    if (total <= 0.0) return

    val colors = listOf(
        Color(0xFF00D8F6), // Neon Cyan
        Color(0xFF9061F9), // Neon Violet
        Color(0xFFFFB703), // Neon Amber
        Color(0xFFFF5E5E), // Coral Red
        Color(0xFF10B981), // Emerald Green
        Color(0xFF4FACFE), // Neon Blue
        Color(0xFFF35588)  // Neon Pink
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isDark = isSystemInDarkTheme()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                // Draw single neutral background track
                drawArc(
                    color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx())
                )

                var startAngle = -90f
                categoryValues.entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / total) * 360f).toFloat()
                    val color = colors.getOrElse(index) { colors.last() }

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = if (categoryValues.size > 1) (sweepAngle - 4f).coerceAtLeast(1f) else sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // Legend details
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            categoryValues.entries.forEachIndexed { index, entry ->
                val color = colors.getOrElse(index) { colors.last() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = color, shape = RoundedCornerShape(3.dp))
                            .border(width = 1.dp, color = color.copy(alpha = 0.5f), shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key} (${String.format(Locale.getDefault(), "%.0f%%", (entry.value / total) * 100)})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsScreenLightPreview() {
    StudyFlowTheme(darkTheme = false) {
        TasksTabContent(
            uiState = AnalyticsUiState(
                completedTaskCount = 8,
                totalTaskCount = 10,
                taskCompletionRate = 0.8f,
                taskCategoryCounts = mapOf("Math" to 3, "CS" to 4, "General" to 1),
                weeklyTaskCompletions = listOf(1, 2, 0, 3, 1, 1, 0)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsScreenDarkPreview() {
    StudyFlowTheme(darkTheme = true) {
        HabitsTabContent(
            uiState = AnalyticsUiState(
                habits = listOf(
                    HabitEntity(id = 1, name = "Coding", iconEmoji = "💻", currentStreak = 5, bestStreak = 12),
                    HabitEntity(id = 2, name = "Reading", iconEmoji = "📚", currentStreak = 0, bestStreak = 3)
                ),
                habitHeatmapData = mapOf()
            )
        )
    }
}
