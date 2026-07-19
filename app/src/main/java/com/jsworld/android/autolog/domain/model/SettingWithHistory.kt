package com.jsworld.android.autolog.domain.model

data class SettingWithHistory(
    val setting: CarMaintenanceSetting,
    val type: MaintenanceType,
    val histories: List<MaintenanceHistory>
) {
    val lastHistory: MaintenanceHistory?
        get() = histories.maxByOrNull { it.serviceDate ?: "" }
}