package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.ExpenseReportCalc
import com.jsworld.android.autolog.domain.model.MileagePoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

/**
 * 주행거리를 잘못 올렸다가 되돌렸을 때 리포트가 따라와야 한다.
 *
 * 월 주행거리는 관측점의 **최댓값**으로 계산한다(주행거리계는 거꾸로 가지 않으므로
 * 낮은 오타를 막는 방어다). 그래서 잘못 올린 관측점을 지우지 않으면 되돌려도
 * 그 값이 계속 최댓값으로 남아 그 달이 부풀고 다음 달은 0으로 눌린다.
 * 실제로 겪은 문제라 계산과 정리 규칙을 함께 고정해 둔다.
 */
class MileageCorrectionTest {

    private val aug = YearMonth.of(2026, 8)
    private val sep = YearMonth.of(2026, 9)

    /** 7월 말 기준점 + 8월 실제 주행 */
    private val base = listOf(
        MileagePoint("2026-07-28", 41_000),
        MileagePoint("2026-08-20", 42_180)
    )

    @Test
    fun `정상적인 달은 그대로 계산된다`() {
        assertEquals(1_180, ExpenseReportCalc.drivenKmIn(aug, base.sortedBy { it.date }))
    }

    @Test
    fun `잘못 올린 관측점이 남아 있으면 그 달이 부풀고 다음 달이 눌린다`() {
        // 테스트하려고 47,000 으로 올렸다가 42,180 으로 되돌린 상태 (행이 둘 다 남음)
        val dirty = (base + listOf(
            MileagePoint("2026-08-26", 47_000),
            MileagePoint("2026-08-26", 42_180)
        )).sortedBy { it.date }

        // 8월이 1,180km 가 아니라 6,000km 로 부푼다
        assertEquals(6_000, ExpenseReportCalc.drivenKmIn(aug, dirty))

        // 그리고 9월 기준선이 47,000 이 되어, 실제로 800km 를 달려도 0 으로 눌린다
        val withSep = (dirty + MileagePoint("2026-09-10", 42_980)).sortedBy { it.date }
        assertEquals(0, ExpenseReportCalc.drivenKmIn(sep, withSep))
    }

    @Test
    fun `낮춘 값보다 높은 관측점을 지우면 원래대로 돌아온다`() {
        // deleteHistoriesAbove(carId, 42_180) 이 한 일과 같다
        val cleaned = (base + MileagePoint("2026-08-26", 42_180))
            .filter { it.mileage <= 42_180 }
            .sortedBy { it.date }

        assertEquals(1_180, ExpenseReportCalc.drivenKmIn(aug, cleaned))

        val withSep = (cleaned + MileagePoint("2026-09-10", 42_980)).sortedBy { it.date }
        assertEquals(800, ExpenseReportCalc.drivenKmIn(sep, withSep))
    }

    @Test
    fun `현재 주행거리보다 높은 관측점을 버리면 저절로 낫는다`() {
        // 실제로 겪은 데이터: 현재 41,847km 인데 이력에 45,810~46,810 이 네 줄 남아 있었다.
        // 사용자가 주행거리를 다시 저장할 때까지 기다리지 않고, 읽는 쪽에서 걸러 바로 고친다.
        val currentMileage = 41_847
        val dirty = listOf(
            MileagePoint("2026-07-28", 41_000),
            MileagePoint("2026-08-20", 41_707),
            MileagePoint("2026-08-27", 45_810),
            MileagePoint("2026-08-27", 46_810),
            MileagePoint("2026-08-27", 41_847)
        ).sortedBy { it.date }

        // 거르기 전 — 8월이 5,810km 로 부푼다
        assertEquals(5_810, ExpenseReportCalc.drivenKmIn(aug, dirty))

        // ExpenseReportRepositoryImpl 이 하는 일과 같다
        val cleaned = dirty.filter { it.mileage <= currentMileage }
        assertEquals(847, ExpenseReportCalc.drivenKmIn(aug, cleaned))
    }

    @Test
    fun `실제로 겪은 데이터 - 5818km 가 855km 로 돌아온다`() {
        // 기기에서 그대로 꺼낸 값이다. 화면에 "이 달 주행 5,818km"로 보이던 상태.
        val currentMileage = 41_847
        val points = listOf(
            MileagePoint("2026-07-31", 40_992),
            MileagePoint("2026-08-18", 41_699),
            MileagePoint("2026-08-21", 41_707),
            MileagePoint("2026-08-23", 41_828),
            MileagePoint("2026-08-27", 45_810),
            MileagePoint("2026-08-27", 45_840),
            MileagePoint("2026-08-27", 45_888),
            MileagePoint("2026-08-27", 46_810),
            MileagePoint("2026-08-27", 41_847)
        ).sortedBy { it.date }

        assertEquals(5_818, ExpenseReportCalc.drivenKmIn(aug, points))

        val cleaned = points.filter { it.mileage <= currentMileage }
        assertEquals(855, ExpenseReportCalc.drivenKmIn(aug, cleaned))
    }
}
