package com.jsworld.android.autolog.domain.model

/**
 * 주유(충전) 기록.
 *
 * 연비/전비는 다루지 않는다. 대신 "이전 기록 이후 주행거리"와 월 지출로 사용성을 만든다.
 *
 * [unit] 은 **차량이 아니라 기록마다** 붙는다. 플러그인 하이브리드는 한 차량이
 * 주유(L)와 충전(kWh)을 모두 하기 때문이다.
 */
data class FuelRecord(
    val id: Long,
    val carId: Long,
    /** yyyy-MM-dd */
    val filledAt: String,
    val mileage: Int?,
    val amount: Int?,
    val quantity: Double?,
    val unitPrice: Int?,
    val unit: FuelUnit,
    val station: String?,
    val memo: String?,
    val photoPath: String?
)

/** 월별 지출. [month] 는 "yyyy-MM". 에너지 종류별로 나뉘어 나온다. */
data class MonthlyFuelCost(
    val month: String,
    val unit: FuelUnit,
    val totalAmount: Int,
    val totalQuantity: Double
)

/**
 * 에너지 종류. 화면 문구(주유/충전)와 단위가 여기서 갈린다.
 */
enum class FuelUnit(val symbol: String) {
    LITER("L"),
    KWH("kWh"),

    /** 수소는 kg 단위로 충전한다. */
    KG("kg");

    /** 전기 충전인지(전용 색·아이콘 판단에 쓴다) */
    val isElectric: Boolean get() = this == KWH

    /** "주유" / "충전" */
    val actionLabel: String get() = if (this == LITER) "주유" else "충전"

    /** "주유비" / "충전비" */
    val costLabel: String get() = if (this == LITER) "주유비" else "충전비"

    /** "주유소" / "충전소" */
    val placeLabel: String get() = if (this == LITER) "주유소" else "충전소"

    /** "주유량" / "충전량" */
    val quantityLabel: String get() = if (this == LITER) "주유량" else "충전량"

    companion object {
        /**
         * 차량이 쓸 수 있는 에너지 종류.
         *
         * - 순수 전기 → 충전(kWh)만
         * - 수소 → 충전(kg)만
         * - **플러그인 하이브리드 → 주유(L) + 충전(kWh) 둘 다**
         * - 일반 하이브리드는 외부 충전을 하지 않으므로 주유만
         * - 그 외(가솔린·디젤·LPG·기타·미설정) → 주유만
         *
         * 목록의 첫 번째가 기본값이다.
         */
        fun supportedUnits(fuelType: String?): List<FuelUnit> {
            val value = fuelType?.trim()?.replace(" ", "").orEmpty()
            if (value.isEmpty()) return listOf(LITER)

            val isPlugIn = value.contains("플러그인") ||
                    value.contains("PHEV", ignoreCase = true)
            if (isPlugIn) return listOf(LITER, KWH)

            if (value.contains("수소")) return listOf(KG)

            val isHybrid = value.contains("하이브리드") ||
                    value.contains("HEV", ignoreCase = true)
            val mentionsElectric = value.contains("전기") ||
                    value.contains("EV", ignoreCase = true)

            // "전기"가 들어가도 하이브리드면 외부 충전을 가정하지 않는다.
            if (mentionsElectric && !isHybrid) return listOf(KWH)

            return listOf(LITER)
        }

        /** 차량의 기본 에너지 종류(입력 화면 초기값) */
        fun defaultFor(fuelType: String?): FuelUnit = supportedUnits(fuelType).first()

        /**
         * **화면에 보여줄** 에너지 종류.
         *
         * [supportedUnits] 는 "지금 무엇을 넣을 수 있나"(입력 기준)이고,
         * 이 함수는 "무엇을 보여줘야 하나"(표시 기준)다. 둘은 다를 수 있다.
         *
         * 연료 타입을 바꿔도 기존 기록은 지워지지 않으므로,
         * 예컨대 플러그인 하이브리드에서 전기로 바꾸면 과거 주유 기록이 남는다.
         * 그때 표시 기준을 차량 설정만으로 잡으면 그 주유 기록이
         * 합계에서 빠지거나 라벨이 어긋난다(실제로 났던 버그).
         *
         * 주유가 먼저 오도록 정렬한다.
         */
        fun displayUnits(recordUnits: Collection<FuelUnit>, fuelType: String?): List<FuelUnit> =
            (recordUnits + supportedUnits(fuelType))
                .distinct()
                .sortedBy { if (it.isElectric) 1 else 0 }

        /** 이 차량이 전기 충전을 하는가 — 안 하면 화면에서 전기 관련 표시를 전부 뺀다. */
        fun usesElectricCharging(fuelType: String?): Boolean =
            supportedUnits(fuelType).any { it.isElectric }

        fun fromSymbol(symbol: String): FuelUnit =
            entries.firstOrNull { it.symbol == symbol } ?: LITER
    }
}

/**
 * 과거 날짜로 기록을 넣을 때의 주행거리 제안.
 *
 * 오늘 값(차량 현재 주행거리)을 그대로 두면 7월 기록에 8월 주행거리가 저장돼
 * 월별 주행거리 계산이 통째로 어긋난다. 대신 그 날짜 앞뒤 이웃 기록 사이에
 * 들어가는 값을 제안한다 — 직전 기록 +1km, 직전이 없으면 다음 기록 -1km.
 *
 * 제안일 뿐이라 정확하진 않지만, 순서(단조 증가)는 절대 깨지 않는다.
 * 이웃이 하나도 없으면 null — 근거 없는 숫자를 지어내지 않고 비워둔다.
 */
fun suggestBackdatedMileage(prevMileage: Int?, nextMileage: Int?): Int? = when {
    // 앞뒤가 다 있으면 그 사이로 — 직전 +1이 다음을 넘지 않게 잘라준다
    prevMileage != null && nextMileage != null ->
        (prevMileage + 1).coerceAtMost(nextMileage)
    prevMileage != null -> prevMileage + 1
    nextMileage != null -> (nextMileage - 1).coerceAtLeast(0)
    else -> null
}
