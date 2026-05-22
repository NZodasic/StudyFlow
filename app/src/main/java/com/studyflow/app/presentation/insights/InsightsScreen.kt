package com.studyflow.app.presentation.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import com.studyflow.app.ui.components.LoadingIndicator
import com.studyflow.app.ui.theme.StudyFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Productivity Insights",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
        } else {
            InsightsContent(
                uiState = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun InsightsContent(
    uiState: InsightsUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ─── STUDY DNA CARD ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.studyDna != null,
            enter = fadeIn() + expandVertically()
        ) {
            uiState.studyDna?.let { dna ->
                StudyDnaCard(profile = dna)
            }
        }

        // ─── CORRELATION INSIGHTS ─────────────────────────────────────────
        Text(
            text = "Daily Reflection Correlations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        uiState.correlations.forEach { correlation ->
            InsightCorrelationCard(correlation = correlation)
        }

        // ─── STATS CARDS ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightMiniStatCard(
                title = "Avg Energy",
                value = String.format("%.1f/5.0", uiState.averageEnergy),
                icon = Icons.Default.ElectricBolt,
                color = Color(0xFFFFB703),
                modifier = Modifier.weight(1f)
            )
            InsightMiniStatCard(
                title = "Avg Productivity",
                value = String.format("%.1f/5.0", uiState.averageProductivity),
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightMiniStatCard(
                title = "Completed Tasks",
                value = "${uiState.weeklyTasksCompleted} tasks",
                icon = Icons.Default.Star,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            InsightMiniStatCard(
                title = "Focus Hours",
                value = String.format("%.1fh", uiState.weeklyFocusHours),
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // ─── ENERGY BY DAY CHART ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Energy Level Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                WeeklyEnergyBarChart(energyLevels = uiState.energyByDayOfWeek)
            }
        }

        // ─── MOOD DISTRIBUTION ────────────────────────────────────────────
        if (uiState.moodStats.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Mood Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.moodStats.forEach { moodStat ->
                        MoodDistributionRow(moodStat = moodStat)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── STUDY DNA CARD ───────────────────────────────────────────────────────────

@Composable
private fun StudyDnaCard(
    profile: StudyDnaProfile,
    modifier: Modifier = Modifier
) {
    val gradientColors = when (profile.archetype) {
        "Night Owl Thinker"          -> listOf(Color(0xFF1A0B2E), Color(0xFF3D1F8C))
        "Sprint Learner"             -> listOf(Color(0xFF1A1200), Color(0xFF7C4E00))
        "Deep Focus Specialist"      -> listOf(Color(0xFF001A2E), Color(0xFF004C8C))
        "Recovery-Dependent Learner" -> listOf(Color(0xFF0A1F12), Color(0xFF145C30))
        else                         -> listOf(Color(0xFF0F1628), Color(0xFF1E3A5F))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = profile.emoji, fontSize = 36.sp)
                    Column {
                        Text(
                            text = "Your Study DNA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = profile.archetype,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Description
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    lineHeight = 22.sp
                )

                // Trait Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.traits) { trait ->
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = trait,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Recommendations
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Personalized Recommendations",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                    profile.recommendations.forEach { rec ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "→",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── CORRELATION CARD ─────────────────────────────────────────────────────────

@Composable
private fun InsightCorrelationCard(
    correlation: InsightCorrelation,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when (correlation.impactLevel) {
        "High Positive"   -> Color(0xFF10B981)
        "Needs Attention" -> Color(0xFFEF4444)
        else              -> Color(0xFF6B7280)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = correlation.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = correlation.impactLevel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = correlation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─── MINI STAT CARD ──────────────────────────────────────────────────────────

@Composable
private fun InsightMiniStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── ENERGY BAR CHART ────────────────────────────────────────────────────────

@Composable
private fun WeeklyEnergyBarChart(
    energyLevels: List<Float>
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val level = energyLevels.getOrNull(index) ?: 0f
            val percentage = level / 5.0f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (level > 0f) String.format("%.1f", level) else "-",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((80 * percentage).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFB703),
                                    Color(0xFFFFB703).copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─── MOOD ROW ────────────────────────────────────────────────────────────────

@Composable
private fun MoodDistributionRow(
    moodStat: MoodStat
) {
    val emoji = when (moodStat.mood.lowercase()) {
        "focused", "focus"     -> "🎯"
        "happy", "joy"         -> "😊"
        "calm", "relaxed"      -> "🧘"
        "excited", "motivated" -> "🚀"
        "stressed", "anxious"  -> "😰"
        "tired", "sleepy"      -> "😴"
        else                   -> "⭐"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = moodStat.mood,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LinearProgressIndicator(
            progress = { moodStat.percentage },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = String.format("%d%%", (moodStat.percentage * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

// ─── PREVIEW ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun InsightsPreviewDark() {
    StudyFlowTheme(darkTheme = true) {
        Surface {
            InsightsContent(
                uiState = InsightsUiState(
                    averageEnergy = 4.2f,
                    averageProductivity = 3.8f,
                    moodStats = listOf(
                        MoodStat("Focused", 4, 0.57f),
                        MoodStat("Calm", 2, 0.28f),
                        MoodStat("Stressed", 1, 0.15f)
                    ),
                    energyByDayOfWeek = listOf(4f, 4.5f, 3f, 4.2f, 5f, 0f, 0f),
                    correlations = listOf(
                        InsightCorrelation(
                            title = "Focus Peaks with Positivity",
                            description = "Your productivity rating was on average 1.2 points higher on days you felt Focused/Happy.",
                            impactLevel = "High Positive"
                        )
                    ),
                    weeklyTasksCompleted = 12,
                    weeklyFocusHours = 4.5f,
                    studyDna = StudyDnaProfile(
                        archetype = "Night Owl Thinker",
                        emoji = "🦉",
                        description = "You achieve maximum cognitive momentum in the evening hours.",
                        traits = listOf("Evening Peak Focus", "Independent Learner", "Creative Problem Solver"),
                        recommendations = listOf(
                            "Schedule high-difficulty study blocks between 7 PM and 10 PM",
                            "Keep your work desk brightly lit to prevent fatigue triggers"
                        )
                    )
                )
            )
        }
    }
}
