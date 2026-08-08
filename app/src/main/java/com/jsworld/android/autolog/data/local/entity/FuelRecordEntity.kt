package com.jsworld.android.autolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 주유(충전) 기록.
 *
 * 연비/전비는 계산하지 않으므로 만땅 여부(`isFullTank`) 같은 컬럼을 두지 않는다.
 * 플러그인 하이브리드에서 성립하지 않고, 사용자가 만땅 여부를 정확히 알기 어려워
 * 값이 신뢰되지 않기 때문이다. 자세한 배경은 docs/DEVELOPMENT.md 참고.
 */
@Entity(
    tableName = "fuel_records",
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
        Index(value = ["carId", "filledAt"])
    ]
)
data class FuelRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val carId: Long,

    /** yyyy-MM-dd */
    val filledAt: String,

    /** 주유 시점의 총 주행거리 */
    val mileage: Int?,

    /** 결제 금액(원) */
    val amount: Int?,

    /** 주유량. L 또는 kWh. 소수점이 있으므로 Double */
    val quantity: Double?,

    /** 단가(원/L, 원/kWh). 금액·주유량으로 계산해 넣을 수 있다 */
    val unitPrice: Int?,

    /** "L" 또는 "kWh" — 차량 연료 타입으로 결정한다 */
    val unit: String,

    val station: String?,
    val memo: String?,

    /**
     * 영수증 사진 경로.
     * ⚠️ 백업(JSON)에는 사진이 포함되지 않으므로, 복원 후 이 경로의 파일이 없을 수 있다.
     */
    val photoPath: String? = null
)
