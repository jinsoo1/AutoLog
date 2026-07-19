package com.jsworld.android.autolog.domain.model

//엔진오일, 타이어 같은 “정비 종류 마스터”
data class MaintenanceType(
    val id: Long,
    val name: String,
    val defaultIntervalKm: Int?,
    val defaultIntervalMonths: Int?
)