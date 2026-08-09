package com.jsworld.android.autolog.domain.model

/**
 * 연료 타입별 정비 항목 적합성 판정.
 *
 * 확실히 해당 없는 항목만 숨기는 보수적 규칙을 쓴다(애매하면 표시).
 * - 전기/수소: 내연기관 전용 항목(엔진오일·점화·연료필터·미션오일·벨트류 등) 제외
 *   (냉각수·워터펌프·디퍼런셜 등은 전기차에도 있을 수 있어 유지)
 * - 디젤: 점화계(점화플러그/코일) 제외 — 디젤 엔진은 압축착화라 점화플러그가 없다
 * - 연료 미설정/기타: 전부 표시
 */
fun isItemApplicableToFuel(itemName: String, fuelType: String?): Boolean {
    val fuel = fuelType?.trim().orEmpty()
    if (fuel.isEmpty() || fuel == "기타") return true

    val n = itemName.replace(" ", "")

    // 내연기관 전용(전기/수소차에는 없음)
    val engineOnly = listOf(
        "엔진오일", "에어클리너", "흡기", "연료필터", "PCV", "스로틀",
        "점화", "알터네이터", "스타터",
        "미션오일", "파워스티어링", "보조벨트", "팬벨트", "타이밍벨트"
    )
    // 점화계(디젤 엔진에는 없음)
    val sparkOnly = listOf("점화")

    val evLike = fuel == "전기" || fuel == "수소"
    if (evLike && engineOnly.any { n.contains(it) }) return false
    if (fuel == "디젤" && sparkOnly.any { n.contains(it) }) return false
    return true
}

/**
 * 세차·코팅류 "관리" 항목 판정.
 *
 * 주기 없는 항목은 기본이 "수리"인데, 세차는 수리가 아니다.
 * 이름으로 구분해 타임라인 배지·상세 문구를 "관리"로 바꾼다.
 */
fun isCareItemName(itemName: String): Boolean {
    val n = itemName.replace(" ", "")
    return listOf("세차", "코팅", "왁스", "광택", "세정").any { n.contains(it) }
}
