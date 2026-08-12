package com.jsworld.android.autolog.data.local.entity

import androidx.room.ColumnInfo

/**
 * 차량 전체 정비 기록(항목 이름 포함) 조회 결과.
 * 정비 탭 통합 타임라인에서 쓴다.
 */
data class CarMaintenanceRecordRow(
    @ColumnInfo(name = "historyId") val historyId: Long,
    @ColumnInfo(name = "settingId") val settingId: Long,
    @ColumnInfo(name = "typeId") val typeId: Long,
    @ColumnInfo(name = "typeName") val typeName: String,
    @ColumnInfo(name = "serviceDate") val serviceDate: String?,
    @ColumnInfo(name = "serviceMileage") val serviceMileage: Int?,
    @ColumnInfo(name = "place") val place: String?,
    @ColumnInfo(name = "cost") val cost: Int?,
    @ColumnInfo(name = "memo") val memo: String?,
    /** 주기(차량 설정·기본값 모두)가 없는 항목의 기록 = 일회성 수리 */
    @ColumnInfo(name = "isRepair") val isRepair: Boolean,
    @ColumnInfo(name = "isCare") val isCare: Boolean = false
)
