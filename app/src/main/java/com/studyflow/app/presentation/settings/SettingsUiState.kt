package com.studyflow.app.presentation.settings

import com.studyflow.app.data.local.entity.UserSettingsEntity

data class SettingsUiState(
    val userSettings: UserSettingsEntity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
