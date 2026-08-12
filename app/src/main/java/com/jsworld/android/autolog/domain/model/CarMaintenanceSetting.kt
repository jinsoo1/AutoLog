package com.jsworld.android.autolog.domain.model

//차량별 교체주기 설정
data class CarMaintenanceSetting(
    val id: Long,
    val carId: Long,
    val maintenanceTypeId: Long,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val isActive: Boolean = true,
    /** 세차 N회마다 하는 관리의 주기(세차 항목 전용) */
    val intervalWashCount: Int? = null
)