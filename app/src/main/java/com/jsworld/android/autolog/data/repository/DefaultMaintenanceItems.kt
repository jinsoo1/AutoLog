package com.jsworld.android.autolog.data.repository

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

        // 🧼 기타 소모품/관리
        "실내/외 세차(관리)" to Pair(null, 1),
        "코팅/왁스(관리)" to Pair(null, 6),

        // 🚗 검사/법정
        "정기점검(정비소 점검)" to Pair(null, 12),
        "종합검사/정기검사(국가검사)" to Pair(null, 24),

        // 🪛 벨트류(차종 따라 다름)
        "보조벨트(팬벨트) 점검/교환" to Pair(60_000, 48),
        "타이밍벨트(벨트식 엔진만)" to Pair(100_000, 60)
    )
}