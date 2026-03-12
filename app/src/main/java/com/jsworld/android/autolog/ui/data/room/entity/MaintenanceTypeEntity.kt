package com.jsworld.android.autolog.ui.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_types")
data class MaintenanceTypeEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,          // 엔진오일
    val defaultIntervalKm: Int?,
    val defaultIntervalMonths: Int?
)