package com.jsworld.android.autolog.data.local.entity

import androidx.room.ColumnInfo

/** 월별 금액 합계(리포트용). month = "yyyy-MM" */
data class MonthlyAmountRow(
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "total") val total: Long
)

/** 정비 기록의 월·항목명·금액 — 카테고리(정비·수리/세차) 분류는 이름 기반이라 코틀린에서 한다 */
data class MaintenanceCostRow(
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "typeName") val typeName: String,
    @ColumnInfo(name = "cost") val cost: Int?
)

/** 주행거리 관측점(날짜, 누적 km) — 월간 주행거리 계산용 */
data class MileagePointRow(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "mileage") val mileage: Int
)
