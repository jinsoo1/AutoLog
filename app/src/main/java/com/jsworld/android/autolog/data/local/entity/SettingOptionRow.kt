package com.jsworld.android.autolog.data.local.entity

import androidx.room.ColumnInfo

data class SettingOptionRow(
    @ColumnInfo(name = "settingId") val settingId: Long,
    @ColumnInfo(name = "typeName") val typeName: String,
    @ColumnInfo(name = "lastServiceDate") val lastServiceDate: String?,
    @ColumnInfo(name = "lastServiceMileage") val lastServiceMileage: Int?,
    @ColumnInfo(name = "isCare") val isCare: Boolean = false,
    @ColumnInfo(name = "intervalMonths") val intervalMonths: Int? = null,
    @ColumnInfo(name = "intervalWashCount") val intervalWashCount: Int? = null
)