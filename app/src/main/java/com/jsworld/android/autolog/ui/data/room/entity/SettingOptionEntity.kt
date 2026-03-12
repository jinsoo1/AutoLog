package com.jsworld.android.autolog.ui.data.room.entity

import androidx.room.ColumnInfo

data class SettingOptionEntity(
    @ColumnInfo(name = "settingId") val settingId: Long,
    @ColumnInfo(name = "typeName") val typeName: String,
    @ColumnInfo(name = "lastServiceDate") val lastServiceDate: String?,     // yyyy-MM-dd
    @ColumnInfo(name = "lastServiceMileage") val lastServiceMileage: Int?
)