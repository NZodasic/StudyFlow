package com.studyflow.app.presentation.resources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.app.data.local.entity.ResourceEntity
import com.studyflow.app.ui.components.CategoryChip
import com.studyflow.app.ui.components.EmptyStateView
import com.studyflow.app.ui.components.StudyFlowTopBar
import com.studyflow.app.ui.theme.StudyFlowTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ResourceScreen(
    viewModel: ResourceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddBottomSheet by remember { mutableStateOf(false) }

    ResourceContent(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSelectCategory = viewModel::selectCategory,
        onDeleteResource = viewModel::deleteResource,
        onAddResourceClick = { showAddBottomSheet = true }
    )

    if (showAddBottomSheet) {
        AddResourceBottomSheet(
            onDismiss = { showAddBottomSheet = false },
            onSave = { amount, category, note, dateMillis, productivityImpact, studyEnv ->
                viewModel.addResource(amount, category, note, dateMillis, productivityImpact, studyEnv)
                showAddBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceContent(
    uiState: ResourceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onDeleteResource: (ResourceEntity) -> Unit,
    onAddResourceClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultCategories = listOf("All", "Caffeine", "Sleep", "Energy", "Stress", "Hydration", "Environment")
    val legacyCategories = uiState.categoryTotals.map { it.category }
        .filter { category -> defaultCategories.none { it.equals(category, ignoreCase = true) } }
    val categories = defaultCategories + legacyCategories

    Scaffold(
        topBar = {
            StudyFlowTopBar(title = "Performance Signals")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddResourceClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Signal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = uiState.monthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Total Logged amount card (neutral summary)
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
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.2f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.2f else 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Logged Signals This Month",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", uiState.totalAmount),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Weekly overview chart
            item {
                Text(
                    text = "Weekly Activity Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                WeeklyResourceBarChart(weeklyAmounts = uiState.weeklyAmounts)
            }

            // Category filter chips
            item {
                Text(
                    text = "Filter by Signal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = uiState.selectedCategory == category
                        CategoryChip(
                            category = category,
                            isSelected = isSelected,
                            onClick = { onSelectCategory(category) }
                        )
                    }
                }
            }

            // Resources list section
            item {
                Text(
                    text = "Logged Signals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.5.sp
                )
            }

            if (uiState.resources.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "No signals logged for the current month or category.",
                        icon = Icons.Default.Info
                    )
                }
            } else {
                items(uiState.resources, key = { it.id }) { resource ->
                    ResourceItemRow(resource = resource, onDelete = { onDeleteResource(resource) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun WeeklyResourceBarChart(
    weeklyAmounts: List<Float>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
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
                            primaryColor.copy(alpha = if (isDark) 0.3f else 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val maxVal = weeklyAmounts.maxOrNull() ?: 0f
                val ceiling = if (maxVal <= 0f) 10f else maxVal * 1.15f

                val barWidth = 22.dp.toPx()
                val availableWidth = size.width
                val availableHeight = size.height
                val spacing = (availableWidth - (barWidth * 7)) / 8

                // Draw tracks and bars
                for (i in 0 until 7) {
                    val amount = weeklyAmounts.getOrElse(i) { 0f }
                    val heightRatio = amount / ceiling
                    val barHeight = availableHeight * heightRatio
                    val x = spacing + i * (barWidth + spacing)
                    
                    // Draw full track representing limit/scale
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, availableHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // Draw actual bar
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
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (day in daysOfWeek) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = labelColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceItemRow(
    resource: ResourceEntity,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val (emoji, unitLabel) = when (resource.category.lowercase()) {
        "caffeine" -> Pair("☕", "mg")
        "sleep" -> Pair("😴", "hrs")
        "spending" -> Pair("💰", "$")
        "energy boost", "energy" -> Pair("⚡", "rating")
        "study materials" -> Pair("📚", "items")
        "entertainment" -> Pair("🎮", "min")
        else -> Pair("📦", "units")
    }

    val impactText = when (resource.productivityImpact) {
        1 -> "+1 Productive"
        -1 -> "-1 Unproductive"
        else -> "0 Neutral"
    }

    val impactColor = when (resource.productivityImpact) {
        1 -> Color(0xFF10B981) // Emerald
        -1 -> Color(0xFFFF5E5E) // Coral Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val indicatorColor = when (resource.category.lowercase()) {
        "caffeine" -> Color(0xFF9061F9) // Violet
        "sleep" -> Color(0xFF00D8F6) // Cyan
        "spending" -> Color(0xFFFFB703) // Amber
        "energy boost", "energy" -> Color(0xFFFF5E5E) // Red
        "study materials" -> Color(0xFF10B981) // Green
        else -> MaterialTheme.colorScheme.secondary
    }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(resource.dateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
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
                            indicatorColor.copy(alpha = if (isDark) 0.4f else 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
                            .border(1.dp, indicatorColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (resource.note.isNotEmpty()) resource.note else resource.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (resource.studyEnvironment.isNotEmpty()) {
                                Text(
                                    text = "• ${resource.studyEnvironment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = impactText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                            color = impactColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${resource.amount} $unitLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Signal",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceBottomSheet(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long, Int, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Caffeine") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val productivityImpact = remember(selectedCategory, amountStr) {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        when (selectedCategory.lowercase(java.util.Locale.ROOT)) {
            "sleep" -> {
                when {
                    amount in 7.0..9.0 -> 1
                    amount < 6.0 || amount > 10.0 -> -1
                    else -> 0
                }
            }
            "caffeine" -> {
                when {
                    amount in 50.0..250.0 -> 1
                    amount > 350.0 -> -1
                    else -> 0
                }
            }
            "energy" -> {
                when {
                    amount >= 7.0 || (amount in 4.0..5.0) -> 1
                    amount <= 3.0 || (amount in 1.0..2.0) -> -1
                    else -> 0
                }
            }
            "stress" -> {
                when {
                    amount >= 7.0 || (amount in 4.0..5.0) -> -1
                    amount <= 3.0 || (amount in 1.0..2.0) -> 1
                    else -> 0
                }
            }
            "hydration" -> {
                when {
                    amount >= 6.0 || amount >= 1500.0 -> 1
                    amount < 4.0 || (amount in 10.0..1000.0) -> -1
                    else -> 0
                }
            }
            else -> 0
        }
    }
    var studyEnvironment by remember { mutableStateOf("Home") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var envExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Caffeine", "Sleep", "Energy", "Stress", "Hydration", "Environment")
    val environments = listOf("Home", "Library", "Cafe", "School", "Other")

    val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.92f else 0.98f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Log Signal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )

            // Category selection
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    readOnly = true,
                    value = selectedCategory,
                    onValueChange = {},
                    label = { Text("Signal Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, fontWeight = FontWeight.Bold) },
                            onClick = {
                                  selectedCategory = category
                                  categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount field
            if (selectedCategory.equals("sleep", ignoreCase = true)) {
                val sleepHours = amountStr.toFloatOrNull() ?: 8f
                // If it was empty or parsing failed, initialize it to 8 so it looks correct immediately
                LaunchedEffect(selectedCategory) {
                    if (amountStr.isBlank()) amountStr = "8.0"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hours of sleep: ${String.format(Locale.getDefault(), "%.1f", sleepHours)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = sleepHours,
                        onValueChange = { amountStr = String.format(Locale.US, "%.1f", it) },
                        valueRange = 0f..12f,
                        steps = 23, // 0.5 hour increments
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val amountLabel = when (selectedCategory.lowercase()) {
                    "caffeine" -> "Amount in milligrams (mg)"
                    "spending" -> "Cost in dollars ($)"
                    "energy", "energy boost" -> "Energy rating (1-10)"
                    "stress" -> "Stress level (1-10)"
                    "hydration" -> "Water / hydration cups"
                    "environment" -> "Environment quality (1-10)"
                    "study materials" -> "Number of items"
                    "entertainment" -> "Duration in minutes"
                    else -> "Amount / Value"
                }

                TextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(amountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Productivity Impact segment
            Text(
                text = "Productivity Impact (Auto-calculated)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val impacts = listOf(
                    Triple(-1, "Negative", Color(0xFFFF5E5E)),
                    Triple(0, "Neutral", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                    Triple(1, "Positive", Color(0xFF10B981))
                )
                impacts.forEach { (value, label, color) ->
                    val isSel = productivityImpact == value
                    Button(
                        onClick = { /* Auto-calculated, non-interactive */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Study Environment dropdown
            ExposedDropdownMenuBox(
                expanded = envExpanded,
                onExpandedChange = { envExpanded = !envExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    readOnly = true,
                    value = studyEnvironment,
                    onValueChange = {},
                    label = { Text("Study Environment") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = envExpanded) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = envExpanded,
                    onDismissRequest = { envExpanded = false }
                ) {
                    environments.forEach { env ->
                        DropdownMenuItem(
                            text = { Text(env, fontWeight = FontWeight.Bold) },
                            onClick = {
                                studyEnvironment = env
                                envExpanded = false
                            }
                        )
                    }
                }
            }

            // Note field
            TextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (e.g. Double Espresso, Cafe Latte, etc.)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Date picker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormatter.format(Date(dateMillis)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0.0) {
                            onSave(amount, selectedCategory, note, dateMillis, productivityImpact, studyEnvironment)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
