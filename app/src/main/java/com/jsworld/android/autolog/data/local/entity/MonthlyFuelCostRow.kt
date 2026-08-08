package com.jsworld.android.autolog.data.local.entity

import androidx.room.ColumnInfo

/** 월별 주유 지출 집계 결과. `month` 는 "yyyy-MM", `unit` 은 "L"/"kWh"/"kg". */
data class MonthlyFuelCostRow(
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "totalAmount") val totalAmount: Int,
    @ColumnInfo(name = "totalQuantity") val totalQuantity: Double
)
