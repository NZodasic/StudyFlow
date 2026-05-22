package com.studyflow.app.presentation.resources

import com.studyflow.app.data.local.dao.ResourceCategoryTotal
import com.studyflow.app.data.local.entity.ResourceEntity

data class ResourceUiState(
    val resources: List<ResourceEntity> = emptyList(),
    val totalAmount: Double = 0.0,
    val categoryTotals: List<ResourceCategoryTotal> = emptyList(),
    val selectedCategory: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val monthLabel: String = "",
    val weeklyAmounts: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
)
