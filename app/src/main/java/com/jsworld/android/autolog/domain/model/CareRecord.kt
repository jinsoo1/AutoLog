package com.jsworld.android.autolog.domain.model

/**
 * 세차·관리 기록. 정비 기록과 별개의 테이블(care_records)에서 온다.
 * 정비와 달리 주행거리가 없고 방식(셀프/자동/손세차 등)이 있다.
 */
data class CareRecord(
    val id: Long,
    val careItemId: Long,
    val itemName: String,
    /** yyyy-MM-dd. 이관된 옛 기록엔 없을 수 있다 */
    val performedAt: String?,
    val cost: Int?,
    val method: String?,
    val place: String?,
    val memo: String?
)
