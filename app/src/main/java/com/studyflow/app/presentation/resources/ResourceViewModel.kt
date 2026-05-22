package com.studyflow.app.presentation.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.local.dao.ResourceCategoryTotal
import com.studyflow.app.data.local.entity.ResourceEntity
import com.studyflow.app.data.repository.ResourceRepository
import com.studyflow.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ResourceViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResourceUiState(isLoading = true))
    val uiState: StateFlow<ResourceUiState> = _uiState.asStateFlow()

    private val currentMonthCal = Calendar.getInstance()
    private var currentWorkspaceId: Long? = null

    private val _monthRange = MutableStateFlow(getMonthStartEnd(currentMonthCal))
    private val workspaceFlow = settingsRepository.getUserSettings()
        .map { it?.selectedWorkspaceId }
        .distinctUntilChanged()

    init {
        observeResources()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeResources() {
        viewModelScope.launch {
            combine(_monthRange, workspaceFlow) { range, workspaceId ->
                currentWorkspaceId = workspaceId
                Triple(range.first, range.second, workspaceId)
            }
            .flatMapLatest { triple ->
                val start = triple.first
                val end = triple.second
                val workspaceId = triple.third

                combine(
                    resourceRepository.getResourcesForMonth(start, end, workspaceId),
                    resourceRepository.getTotalForMonth(start, end, workspaceId),
                    resourceRepository.getTotalByCategory(start, end, workspaceId)
                ) { list, total, catTotals ->
                    Triple(list, total, catTotals)
                }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
            .collect { triple ->
                val allResources = triple.first
                val total = triple.second
                val catTotals = triple.third

                val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val monthLabel = formatter.format(currentMonthCal.time)

                _uiState.update { state ->
                    val filteredResources = if (state.selectedCategory == "All") {
                        allResources
                    } else {
                        allResources.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
                    }

                    state.copy(
                        resources = filteredResources,
                        totalAmount = total,
                        categoryTotals = catTotals,
                        monthLabel = monthLabel,
                        weeklyAmounts = calculateWeeklyAmounts(allResources),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        triggerMonthRefresh()
    }

    fun nextMonth() {
        currentMonthCal.add(Calendar.MONTH, 1)
        _monthRange.value = getMonthStartEnd(currentMonthCal)
    }

    fun previousMonth() {
        currentMonthCal.add(Calendar.MONTH, -1)
        _monthRange.value = getMonthStartEnd(currentMonthCal)
    }

    fun addResource(
        amount: Double,
        category: String,
        note: String,
        dateMillis: Long,
        productivityImpact: Int,
        studyEnvironment: String
    ) {
        viewModelScope.launch {
            try {
                val newResource = ResourceEntity(
                    amount = amount,
                    category = category,
                    note = note,
                    dateMillis = dateMillis,
                    workspaceId = currentWorkspaceId,
                    productivityImpact = productivityImpact,
                    studyEnvironment = studyEnvironment
                )
                resourceRepository.insertResource(newResource)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun deleteResource(resource: ResourceEntity) {
        viewModelScope.launch {
            try {
                resourceRepository.deleteResource(resource)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun triggerMonthRefresh() {
        _monthRange.value = getMonthStartEnd(currentMonthCal)
    }

    private fun getMonthStartEnd(cal: Calendar): Pair<Long, Long> {
        val startCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    private fun calculateWeeklyAmounts(monthResources: List<ResourceEntity>): List<Float> {
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfWeek = (startOfWeek.clone() as Calendar).apply {
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val weeklyTotals = FloatArray(7) { 0f }
        val itemCal = Calendar.getInstance()

        for (res in monthResources) {
            if (res.dateMillis in startOfWeek.timeInMillis..endOfWeek.timeInMillis) {
                itemCal.timeInMillis = res.dateMillis
                val dayOfWeek = itemCal.get(Calendar.DAY_OF_WEEK)
                weeklyTotals[dayOfWeek - 1] += res.amount.toFloat()
            }
        }
        return weeklyTotals.toList()
    }
}
