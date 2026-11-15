package com.jsworld.android.autolog.domain.model

import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.LocalizedNames
import com.jsworld.android.autolog.core.ui.UiText
import kotlin.math.abs
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
        title = UiText.Res(R.string.year_empty_title),
        subtitle = UiText.Res(R.string.year_empty_subtitle),
        tone = NarrativeTone.EMPTY
    )

    // 횟수는 적어도 금액이 크면 그 해의 사건이다 — 잦은 수리보다 먼저 판정.
    maxRepairCost >= BIG_REPAIR_WON -> ReportNarrative(
        title = UiText.Res(R.string.year_big_repair_title),
        subtitle = UiText.Res(R.string.year_big_repair_subtitle, maxRepairCost),
        tone = NarrativeTone.SPIKE
    )

    repairCount >= 2 -> ReportNarrative(
        title = UiText.Res(R.string.year_frequent_repair_title),
        subtitle = UiText.Res(R.string.year_frequent_repair_subtitle, repairCount),
        tone = NarrativeTone.SPIKE
    )

    prevYearTotal != null &&
        yearTotal - prevYearTotal >= 300_000L &&
        yearTotal >= prevYearTotal + prevYearTotal * 3 / 10 -> ReportNarrative(
        title = UiText.Res(R.string.year_spend_up_title),
        subtitle = UiText.Res(R.string.year_spend_up_subtitle, yearTotal - prevYearTotal),
        tone = NarrativeTone.SPIKE
    )

    isCompleteYear && prevYearTotal != null &&
        yearTotal <= prevYearTotal * 8 / 10 -> ReportNarrative(
        title = UiText.Res(R.string.year_thrifty_title),
        subtitle = UiText.Res(R.string.year_thrifty_subtitle, prevYearTotal - yearTotal),
        tone = NarrativeTone.CALM
    )

    else -> ReportNarrative(
        title = UiText.Res(R.string.year_calm_title),
        subtitle = UiText.Res(R.string.year_calm_subtitle),
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

fun topSpendItems(
    yearMaint: List<CarMaintenanceRecord>,
    yearCare: List<CareRecord> = emptyList(),
    limit: Int = 5
): List<TopSpendItem> {
    val maint = yearMaint
        .filter { (it.cost ?: 0) > 0 }
        .groupBy { it.typeName }
        .map { (name, records) ->
            TopSpendItem(
                name = name,
                count = records.size,
                total = records.sumOf { it.cost!!.toLong() },
                isCare = false
            )
        }
    val care = yearCare
        .filter { (it.cost ?: 0) > 0 }
        .groupBy { it.itemName }
        .map { (name, records) ->
            TopSpendItem(
                name = name,
                count = records.size,
                total = records.sumOf { it.cost!!.toLong() },
                isCare = true
            )
        }
    return (maint + care).sortedByDescending { it.total }.take(limit)
}

/** 올해의 기록 한 줄 */
data class YearHighlight(val label: UiText, val value: UiText)

fun buildYearHighlights(
    yearMonths: List<MonthlyExpense>,
    yearFuel: List<FuelRecord>,
    yearMaint: List<CarMaintenanceRecord>,
    prevYearFuel: List<FuelRecord> = emptyList(),
    yearCare: List<CareRecord> = emptyList()
): List<YearHighlight> = buildList {
    // 올해 주행 — 숫자만. 지구·드럼통 같은 비유는 1.5.x 리뷰("필요 없는 텍스트")로 뺐다.
    val drivenKm = yearMonths.mapNotNull { it.drivenKm }.sum()
    if (drivenKm > 0) {
        add(
            YearHighlight(
                UiText.Res(R.string.hl_driven_label),
                UiText.Res(R.string.hl_driven_value, drivenKm)
            )
        )
    }

    // 올해의 큰 지출 — 단일 기록 중 최대 (정비·수리·세차 통틀어)
    val bigCandidates = buildList {
        yearMaint.filter { (it.cost ?: 0) > 0 }.forEach {
            add(Triple(it.typeName, it.cost!!, it.serviceDate))
        }
        yearCare.filter { (it.cost ?: 0) > 0 }.forEach {
            add(Triple(it.itemName, it.cost!!, it.performedAt))
        }
    }
    bigCandidates.maxByOrNull { it.second }?.let { (name, cost, date) ->
        val month = date?.substring(5, 7)?.toIntOrNull()
        val prefix: UiText = month?.let { UiText.Res(R.string.month_prefix, it) } ?: UiText.Empty
        add(
            YearHighlight(
                UiText.Res(R.string.hl_big_spend_label),
                UiText.Res(R.string.hl_big_spend_value, prefix, LocalizedNames.item(name), cost)
            )
        )
    }

    // 가장 많이 달린 달
    yearMonths.filter { (it.drivenKm ?: 0) > 0 }.maxByOrNull { it.drivenKm!! }?.let {
        add(
            YearHighlight(
                UiText.Res(R.string.hl_most_driven_label),
                UiText.Res(R.string.hl_most_driven_value, it.month.monthValue, it.drivenKm!!)
            )
        )
    }

    // 가장 알뜰했던 달 — 지출이 있었던 달이 둘 이상일 때만 의미가 있다
    val spentMonths = yearMonths.filter { it.total > 0L }
    if (spentMonths.size >= 2) {
        val thrifty = spentMonths.minBy { it.total }
        add(
            YearHighlight(
                UiText.Res(R.string.hl_thrifty_label),
                UiText.Res(R.string.hl_thrifty_value, thrifty.month.monthValue, thrifty.total)
            )
        )
    }

    // 단골 주유소/충전소
    yearFuel.mapNotNull { it.station?.trim()?.takeIf { s -> s.isNotBlank() } }
        .groupingBy { it }.eachCount()
        .maxByOrNull { it.value }
        ?.takeIf { it.value >= 2 }
        ?.let {
            add(
                YearHighlight(
                    UiText.Res(R.string.hl_regular_label),
                    UiText.Res(R.string.hl_regular_value, it.key, it.value)
                )
            )
        }

    // 주유량/충전량
    val liters = yearFuel.filter { !it.unit.isElectric }.mapNotNull { it.quantity }.sum()
    if (liters > 0) {
        add(
            YearHighlight(
                UiText.Res(R.string.hl_fuel_label),
                UiText.Res(R.string.hl_fuel_value, liters)
            )
        )
    }
    val kwh = yearFuel.filter { it.unit.isElectric }.mapNotNull { it.quantity }.sum()
    if (kwh > 0) {
        add(
            YearHighlight(
                UiText.Res(R.string.hl_charge_label),
                UiText.Res(R.string.hl_charge_value, kwh)
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
                    UiText.Res(R.string.hl_price_label, dominantUnit.actionLabel),
                    UiText.Res(
                        R.string.hl_price_value,
                        dominantUnit.symbol,
                        abs(diff),
                        UiText.Res(if (diff < 0) R.string.hl_price_cheaper else R.string.hl_price_pricier)
                    )
                )
            )
        }
    }

    // 관리 기록 요약 — 세차는 별도 테이블이라 그대로 센다
    val cares = yearCare.size
    val repairs = yearMaint.count { it.isRepair }
    val maints = yearMaint.size - repairs
    val parts = buildList<UiText> {
        if (maints > 0) add(UiText.Res(R.string.count_maintenance, maints))
        if (repairs > 0) add(UiText.Res(R.string.count_repair, repairs))
        if (cares > 0) add(UiText.Res(R.string.count_care, cares))
    }
    if (parts.isNotEmpty()) add(YearHighlight(UiText.Res(R.string.hl_records_label), UiText.Joined(parts)))
}

/** 평균 단가 — 수량·금액이 모두 있는 기록으로만 */
fun averageUnitPrice(records: List<FuelRecord>): Int? {
    val priced = records.filter { it.quantity != null && it.amount != null }
    val totalQty = priced.sumOf { it.quantity!! }
    if (totalQty <= 0) return null
    return (priced.sumOf { it.amount!!.toDouble() } / totalQty).roundToInt()
}
