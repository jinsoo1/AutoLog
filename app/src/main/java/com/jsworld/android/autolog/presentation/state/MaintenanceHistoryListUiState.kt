package com.jsworld.android.autolog.presentation.state

import com.jsworld.android.autolog.domain.model.MaintenanceHistory

data class MaintenanceHistoryListUiState(
    val loading: Boolean = true,
    val typeName: String = "정비 내역",
    val histories: List<MaintenanceHistory> = emptyList()
)
