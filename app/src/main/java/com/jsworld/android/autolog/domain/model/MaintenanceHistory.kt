package com.jsworld.android.autolog.domain.model

//실제 교체 기록
data class MaintenanceHistory(
    val id: Long,
    val settingId: Long,

    val serviceDate: String?,
    val serviceMileage: Int?,

    val place: String?,
    val cost: Int?,
    val memo: String?
)