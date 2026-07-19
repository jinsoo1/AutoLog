package com.jsworld.android.autolog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,        // 그랜저
    val plate: String,       // 12가1234
    val year: String? = null,
    val mileage: Int = 0,    // 현재 총 주행거리

    val fuelType: String? = null,
    val notes: String? = null,

    val isPrimary: Boolean = false,

    /** 마지막으로 주행거리를 업데이트한 시각 */
    val lastMileageUpdatedAt: Long? = null
)