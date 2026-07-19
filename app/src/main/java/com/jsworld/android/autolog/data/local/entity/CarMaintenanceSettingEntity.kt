package com.jsworld.android.autolog.data.local.entity

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
    indices = [
        Index("carId"),
        Index("maintenanceTypeId"),
        Index(value = ["carId", "maintenanceTypeId"], unique = true)
    ]
)
data class CarMaintenanceSettingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val carId: Long,
    val maintenanceTypeId: Long,

    val intervalKm: Int?,
    val intervalMonths: Int?,
    val isActive: Boolean = true
)