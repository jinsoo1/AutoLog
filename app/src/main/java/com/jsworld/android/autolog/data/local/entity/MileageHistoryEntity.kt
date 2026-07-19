package com.jsworld.android.autolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mileage_history",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("carId"),
        Index(value = ["carId", "recordedAt"])
    ]
)
data class MileageHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val carId: Long,

    /** 해당 시점의 총 주행거리 */
    val mileage: Int,

    /** 기록 시각 */
    val recordedAt: Long = System.currentTimeMillis(),

    /** 선택 메모 (선택사항) */
    val memo: String? = null
)