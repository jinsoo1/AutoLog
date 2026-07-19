package com.jsworld.android.autolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_history",
    foreignKeys = [
        ForeignKey(
            entity = CarMaintenanceSettingEntity::class,
            parentColumns = ["id"],
            childColumns = ["settingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("settingId")]
)
data class MaintenanceHistoryEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val settingId: Long,      // 차량 + 항목 조합

    val serviceDate: String?,
    val serviceMileage: Int?,

    val place: String? = null,
    val cost: Int? = null,
    val memo: String? = null
)