package com.jsworld.android.autolog.domain.model

import com.jsworld.android.autolog.core.ui.UiText


data class MaintenanceSummaryUi(
    val status: MaintenanceStatus, // NORMAL / SOON / OVERDUE
    val title: UiText,             // 예: "엔진오일"
    val detail: UiText             // 예: "초과 1,200km · 3일"
)

data class CarCardUi(
    val car: Car,
    val summary: MaintenanceSummaryUi,
    val dangerCount: Int
)

data class CarMaintenanceDigest(
    val summary: MaintenanceSummaryUi,
    val dangerCount: Int
)
