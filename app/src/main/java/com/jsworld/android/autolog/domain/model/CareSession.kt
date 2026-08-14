package com.jsworld.android.autolog.domain.model

/**
 * 하루치 세차·관리 묶음.
 *
 * 저장은 항목별로 쪼개서 한다 — 주기 계산("왁스 45일마다")이 항목 단위라 그래야 정확하다.
 * 하지만 사용자에게 그건 "8월 5일에 세차 1건"이고, 왁스·실내 클리닝은 그 안에 든 내용이다.
 * 그래서 목록에서는 같은 날짜의 기록을 한 건으로 묶고, 눌러야 무엇을 했는지 펼친다.
 */
data class CareSession(
    /** 목록 key — 날짜가 있으면 날짜, 날짜 없는 옛 기록은 묶지 않고 기록 자체를 키로 쓴다 */
    val key: String,
    val performedAt: String?,
    /** 기본 세차가 맨 앞, 나머지는 저장된 순서 */
    val records: List<CareRecord>
) {
    /** 대표 기록 — 세차가 있으면 세차, 없으면 그날의 첫 기록 */
    val primary: CareRecord get() = records.first()

    val itemNames: List<String> get() = records.map { it.itemName }

    val includesWash: Boolean get() = records.any { it.itemName == BASE_WASH_NAME }

    /** 묶음 총비용. 비용을 하나도 안 적었으면 null — 0원으로 지어내지 않는다 */
    val totalCost: Int? get() = records.mapNotNull { it.cost }.takeIf { it.isNotEmpty() }?.sum()
}

/**
 * 기록들을 날짜별 묶음으로 만든다(최신순).
 *
 * 같은 날 따로 남긴 기록(넛지로 왁스만 추가한 경우 등)도 한 묶음이 된다 —
 * 사용자 머릿속에서도 "그날 한 것들"이 한 덩어리이기 때문이다.
 */
fun buildCareSessions(careRecords: List<CareRecord>): List<CareSession> =
    careRecords
        .groupBy { it.performedAt ?: "record-${it.id}" }
        .map { (key, group) ->
            CareSession(
                key = key,
                performedAt = group.first().performedAt,
                records = group.sortedWith(
                    compareByDescending<CareRecord> { it.itemName == BASE_WASH_NAME }
                        .thenBy { it.id }
                )
            )
        }
        // 날짜 없는 기록은 맨 뒤로("" 가 가장 작다)
        .sortedWith(
            compareByDescending<CareSession> { it.performedAt ?: "" }
                .thenByDescending { it.primary.id }
        )
