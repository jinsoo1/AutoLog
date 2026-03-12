package com.jsworld.android.autolog.ui.data.item

//차량별 교체주기 설정
data class CarMaintenanceSetting(
    val id: Long,
    val carId: Long,
    val maintenanceTypeId: Long,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val isActive: Boolean = true
)