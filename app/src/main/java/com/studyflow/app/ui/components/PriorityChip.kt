package com.studyflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PriorityChip(
    priority: Int,
    modifier: Modifier = Modifier
) {
    val label = when (priority) {
        0 -> "Low"
        1 -> "Medium"
        else -> "High"
    }

    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (priority) {
        0 -> Pair(colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
        1 -> Pair(colorScheme.primaryContainer, colorScheme.primary)
        else -> Pair(colorScheme.errorContainer, colorScheme.error)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
