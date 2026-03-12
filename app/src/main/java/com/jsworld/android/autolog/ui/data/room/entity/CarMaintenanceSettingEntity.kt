package com.jsworld.android.autolog.ui.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "car_maintenance_settings",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MaintenanceTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["maintenanceTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("maintenanceTypeId")]
)
data class CarMaintenanceSettingEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val carId: Long,                 // 어떤 차량
    val maintenanceTypeId: Long,     // 어떤 정비항목

    val intervalKm: Int?,            // 이 차량 전용 교체주기
    val intervalMonths: Int?,
    val isActive: Boolean = true // ✅ 추가
)