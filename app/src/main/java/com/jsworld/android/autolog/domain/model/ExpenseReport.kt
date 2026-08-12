package com.jsworld.android.autolog.domain.model

import java.time.YearMonth

/**
 * 한 달치 지출 요약. 리포트 탭과 홈 요약 카드가 쓴다.
 *
 * 금액이 입력되지 않은 정비 기록은 합계에서 빠진다 — 대신 [missingCostCount]로
 * 몇 건이 빠졌는지 화면에 밝힌다(총액이 실제보다 적어 보이는 이유를 숨기지 않기).
 */
data class MonthlyExpense(
    val month: YearMonth,
    /** 주유·충전 지출 */
    val fuelCost: Long,
    /** 정비·수리 지출 (주기 정비 + 일회성 수리) */
    val maintenanceCost: Long,
    /** 세차·관리 지출 */
    val careCost: Long,
    /** 금액 미입력으로 합계에서 빠진 정비 기록 수 */
    val missingCostCount: Int,
    /** 이 달에 달린 거리(km). 주행거리 기록이 부족해 계산할 수 없으면 null */
    val drivenKm: Int?
) {
    val total: Long get() = fuelCost + maintenanceCost + careCost

    /** km당 유지비(원). 주행거리를 모르거나 0이면 null — 숫자를 지어내지 않는다 */
    val costPerKm: Int? get() = drivenKm?.takeIf { it > 0 }?.let { (total / it).toInt() }
}

/** 정비 기록 원천 행 — DAO 조회 결과를 집계 함수에 넘기는 형태 */
data class ExpenseCostRow(
    val month: String, // "yyyy-MM"
    val typeName: String,
    val cost: Int?
)

/** 주행거리 관측점 — 주유 기록·정비 기록·주행거리 업데이트에서 모은다 */
data class MileagePoint(
    val date: String, // "yyyy-MM-dd"
    val mileage: Int
)

/**
 * 월별 지출 리포트 집계 — 전부 순수 함수라 단위 테스트로 지킨다.
 */
object ExpenseReportCalc {

    /**
     * 원천 데이터를 월별 지출 목록으로 만든다.
     * 가장 오래된 기록의 달부터 [current]까지 빈 달도 0원으로 채운다
     * (추이 차트가 끊기지 않게).
     */
    fun build(
        fuelByMonth: Map<String, Long>,
        maintenanceRows: List<ExpenseCostRow>,
        mileagePoints: List<MileagePoint>,
        current: YearMonth,
        /** 세차·관리는 별도 테이블에서 온다(월 합계 / 금액 미입력 건수) */
        careByMonth: Map<String, Long> = emptyMap(),
        careMissingByMonth: Map<String, Int> = emptyMap()
    ): List<MonthlyExpense> {
        val monthKeys = fuelByMonth.keys + maintenanceRows.map { it.month } + careByMonth.keys
        val first = monthKeys.mapNotNull { it.toYearMonthOrNull() }.minOrNull() ?: return emptyList()

        val sortedPoints = mileagePoints.sortedBy { it.date }

        val result = mutableListOf<MonthlyExpense>()
        var m = first
        while (m <= current) {
            val key = "%04d-%02d".format(m.year, m.monthValue)
            val rows = maintenanceRows.filter { it.month == key }
            val maint = rows.filter { it.cost != null }

            result += MonthlyExpense(
                month = m,
                fuelCost = fuelByMonth[key] ?: 0L,
                maintenanceCost = maint.sumOf { it.cost!!.toLong() },
                careCost = careByMonth[key] ?: 0L,
                missingCostCount = rows.count { it.cost == null } +
                    (careMissingByMonth[key] ?: 0),
                drivenKm = drivenKmIn(m, sortedPoints)
            )
            m = m.plusMonths(1)
        }
        return result
    }

    /**
     * 이 달에 달린 거리 = (달 끝까지의 최대 누적) - (달 시작 전까지의 최대 누적).
     * 달 시작 전 관측점이 없으면 기준이 없어 계산 불가(null) — 그 달에 처음
     * 기록을 시작한 경우 부풀려진 값을 내지 않기 위해서다.
     */
    fun drivenKmIn(month: YearMonth, sortedPoints: List<MileagePoint>): Int? {
        val startExclusive = "%04d-%02d-01".format(month.year, month.monthValue)
        val next = month.plusMonths(1)
        val endExclusive = "%04d-%02d-01".format(next.year, next.monthValue)

        val beforeStart = sortedPoints.filter { it.date < startExclusive }
        if (beforeStart.isEmpty()) return null

        val baseline = beforeStart.maxOf { it.mileage }
        val endMax = sortedPoints.filter { it.date < endExclusive }.maxOf { it.mileage }
        return (endMax - baseline).coerceAtLeast(0)
    }

    private fun String.toYearMonthOrNull(): YearMonth? =
        runCatching { YearMonth.parse(this) }.getOrNull()
}
