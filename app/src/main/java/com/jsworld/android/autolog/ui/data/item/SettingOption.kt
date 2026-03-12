package com.jsworld.android.autolog.ui.data.item

data class SettingOption(
    val settingId: Long,
    val typeName: String,
    val lastServiceDate: String?,     // yyyy-MM-dd
    val lastServiceMileage: Int?
)