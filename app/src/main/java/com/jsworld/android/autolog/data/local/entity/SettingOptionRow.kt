package com.jsworld.android.autolog.data.local.entity

import androidx.room.ColumnInfo

data class SettingOptionRow(
    @ColumnInfo(name = "settingId") val settingId: Long,
    @ColumnInfo(name = "typeName") val typeName: String,
    @ColumnInfo(name = "lastServiceDate") val lastServiceDate: String?,
    @ColumnInfo(name = "lastServiceMileage") val lastServiceMileage: Int?
)