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
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.studyflow.app.data.local.entity.AIRecommendationEntity
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBreathingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Productivity Insights",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 0.5.sp
                    )
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
            InsightsContent(
                uiState = uiState,
                onNavigateToPomodoro = onNavigateToPomodoro,
                onShowBreathing = { showBreathingDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    if (showBreathingDialog) {
        BreathingExerciseDialog(onDismiss = { showBreathingDialog = false })
    }
}

@Composable
private fun InsightsContent(
    uiState: InsightsUiState,
    onNavigateToPomodoro: () -> Unit,
    onShowBreathing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
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

        // ─── AI STUDY COACH SECTION ────────────────────────────────────────
        Text(
            text = "AI Study Coach Predictions 🤖",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 0.5.sp
        )

        if (uiState.aiRecommendations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your Coach is analyzing your patterns. Log sleep, caffeine, focus, and reflection to receive predictions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            uiState.aiRecommendations.forEach { recommendation ->
                AIRecommendationCard(
                    recommendation = recommendation,
                    onNavigateToPomodoro = onNavigateToPomodoro,
                    onShowBreathing = onShowBreathing
                )
            }
        }

        // ─── CORRELATION INSIGHTS ─────────────────────────────────────────
        Text(
            text = "Daily Reflection Correlations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 0.5.sp
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
                color = Color(0xFF9061F9),
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
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.3f else 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Weekly Energy Level Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
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
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.3f else 0.15f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Mood Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
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
    val isDark = isSystemInDarkTheme()
    val baseGradientColors = when (profile.archetype) {
        "Night Owl Thinker"          -> listOf(Color(0xFF1A0B2E), Color(0xFF3D1F8C))
        "Sprint Learner"             -> listOf(Color(0xFF1A1200), Color(0xFF7C4E00))
        "Deep Focus Specialist"      -> listOf(Color(0xFF001A2E), Color(0xFF004C8C))
        "Recovery-Dependent Learner" -> listOf(Color(0xFF0A1F12), Color(0xFF145C30))
        else                         -> listOf(Color(0xFF0F1628), Color(0xFF1E3A5F))
    }

    val gradientColors = if (isDark) {
        baseGradientColors.map { it.copy(alpha = 0.45f) }
    } else {
        baseGradientColors.map { it.copy(alpha = 0.15f) }
    }

    val glowColor = baseGradientColors.lastOrNull() ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        }
                    )
                )
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (isDark) 0.6f else 0.4f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.08f else 0.12f))
                            .border(1.dp, glowColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = profile.emoji, fontSize = 28.sp)
                    }
                    Column {
                        Text(
                            text = "YOUR STUDY DNA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.55f),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = profile.archetype,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.25.sp
                        )
                    }
                }

                // Description
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.85f),
                    lineHeight = 22.sp
                )

                // Trait Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profile.traits) { trait ->
                        Surface(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = glowColor.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = trait,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
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
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Personalized Recommendations",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB703),
                            letterSpacing = 0.25.sp
                        )
                    }
                    profile.recommendations.forEach { rec ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "→",
                                color = glowColor.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodySmall,
                                color = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.8f),
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
    val isDark = isSystemInDarkTheme()
    val indicatorColor = when (correlation.impactLevel) {
        "High Positive"   -> Color(0xFF10B981) // Emerald Green
        "Needs Attention" -> Color(0xFFFF5E5E) // Vibrant Coral Red
        else              -> Color(0xFF00D8F6) // Accent Cyan (for general/other)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            indicatorColor.copy(alpha = if (isDark) 0.5f else 0.35f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .border(2.dp, indicatorColor.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = correlation.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = correlation.impactLevel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = indicatorColor,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = correlation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 20.sp
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
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = if (isDark) 0.4f else 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(14.dp),
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

// ─── ENERGY BAR CHART ────────────────────────────────────────────────────────

@Composable
private fun WeeklyEnergyBarChart(
    energyLevels: List<Float>
) {
    val isDark = isSystemInDarkTheme()
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val level = energyLevels.getOrNull(index) ?: 0f
            val percentage = level / 5.0f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (level > 0f) String.format("%.1f", level) else "-",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                // Bar with a track
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (percentage > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(percentage)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFB703), // Glowing Amber
                                            Color(0xFFFF7E00)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        )
                    }
                }
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    val isDark = isSystemInDarkTheme()
    val (emoji, color) = when (moodStat.mood.lowercase()) {
        "focused", "focus"     -> Pair("🎯", Color(0xFF9061F9)) // Violet
        "happy", "joy"         -> Pair("😊", Color(0xFF00D8F6)) // Cyan
        "calm", "relaxed"      -> Pair("🧘", Color(0xFF10B981)) // Emerald Green
        "excited", "motivated" -> Pair("🚀", Color(0xFFFFB703)) // Amber
        "stressed", "anxious"  -> Pair("😰", Color(0xFFFF5E5E)) // Coral Red
        "tired", "sleepy"      -> Pair("😴", Color(0xFF6B7280)) // Slate
        else                   -> Pair("⭐", MaterialTheme.colorScheme.primary)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Text(
            text = moodStat.mood,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(85.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Styled progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.CenterStart
        ) {
            if (moodStat.percentage > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(moodStat.percentage)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.6f),
                                    color
                                )
                            )
                        )
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(5.dp)
                        )
                )
            }
        }
        Text(
            text = String.format("%d%%", (moodStat.percentage * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                ),
                onNavigateToPomodoro = {},
                onShowBreathing = {}
            )
        }
    }
}

// ─── AI RECOMMENDATION CARD ──────────────────────────────────────────────────

@Composable
private fun AIRecommendationCard(
    recommendation: AIRecommendationEntity,
    onNavigateToPomodoro: () -> Unit,
    onShowBreathing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val priorityColor = when (recommendation.priority) {
        "high" -> Color(0xFFEF4444) // Red
        "medium" -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFF10B981) // Emerald Green
    }

    val typeIcon = when (recommendation.type) {
        "schedule" -> Icons.Default.TrendingUp
        "break" -> Icons.Default.Timer
        "focus" -> Icons.Default.Timer
        "health" -> Icons.Default.SelfImprovement
        else -> Icons.Default.Lightbulb
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            priorityColor.copy(alpha = if (isDark) 0.5f else 0.35f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon Column
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(priorityColor.copy(alpha = 0.15f))
                    .border(1.dp, priorityColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = priorityColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Text Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recommendation.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Confidence: ${(recommendation.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recommendation.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                // Actions if any
                if (recommendation.action != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (recommendation.action == "START_POMODORO") {
                                onNavigateToPomodoro()
                            } else if (recommendation.action == "SHOW_BREATHING_EXERCISE") {
                                onShowBreathing()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = priorityColor.copy(alpha = 0.12f),
                            contentColor = priorityColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        val btnText = if (recommendation.action == "START_POMODORO") {
                            "Start Focus Session ⏱"
                        } else {
                            "Breathing Guide 🧘"
                        }
                        Text(
                            text = btnText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ─── BREATHING EXERCISE DIALOG ──────────────────────────────────────────────

enum class BreathingDialogPhase {
    INHALE, HOLD_IN, EXHALE, HOLD_OUT
}

@Composable
fun BreathingExerciseDialog(
    onDismiss: () -> Unit
) {
    var breathingPhase by remember { mutableStateOf(BreathingDialogPhase.INHALE) }
    var animProgress by remember { mutableStateOf(0f) }

    // Breathing phase timer cycle (16s total: 4s inhale, 4s hold, 4s exhale, 4s hold)
    LaunchedEffect(Unit) {
        while (isActive) {
            // Inhale: 4 seconds
            breathingPhase = BreathingDialogPhase.INHALE
            val inhaleStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - inhaleStart < 4000) {
                animProgress = ((System.currentTimeMillis() - inhaleStart).toFloat() / 4000f).coerceIn(0f, 1f)
                delay(16)
            }
            // Hold In: 4 seconds
            breathingPhase = BreathingDialogPhase.HOLD_IN
            animProgress = 1f
            delay(4000)

            // Exhale: 4 seconds
            breathingPhase = BreathingDialogPhase.EXHALE
            val exhaleStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - exhaleStart < 4000) {
                animProgress = (1f - ((System.currentTimeMillis() - exhaleStart).toFloat() / 4000f)).coerceIn(0f, 1f)
                delay(16)
            }
            // Hold Out: 4 seconds
            breathingPhase = BreathingDialogPhase.HOLD_OUT
            animProgress = 0f
            delay(4000)
        }
    }

    val phaseText = when (breathingPhase) {
        BreathingDialogPhase.INHALE -> "Inhale..."
        BreathingDialogPhase.HOLD_IN -> "Hold..."
        BreathingDialogPhase.EXHALE -> "Exhale..."
        BreathingDialogPhase.HOLD_OUT -> "Hold..."
    }

    val scale = 1.0f + (animProgress * 0.6f)
    val alpha = 0.1f + (animProgress * 0.3f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Box Breathing Guide 🧘",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Breathing Halo
                Box(
                    modifier = Modifier
                        .size(160.dp * scale)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            shape = CircleShape
                        )
                )
                // Inner Breathing Halo
                Box(
                    modifier = Modifier
                        .size(120.dp * (1f + animProgress * 0.4f))
                        .background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha * 0.7f),
                            shape = CircleShape
                        )
                )
                
                // Phase Text
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        }
    )
}
