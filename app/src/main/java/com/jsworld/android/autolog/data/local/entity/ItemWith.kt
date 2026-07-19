package com.jsworld.android.autolog.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity

//차량 → 정비 설정
data class CarWithSettings(
    @Embedded val car: CarEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "carId"
    )
    val settings: List<CarMaintenanceSettingEntity>
)

//설정 → 정비 타입
data class SettingWithType(
    @Embedded val setting: CarMaintenanceSettingEntity,

    @Relation(
        parentColumn = "maintenanceTypeId",
        entityColumn = "id"
    )
    val type: MaintenanceTypeEntity
)

//설정 → 교체 이력
data class SettingWithHistory(
    @Embedded val setting: CarMaintenanceSettingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "settingId"
    )
    val history: List<MaintenanceHistoryEntity>
)

data class SettingWithTypeAndHistories(
    @Embedded val setting: CarMaintenanceSettingEntity,

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