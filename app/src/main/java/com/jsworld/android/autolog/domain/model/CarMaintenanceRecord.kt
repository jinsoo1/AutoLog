package com.jsworld.android.autolog.domain.model

/**
 * 정비 항목 구분 없이 "차량이 언제 무엇을 정비했는가"를 한 줄로 나타내는 기록.
 * 정비 탭의 통합 타임라인이 이 모델을 쓴다.
 */
data class CarMaintenanceRecord(
    val historyId: Long,
    val settingId: Long,
    val typeId: Long,
    val typeName: String,
    val serviceDate: String?,
    val serviceMileage: Int?,
    val place: String?,
    val cost: Int?,
    val memo: String?,
    /** 주기 없는 항목의 기록 = 일회성 수리. 타임라인에서 배지로 구분한다. */
    val isRepair: Boolean = false
)
