package com.jsworld.android.autolog.ui.data.item

data class SettingWithHistory(
    val setting: CarMaintenanceSetting,
    val type: MaintenanceType,
    val histories: List<MaintenanceHistory>
) {
    val lastHistory: MaintenanceHistory?
        get() = histories.maxByOrNull { it.serviceDate ?: "" }
}