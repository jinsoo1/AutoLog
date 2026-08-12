package com.jsworld.android.autolog.domain.model

import kotlin.math.roundToInt

/**
 * 연간 리포트 전용 요약들 — 항목별 TOP, 올해의 기록, 연간 내러티브.
 * 전부 순수 함수라 단위 테스트로 지킨다.
 */

/** 한 건만으로도 "큰 수리"로 부를 금액 */
private const val BIG_REPAIR_WON = 1_000_000L

/** 연간 내러티브 — 월간과 같은 카드 UI 를 쓰되 판정 기준만 연 단위 */
fun buildYearNarrative(
    yearTotal: Long,
    prevYearTotal: Long?,
    repairCount: Int,
    /** 지나간 해인지 — 진행 중인 해에 "알뜰했다"고 단정하지 않기 위해 */
    isCompleteYear: Boolean,
    /** 그 해 단일 수리 기록의 최대 금액. 없으면 0 */
    maxRepairCost: Long = 0L
): ReportNarrative = when {
    yearTotal == 0L -> ReportNarrative(
        title = "기록이 없는 해예요",
        subtitle = "이 해엔 지출 기록이 없어요.",
        tone = NarrativeTone.EMPTY
    )

    // 횟수는 적어도 금액이 크면 그 해의 사건이다 — 잦은 수리보다 먼저 판정.
    maxRepairCost >= BIG_REPAIR_WON -> ReportNarrative(
        title = "큰 수리가 있었던 해예요",
        subtitle = "%,d원짜리 수리가 있었어요. 올해의 기록에서 확인해보세요."
            .format(maxRepairCost),
        tone = NarrativeTone.SPIKE
    )

    repairCount >= 2 -> ReportNarrative(
        title = "수리가 잦았던 해예요",
        subtitle = "일회성 수리가 ${repairCount}건 있었어요. 항목별 지출에서 확인해보세요.",
        tone = NarrativeTone.SPIKE
    )

    prevYearTotal != null &&
        yearTotal - prevYearTotal >= 300_000L &&
        yearTotal >= prevYearTotal + prevYearTotal * 3 / 10 -> ReportNarrative(
        title = "지출이 커진 해예요",
        subtitle = "작년보다 %,d원 늘었어요.".format(yearTotal - prevYearTotal),
        tone = NarrativeTone.SPIKE
    )

    isCompleteYear && prevYearTotal != null &&
        yearTotal <= prevYearTotal * 8 / 10 -> ReportNarrative(
        title = "작년보다 알뜰했던 해예요",
        subtitle = "전년보다 %,d원 줄었어요.".format(prevYearTotal - yearTotal),
        tone = NarrativeTone.CALM
    )

    else -> ReportNarrative(
        title = "잔잔하게 흘러간 해예요",
        subtitle = "큰 사건 없이 관리가 이어지고 있어요.",
        tone = NarrativeTone.CALM
    )
}

/** 항목별 연간 지출 순위 — "내 돈이 어디로 갔는지"의 최종 답 */
data class TopSpendItem(
    val name: String,
    val count: Int,
    val total: Long,
    /** 세차·관리 항목 여부 — 색 구분용 */
    val isCare: Boolean
)

fun topSpendItems(yearMaint: List<CarMaintenanceRecord>, limit: Int = 5): List<TopSpendItem> =
    yearMaint
        .filter { (it.cost ?: 0) > 0 }
        .groupBy { it.typeName }
        .map { (name, records) ->
            TopSpendItem(
                name = name,
                count = records.size,
                total = records.sumOf { it.cost!!.toLong() },
                isCare = records.any { it.isCare }
            )
        }
        .sortedByDescending { it.total }
        .take(limit)

/** 올해의 기록 한 줄 */
data class YearHighlight(val label: String, val value: String)

private const val EARTH_KM = 40_075.0
private const val DRUM_LITER = 200.0

/** 4인 가구 월평균 전기 사용량(kWh) — 충전량을 실감 나는 크기로 환산하는 기준 */
private const val HOUSEHOLD_MONTH_KWH = 350.0

fun buildYearHighlights(
    yearMonths: List<MonthlyExpense>,
    yearFuel: List<FuelRecord>,
    yearMaint: List<CarMaintenanceRecord>,
    prevYearFuel: List<FuelRecord> = emptyList()
): List<YearHighlight> = buildList {
    // 올해 주행 — 지구 환산
    val drivenKm = yearMonths.mapNotNull { it.drivenKm }.sum()
    if (drivenKm > 0) {
        val laps = drivenKm / EARTH_KM
        val lapText =
            if (laps >= 1.0) "지구 ${"%.1f".format(laps)}바퀴"
            else "지구 한 바퀴의 ${(laps * 100).roundToInt().coerceAtLeast(1)}%"
        add(YearHighlight("올해 주행", "${"%,d".format(drivenKm)}km · $lapText"))
    }

    // 올해의 큰 지출 — 단일 정비·수리 기록 중 최대
    yearMaint.filter { (it.cost ?: 0) > 0 }.maxByOrNull { it.cost!! }?.let { top ->
        val month = top.serviceDate?.substring(5, 7)?.toIntOrNull()
        val prefix = month?.let { "${it}월 " }.orEmpty()
        add(YearHighlight("올해의 큰 지출", "$prefix${top.typeName} · ${"%,d".format(top.cost)}원"))
    }

    // 가장 많이 달린 달
    yearMonths.filter { (it.drivenKm ?: 0) > 0 }.maxByOrNull { it.drivenKm!! }?.let {
        add(
            YearHighlight(
                "가장 많이 달린 달",
                "${it.month.monthValue}월 · ${"%,d".format(it.drivenKm)}km"
            )
        )
    }

    // 가장 알뜰했던 달 — 지출이 있었던 달이 둘 이상일 때만 의미가 있다
    val spentMonths = yearMonths.filter { it.total > 0L }
    if (spentMonths.size >= 2) {
        val thrifty = spentMonths.minBy { it.total }
        add(
            YearHighlight(
                "가장 알뜰했던 달",
                "${thrifty.month.monthValue}월 · ${"%,d".format(thrifty.total)}원"
            )
        )
    }

    // 단골 주유소/충전소
    yearFuel.mapNotNull { it.station?.trim()?.takeIf { s -> s.isNotBlank() } }
        .groupingBy { it }.eachCount()
        .maxByOrNull { it.value }
        ?.takeIf { it.value >= 2 }
        ?.let { add(YearHighlight("단골", "${it.key} · ${it.value}회")) }

    // 주유량/충전량 환산
    val liters = yearFuel.filter { !it.unit.isElectric }.mapNotNull { it.quantity }.sum()
    if (liters > 0) {
        add(
            YearHighlight(
                "올해 주유량",
                "${"%,.0f".format(liters)}L · 드럼통 ${"%.1f".format(liters / DRUM_LITER)}개 분량"
            )
        )
    }
    val kwh = yearFuel.filter { it.unit.isElectric }.mapNotNull { it.quantity }.sum()
    if (kwh > 0) {
        val months = kwh / HOUSEHOLD_MONTH_KWH
        val scale =
            if (months >= 1.0) "약 ${"%.1f".format(months)}달 치"
            else "약 ${(kwh / (HOUSEHOLD_MONTH_KWH / 30)).roundToInt().coerceAtLeast(1)}일 치"
        add(
            YearHighlight(
                "올해 충전량",
                "${"%,.0f".format(kwh)}kWh · 4인 가구 $scale 전기 분량"
            )
        )
    }

    // 작년 내 단가와 비교 — 올해 기록이 가장 많은 종류 기준, 2년차부터 나온다
    val dominantUnit = yearFuel.groupingBy { it.unit }.eachCount().maxByOrNull { it.value }?.key
    if (dominantUnit != null) {
        val cur = averageUnitPrice(yearFuel.filter { it.unit == dominantUnit })
        val prev = averageUnitPrice(prevYearFuel.filter { it.unit == dominantUnit })
        if (cur != null && prev != null && cur != prev) {
            val diff = cur - prev
            add(
                YearHighlight(
                    "${dominantUnit.actionLabel} 단가",
                    "작년보다 ${dominantUnit.symbol}당 ${"%,d".format(kotlin.math.abs(diff))}원 " +
                        if (diff < 0) "저렴" else "비쌈"
                )
            )
        }
    }

    // 관리 기록 요약 — 세차 항목은 주기가 없어 isRepair 로도 잡히므로 세차를 먼저 뺀다
    val cares = yearMaint.count { it.isCare }
    val repairs = yearMaint.count { it.isRepair && !it.isCare }
    val maints = yearMaint.size - repairs - cares
    val parts = buildList {
        if (maints > 0) add("정비 ${maints}건")
        if (repairs > 0) add("수리 ${repairs}건")
        if (cares > 0) add("세차·관리 ${cares}회")
    }
    if (parts.isNotEmpty()) add(YearHighlight("관리 기록", parts.joinToString(" · ")))
}

/** 평균 단가 — 수량·금액이 모두 있는 기록으로만 */
fun averageUnitPrice(records: List<FuelRecord>): Int? {
    val priced = records.filter { it.quantity != null && it.amount != null }
    val totalQty = priced.sumOf { it.quantity!! }
    if (totalQty <= 0) return null
    return (priced.sumOf { it.amount!!.toDouble() } / totalQty).roundToInt()
}
