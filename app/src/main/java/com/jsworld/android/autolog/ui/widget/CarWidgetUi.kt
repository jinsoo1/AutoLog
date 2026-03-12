package com.jsworld.android.autolog.ui.widget

import com.jsworld.android.autolog.ui.data.item.MaintenanceStatus

data class CarWidgetUi(
    val carName: String,
    val plate: String,
    val mileage: Int,
    val overallStatus: MaintenanceStatus,
    val dangerCount: Int,
    val rows: List<MaintenanceProgressRow>
)

data class MaintenanceProgressRow(
    val name: String,
    val status: MaintenanceStatus,
    val progress: Float,      // 0..1
    val remainText: String    // "초과 1200km" / "잔여 22100km·1096일"
)

data class RowWithKey(
    val row: MaintenanceProgressRow,
    val statusRank: Int,
    val urgentKey: Long
)
