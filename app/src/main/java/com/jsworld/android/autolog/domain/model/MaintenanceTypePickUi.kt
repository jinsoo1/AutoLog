package com.jsworld.android.autolog.domain.model

data class MaintenanceTypePickUi(
    val typeId: Long,
    val typeName: String,
    val defaultKm: Int?,
    val defaultMonths: Int?,
    val checked: Boolean,
    val settingId: Long? // 있으면 enable/disable에 사용 가능
)