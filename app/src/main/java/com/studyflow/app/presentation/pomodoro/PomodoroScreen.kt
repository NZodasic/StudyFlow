package com.studyflow.app.presentation.pomodoro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.app.data.local.entity.PomodoroSessionEntity
import com.studyflow.app.ui.components.EmptyStateView
import com.studyflow.app.ui.components.StudyFlowTopBar
import com.studyflow.app.ui.theme.StudyFlowTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    PomodoroContent(
        uiState = uiState,
        onStart = viewModel::startTimer,
        onPause = viewModel::pauseTimer,
        onReset = viewModel::resetTimer,
        onSkip = viewModel::skipSession,
        onTaskLabelChange = viewModel::updateTaskLabel,
        onNavigateToSettings = onNavigateToSettings,
        onSaveReflection = viewModel::saveSessionReflection,
        onCancelReflection = { viewModel.setReflectionDialogVisible(false) }
    )
}

enum class AmbientSoundType {
    NONE, WHITE, BROWN
}

enum class BreathingPhase {
    INHALE, HOLD_IN, EXHALE, HOLD_OUT
}

@Composable
fun AmbientModeOverlay(
    secondsRemaining: Int,
    totalSeconds: Int,
    mode: PomodoroMode,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ambientSoundType by remember { mutableStateOf(AmbientSoundType.NONE) }
    var breathingPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var animProgress by remember { mutableStateOf(0f) }

    // Audio generator triggered by ambientSoundType
    LaunchedEffect(ambientSoundType) {
        if (ambientSoundType == AmbientSoundType.NONE) return@LaunchedEffect
        
        withContext(Dispatchers.IO) {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufferSize > 0) minBufferSize else 8192
            val audioTrack = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                null
            }

            if (audioTrack == null) return@withContext

            try {
                audioTrack.play()
                val buffer = ShortArray(bufferSize / 2)
                var lastOut = 0.0f
                val random = Random(System.currentTimeMillis())
                while (isActive) {
                    for (i in buffer.indices) {
                        val white = random.nextFloat() * 2f - 1f
                        if (ambientSoundType == AmbientSoundType.BROWN) {
                            // Leaky integrator for brown noise: 1/f^2 slope
                            lastOut = (lastOut + (0.02f * white)) / 1.02f
                            val sample = (lastOut * 3.5f * Short.MAX_VALUE).toInt()
                            buffer[i] = sample.coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        } else {
                            // White noise: random PCM samples
                            val sample = (white * 0.15f * Short.MAX_VALUE).toInt()
                            buffer[i] = sample.coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                    yield()
                }
            } catch (e: Exception) {
                // Cancelled or errored
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Breathing phase timer cycle (16s total: 4s inhale, 4s hold, 4s exhale, 4s hold)
    LaunchedEffect(Unit) {
        while (isActive) {
            // Inhale: 4 seconds
            breathingPhase = BreathingPhase.INHALE
            val inhaleStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - inhaleStart < 4000) {
                animProgress = ((System.currentTimeMillis() - inhaleStart).toFloat() / 4000f).coerceIn(0f, 1f)
                delay(16)
            }
            // Hold In: 4 seconds
            breathingPhase = BreathingPhase.HOLD_IN
            animProgress = 1f
            delay(4000)

            // Exhale: 4 seconds
            breathingPhase = BreathingPhase.EXHALE
            val exhaleStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - exhaleStart < 4000) {
                animProgress = (1f - ((System.currentTimeMillis() - exhaleStart).toFloat() / 4000f)).coerceIn(0f, 1f)
                delay(16)
            }
            // Hold Out: 4 seconds
            breathingPhase = BreathingPhase.HOLD_OUT
            animProgress = 0f
            delay(4000)
        }
    }

    val phaseText = when (breathingPhase) {
        BreathingPhase.INHALE -> "Inhale..."
        BreathingPhase.HOLD_IN -> "Hold..."
        BreathingPhase.EXHALE -> "Exhale..."
        BreathingPhase.HOLD_OUT -> "Hold..."
    }

    // Interpolate scale and alpha based on animProgress
    // Scale ranges from 1.0f to 1.6f
    val scale = 1.0f + (animProgress * 0.6f)
    val alpha = 0.1f + (animProgress * 0.3f)

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Full screen breathing waves
        Box(
            modifier = Modifier
                .size(300.dp * scale)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp * (1f + animProgress * 0.4f))
                .background(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha * 0.7f),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = phaseText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 72.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = when (mode) {
                    PomodoroMode.FOCUS -> "FOCUS PERIOD"
                    else -> "RECHARGE PERIOD"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 3.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sound Selector
            Text(
                text = "Ambient Soundscapes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    AmbientSoundType.NONE to "Silent",
                    AmbientSoundType.WHITE to "White Noise 🔕",
                    AmbientSoundType.BROWN to "Brown Noise 🌊"
                ).forEach { (type, label) ->
                    val selected = ambientSoundType == type
                    FilterChip(
                        selected = selected,
                        onClick = { ambientSoundType = type },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("Exit Ambient Mode", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroContent(
    uiState: PomodoroUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onTaskLabelChange: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSaveReflection: (Int, String, Int) -> Unit,
    onCancelReflection: () -> Unit
) {
    var isAmbientModeActive by remember { mutableStateOf(false) }

    if (uiState.showReflectionDialog) {
        var rating by remember { mutableStateOf(5) }
        var reflectionText by remember { mutableStateOf("") }
        var distractions by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = onCancelReflection,
            title = {
                Text(
                    text = "Session Complete! 🎯",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.taskLabel.isNotEmpty()) {
                        Text(
                            text = "You focused on: ${uiState.taskLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text("How was your focus?", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isSelected = i <= rating
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { rating = i }
                            )
                        }
                    }

                    Text("Distractions count", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { if (distractions > 0) distractions-- },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("-", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "$distractions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { distractions++ },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    OutlinedTextField(
                        value = reflectionText,
                        onValueChange = { reflectionText = it },
                        label = { Text("What did you achieve? (Reflection)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveReflection(rating, reflectionText, distractions)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Reflection")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelReflection) {
                    Text("Skip")
                }
            }
        )
    }

    if (isAmbientModeActive) {
        AmbientModeOverlay(
            secondsRemaining = uiState.secondsRemaining,
            totalSeconds = uiState.totalSeconds,
            mode = uiState.mode,
            onExit = { isAmbientModeActive = false }
        )
    } else {
        Scaffold(
            topBar = {
                StudyFlowTopBar(
                    title = "Focus Space",
                    actions = {
                        IconButton(onClick = { isAmbientModeActive = true }) {
                            Text("🌿", fontSize = 20.sp)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // Main Timer Canvas Section with breathing effect
            item {
                Spacer(modifier = Modifier.height(10.dp))
                TimerDisplay(
                    secondsRemaining = uiState.secondsRemaining,
                    totalSeconds = uiState.totalSeconds,
                    mode = uiState.mode,
                    isRunning = uiState.isRunning
                )
            }

            // Mode Label
            item {
                val modeLabel = when (uiState.mode) {
                    PomodoroMode.FOCUS -> "Deep Focus"
                    PomodoroMode.SHORT_BREAK -> "Rest & Reset"
                    PomodoroMode.LONG_BREAK -> "Extended Break"
                }
                val modeColor = when (uiState.mode) {
                    PomodoroMode.FOCUS -> MaterialTheme.colorScheme.primary
                    PomodoroMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
                    PomodoroMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = modeColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Session ${uiState.sessionCount} of 4",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Controls Block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Start / Pause
                    Button(
                        onClick = {
                            if (uiState.isRunning) onPause() else onStart()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.mode == PomodoroMode.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp)
                    ) {
                        Text(
                            text = if (uiState.isRunning) "PAUSE" else "START",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Skip
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            text = "⏭",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Optional Task Input
            item {
                OutlinedTextField(
                    value = uiState.taskLabel,
                    onValueChange = onTaskLabelChange,
                    label = { Text("What are you working on?") },
                    placeholder = { Text("e.g. Study Chemistry, Read Essay") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Settings Summary Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.focusMinutes}m focus · ${uiState.shortBreakMinutes}m break",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onNavigateToSettings) {
                            Text("Configure", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // History Label
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "History Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // History List
            if (uiState.history.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No completed focus sessions today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.history) { session ->
                    HistoryItemRow(session = session)
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
}

@Composable
fun TimerDisplay(
    secondsRemaining: Int,
    totalSeconds: Int,
    mode: PomodoroMode,
    isRunning: Boolean
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    
    // Progress calculation
    val progress = if (totalSeconds > 0) {
        secondsRemaining.toFloat() / totalSeconds.toFloat()
    } else {
        1.0f
    }

    val sweepAngle = progress * 360f

    val primaryColor = MaterialTheme.colorScheme.primary
    val breakColor = MaterialTheme.colorScheme.secondary
    val longBreakColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    val activeColor = when (mode) {
        PomodoroMode.FOCUS -> primaryColor
        PomodoroMode.SHORT_BREAK -> breakColor
        PomodoroMode.LONG_BREAK -> longBreakColor
    }

    // Breathing pulse animation for active study session
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // Breathing soft light halo
        if (isRunning && mode == PomodoroMode.FOCUS) {
            Box(
                modifier = Modifier
                    .size(220.dp * pulseScale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = pulseAlpha))
            )
        }

        Canvas(modifier = Modifier.size(220.dp)) {
            // Track Circle
            drawCircle(
                color = trackColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Outer soft glow Arc
            drawArc(
                color = activeColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset((size.width - size.minDimension) / 2 + 2.dp.toPx(), (size.height - size.minDimension) / 2 + 2.dp.toPx()),
                size = Size(size.minDimension - 4.dp.toPx(), size.minDimension - 4.dp.toPx()),
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // Main Progress Arc
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset((size.width - size.minDimension) / 2 + 5.dp.toPx(), (size.height - size.minDimension) / 2 + 5.dp.toPx()),
                size = Size(size.minDimension - 10.dp.toPx(), size.minDimension - 10.dp.toPx()),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 52.sp,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (mode) {
                    PomodoroMode.FOCUS -> "FOCUSING"
                    else -> "RECHARGING"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun HistoryItemRow(session: PomodoroSessionEntity) {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedDate = formatter.format(Date(session.completedAtMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (session.taskLabel.isNotEmpty()) session.taskLabel else "Focus Session",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (session.focusRating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        for (i in 1..5) {
                            val isSelected = i <= session.focusRating
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFF59E0B) else Color.LightGray.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (session.distractionsCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ ${session.distractionsCount} distraction(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                if (session.reflectionNote.isNotEmpty()) {
                    Text(
                        text = "\"${session.reflectionNote}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.durationMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PomodoroContentLightPreview() {
    StudyFlowTheme(darkTheme = false) {
        PomodoroContent(
            uiState = PomodoroUiState(
                secondsRemaining = 15 * 60 + 45,
                totalSeconds = 25 * 60,
                isRunning = true,
                mode = PomodoroMode.FOCUS,
                history = listOf(
                    PomodoroSessionEntity(id = 1, durationMinutes = 25, taskLabel = "Physics Exam Study"),
                    PomodoroSessionEntity(id = 2, durationMinutes = 25, taskLabel = "Android Composable UI")
                )
            ),
            onStart = {},
            onPause = {},
            onReset = {},
            onSkip = {},
            onTaskLabelChange = {},
            onNavigateToSettings = {},
            onSaveReflection = { _, _, _ -> },
            onCancelReflection = {}
        )
    }
}
