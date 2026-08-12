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
    val isActive: Boolean = true,

    /**
     * 세차 N회마다 하는 관리의 주기(세차 횟수). 세차 항목 전용.
     *
     * km·개월로는 표현할 수 없는 단위다 — "세차 3번 중 1번은 실내 클리닝"처럼
     * 세차 기록 수를 세어 진행도를 계산한다.
     */
    val intervalWashCount: Int? = null
)