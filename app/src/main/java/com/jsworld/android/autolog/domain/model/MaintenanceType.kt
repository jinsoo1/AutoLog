package com.jsworld.android.autolog.domain.model

//엔진오일, 타이어 같은 “정비 종류 마스터”
data class MaintenanceType(
    val id: Long,
    val name: String,
    val defaultIntervalKm: Int?,
    val defaultIntervalMonths: Int?,
    /** 세차·관리 항목 — 정비 시스템에서 분리되어 세차 허브에서만 다뤄진다 */
    val isCare: Boolean = false
)