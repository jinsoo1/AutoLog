package com.jsworld.android.autolog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 세차·관리 항목. 정비와 완전히 분리된 테이블이다.
 *
 * 정비처럼 2단(전역 타입 + 차량별 설정)으로 두지 않는다 — 세차 항목은 개수가 적고
 * 차량마다 리듬이 달라서, 차량별 단일 테이블이 더 단순하고 오히려 정확하다.
 */
@Entity(
    tableName = "care_items",
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
        Index(value = ["carId", "name"], unique = true)
    ]
)
data class CareItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val carId: Long,
    val name: String,

    /** N일마다. 개월이 아니라 일 단위 — "45일마다"처럼 세밀하게 정할 수 있어야 한다 */
    val intervalDays: Int? = null,

    /**
     * 세차 N회마다. km·개월로 표현할 수 없는 단위라 세차에만 있다
     * ("세차 3번 중 1번은 실내 클리닝").
     */
    val intervalWashCount: Int? = null,

    val isActive: Boolean = true
)

/**
 * 세차·관리 기록.
 *
 * 정비 기록과 달리 주행거리가 없고, 대신 방식(셀프/자동/손세차 등)이 있다 —
 * 세차를 자세히 남기는 사용자의 실제 관심사가 거기에 있다.
 */
@Entity(
    tableName = "care_records",
    foreignKeys = [
        ForeignKey(
            entity = CareItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["careItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("careItemId")]
)
data class CareRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val careItemId: Long,

    /**
     * yyyy-MM-dd. 이전 버전에서 이관된 기록에 날짜가 없을 수 있어 nullable 이다
     * (경과일·세차 횟수 계산에서는 제외된다).
     */
    val performedAt: String?,

    val cost: Int? = null,
    /** 셀프세차·자동세차·손세차·실내 클리닝 등 */
    val method: String? = null,
    val place: String? = null,
    val memo: String? = null
)

/** 기록 목록·통계용 조회 결과 (항목 이름 포함) */
data class CareRecordRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "careItemId") val careItemId: Long,
    @ColumnInfo(name = "itemName") val itemName: String,
    @ColumnInfo(name = "performedAt") val performedAt: String?,
    @ColumnInfo(name = "cost") val cost: Int?,
    @ColumnInfo(name = "method") val method: String?,
    @ColumnInfo(name = "place") val place: String?,
    @ColumnInfo(name = "memo") val memo: String?
)
