package com.studyflow.app.presentation.resources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = uiState.monthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }

            // Total Logged amount card (neutral summary)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Logged Signals This Month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", uiState.totalAmount),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Weekly overview chart
            item {
                Text(
                    text = "Weekly Activity Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                WeeklyResourceBarChart(weeklyAmounts = uiState.weeklyAmounts)
            }

            // Category filter chips
            item {
                Text(
                    text = "Filter by Signal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.resources.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "No signals logged for the current month or category.",
                        iconEmoji = "📦"
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
    val barColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val maxVal = weeklyAmounts.maxOrNull() ?: 0f
                val ceiling = if (maxVal <= 0f) 10f else maxVal * 1.15f

                val barWidth = 24.dp.toPx()
                val availableWidth = size.width
                val availableHeight = size.height
                val spacing = (availableWidth - (barWidth * 7)) / 8

                // Grid lines
                val gridLines = 4
                for (i in 0 until gridLines) {
                    val y = (availableHeight / gridLines) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(availableWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw bars
                for (i in 0 until 7) {
                    val amount = weeklyAmounts.getOrElse(i) { 0f }
                    val heightRatio = amount / ceiling
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

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (day in daysOfWeek) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = labelColor,
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
    val (emoji, unitLabel) = when (resource.category.lowercase()) {
        "caffeine" -> Pair("☕", "mg")
        "sleep" -> Pair("😴", "hrs")
        "spending" -> Pair("💰", "$")
        "energy boost" -> Pair("⚡", "rating")
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
        1 -> Color(0xFF22C55E) // Emerald
        -1 -> Color(0xFFEF4444) // Red
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val indicatorColor = when (resource.category.lowercase()) {
        "caffeine" -> MaterialTheme.colorScheme.primary
        "sleep" -> MaterialTheme.colorScheme.secondary
        "spending" -> MaterialTheme.colorScheme.tertiary
        "energy boost" -> MaterialTheme.colorScheme.error
        "study materials" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.outline
    }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(resource.dateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
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
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (resource.note.isNotEmpty()) resource.note else resource.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (resource.studyEnvironment.isNotEmpty()) {
                                Text(
                                    text = "• ${resource.studyEnvironment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = impactText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = impactColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${resource.amount} $unitLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Caffeine") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var productivityImpact by remember { mutableStateOf(0) }
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
                    Text("OK")
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
        onDismissRequest = onDismiss
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                            text = { Text(category) },
                            onClick = {
                                  selectedCategory = category
                                  categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount field
            val amountLabel = when (selectedCategory.lowercase()) {
                "caffeine" -> "Amount in milligrams (mg)"
                "sleep" -> "Hours of sleep"
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

            // Productivity Impact segment
            Text(
                text = "Productivity Impact",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val impacts = listOf(
                    Triple(-1, "Negative", Color(0xFFEF4444)),
                    Triple(0, "Neutral", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                    Triple(1, "Positive", Color(0xFF22C55E))
                )
                impacts.forEach { (value, label, color) ->
                    val isSel = productivityImpact == value
                    Button(
                        onClick = { productivityImpact = value },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) color else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp)
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
                            text = { Text(env) },
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
                    Text("Cancel")
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
                    Text("Save")
                }
            }
        }
    }
}
