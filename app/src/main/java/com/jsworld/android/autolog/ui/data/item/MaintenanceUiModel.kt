package com.jsworld.android.autolog.ui.data.item

enum class MaintenanceStatus { NORMAL, SOON, OVERDUE }

data class MaintenanceUiModel(
    val name: String,
    val status: MaintenanceStatus,
    val remainingText: String
)