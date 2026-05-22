package com.studyflow.app.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Task Completions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AnalyticsBarChart(data = uiState.weeklyTaskCompletions)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Task Category Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "30-Day Completion Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                    iconEmoji = "⭐"
                )
            }
        } else {
            items(uiState.habits) { habit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = habit.iconEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔥 ${habit.currentStreak}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🏆 ${habit.bestStreak}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Best",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Focus Sessions (Current Week)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                    value = uiState.biggestResourceCategory,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Signal Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ResourceTrendLineChart(points = uiState.last6MonthsResourceTrend)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Signal Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxVal = data.maxOrNull() ?: 0
            val ceiling = if (maxVal <= 0) 5f else maxVal.toFloat() * 1.1f

            val availableWidth = size.width
            val availableHeight = size.height
            val barWidth = 20.dp.toPx()
            val spacing = (availableWidth - (barWidth * 7)) / 8

            // Grid lines
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

            // Draw Bars
            for (i in 0 until 7) {
                val value = data.getOrElse(i) { 0 }.toFloat()
                val heightRatio = value / ceiling
                val barHeight = availableHeight * heightRatio
                val x = spacing + i * (barWidth + spacing)
                val y = availableHeight - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = labelColor,
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

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
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

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
                        colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
            }

            // Draw circles on points
            coordinates.forEach { coord ->
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = coord
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        points.forEach { point ->
            Text(
                text = point.first,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = labelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp)
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
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

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
                    val color = when {
                        count <= 0 -> emptyColor
                        count == 1 -> primaryColor.copy(alpha = 0.3f)
                        count == 2 -> primaryColor.copy(alpha = 0.6f)
                        else -> primaryColor
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color = color, shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Less", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.size(12.dp).background(emptyColor, shape = RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.size(12.dp).background(primaryColor.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.size(12.dp).background(primaryColor.copy(alpha = 0.6f), shape = RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.size(12.dp).background(primaryColor, shape = RoundedCornerShape(2.dp)))
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
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline,
        Color(0xFF7A6A85), // Desaturated Amethyst
        Color(0xFF5D7A75)  // Desaturated Steel Teal
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                var startAngle = -90f
                categoryValues.entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / total) * 360f).toFloat()
                    val color = colors.getOrElse(index) { colors.last() }

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
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
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = color, shape = RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${entry.key} (${String.format(Locale.getDefault(), "%.0f%%", (entry.value / total) * 100)})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
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
