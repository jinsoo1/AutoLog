package com.jsworld.android.autolog.ui.data.room.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SettingWithHistoryEntity(

    @Embedded
    val setting: CarMaintenanceSettingEntity,

    @Relation(
        parentColumn = "maintenanceTypeId",
        entityColumn = "id"
    )
    val type: MaintenanceTypeEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "settingId"
    )
    val histories: List<MaintenanceHistoryEntity>
)