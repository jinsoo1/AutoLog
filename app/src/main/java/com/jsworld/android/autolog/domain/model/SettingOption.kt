package com.jsworld.android.autolog.domain.model

data class SettingOption(
    val settingId: Long,
    val typeName: String,
    val lastServiceDate: String?,     // yyyy-MM-dd
    val lastServiceMileage: Int?
)