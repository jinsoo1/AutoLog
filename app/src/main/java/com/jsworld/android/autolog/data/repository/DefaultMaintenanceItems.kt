package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.domain.model.MaintenanceStarterPack

object DefaultMaintenanceItems {

    val items = listOf(
        // 🛢 엔진/필터류
        "엔진오일" to Pair(10_000, 6),
        "엔진오일 필터" to Pair(10_000, 6),
        "에어컨(캐빈) 필터" to Pair(15_000, 12),
        "에어클리너(엔진흡기 필터)" to Pair(30_000, 24),
        "연료필터" to Pair(60_000, 48),
        "PCV 밸브(점검/교환)" to Pair(60_000, 48),
        "스로틀바디 청소" to Pair(50_000, 36),
        "흡기/밸브 카본 청소(선택)" to Pair(80_000, 48),

        // 🔥 점화/전기
        "점화플러그" to Pair(80_000, 48),
        "점화코일(점검/교환)" to Pair(null, 60),
        "배터리" to Pair(50_000, 36),
        "발전기/알터네이터 점검" to Pair(null, 36),
        "스타터 모터 점검" to Pair(null, 36),

        // 🧊 냉각/에어컨
        "냉각수(부동액)" to Pair(60_000, 36),
        "라디에이터 캡/호스 점검" to Pair(null, 12),
        "워터펌프(점검)" to Pair(null, 60),
        "에어컨 냉매 점검/보충" to Pair(null, 24),

        // 🧯 브레이크
        "브레이크패드" to Pair(50_000, 36),
        "브레이크 디스크(로터)" to Pair(100_000, 60),
        "브레이크오일" to Pair(40_000, 24),
        "브레이크 라인/누유 점검" to Pair(null, 12),

        // 🛞 타이어/하체
        "타이어 교체" to Pair(40_000, 36),
        "타이어 위치교환" to Pair(10_000, 12),
        "휠 얼라인먼트 점검" to Pair(null, 12),
        "타이어 공기압 점검" to Pair(null, 1), // 매달 알림용(원치 않으면 유저가 끄면 됨)
        "서스펜션/부싱/조인트 점검" to Pair(null, 12),
        "하부(누유/부식) 점검" to Pair(null, 12),

        // ⚙️ 미션/구동계 오일류
        "미션오일(AT/CVT/DCT)" to Pair(40_000, 24),
        "미션오일 필터(있는 경우)" to Pair(80_000, 48),
        "디퍼런셜 오일(후륜/4륜)" to Pair(60_000, 36),
        "트랜스퍼 케이스 오일(4WD)" to Pair(60_000, 36),
        "파워스티어링 오일(유압식)" to Pair(40_000, 24),

        // 🧻 와이퍼/조명
        "와이퍼 블레이드" to Pair(null, 12),
        "워셔액 보충" to Pair(null, 1),
        "전조등/미등/브레이크등 점검" to Pair(null, 6),

        // 세차·코팅류는 정비 항목이 아니다 — [DefaultCareItems] 로 분리돼
        // 세차 허브에서 자체 주기(세차 N회마다 / N개월마다)로 관리한다.

        // 🚗 검사/법정
        "정기점검(정비소 점검)" to Pair(null, 12),
        "종합검사/정기검사(국가검사)" to Pair(null, 24),

        // 🪛 벨트류(차종 따라 다름)
        "보조벨트(팬벨트) 점검/교환" to Pair(60_000, 48),
        "타이밍벨트(벨트식 엔진만)" to Pair(100_000, 60)
    )


    /**
     * 온보딩 추천 팩 — [MaintenanceStarterPack] 단계별로 켤 항목.
     *
     * ⚠️ 이름은 위 items 의 이름과 글자까지 정확히 일치해야 한다(이름으로 타입을 찾는다).
     *    일치 여부는 MaintenanceStarterPackTest 가 검증한다.
     * 연료 타입 필터(isItemApplicableToFuel)는 적용 시점에 걸리므로
     * 여기서는 내연기관 기준 전체 목록을 적는다.
     */
    val lightPack = listOf(
        "엔진오일",
        "엔진오일 필터",
        "에어컨(캐빈) 필터",
        "타이어 교체",
        "배터리",
        "와이퍼 블레이드"
    )

    /** 꼼꼼하게 = 가볍게 + 아래 항목 */
    val standardExtra = listOf(
        "에어클리너(엔진흡기 필터)",
        "브레이크패드",
        "브레이크오일",
        "냉각수(부동액)",
        "미션오일(AT/CVT/DCT)",
        "타이어 위치교환",
        "점화플러그",
        "정기점검(정비소 점검)"
    )

    val standardPack: List<String> get() = lightPack + standardExtra

    /** 빈틈없이 = 기본 항목 전부 */
    val fullPack: List<String> get() = items.map { it.first }
}

/**
 * 세차·관리 기본 항목 — 정비 항목과 분리되어 세차 허브에서만 다뤄진다.
 *
 * 주기는 시딩하지 않는다. "세차 3회마다 실내 클리닝"처럼 사람마다 완전히 다르고,
 * 기본값을 넣으면 켜자마자 초과로 보이기 때문이다(세차 허브에서 직접 정한다).
 * 첫 항목인 '세차'가 세차 횟수 주기의 카운터 기준이 된다.
 */
object DefaultCareItems {

    /** 카운터 기준이 되는 기본 세차 항목 이름 */
    const val WASH = "세차"

    val items = listOf(
        WASH,          // 실외(기본) 세차 — 경과일·세차 횟수 카운터의 기준
        "실내 세차",
        "왁스 코팅",
        "유리막 코팅",
        "발수 코팅",
        "광택"
    )
}
