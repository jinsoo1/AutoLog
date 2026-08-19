package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.DrivingPace
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.PredictionBasis
import com.jsworld.android.autolog.domain.model.buildMaintenancePredictions
import com.jsworld.android.autolog.domain.model.estimateDrivingPace
import com.jsworld.android.autolog.domain.model.predictMaintenanceDate
import com.jsworld.android.autolog.domain.model.predictedDateLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * 정비 시기 예측 — 틀린 날짜를 자신 있게 보여주면 사용자가 그걸 믿고 정비를 미룬다.
 * "계산할 수 없으면 null" 을 여기서 잠근다.
 */
class MaintenancePredictionTest {

    private val today = LocalDate.of(2026, 8, 18)
    private val current = YearMonth.of(2026, 8)

    private fun month(m: Int, km: Int?) = MonthlyExpense(
        month = YearMonth.of(2026, m),
        fuelCost = 0, maintenanceCost = 0, careCost = 0,
        missingCostCount = 0, drivenKm = km
    )

    private fun item(
        name: String,
        remainingKm: Int? = null,
        remainingDays: Long? = null,
        hasHistory: Boolean = true,
        settingId: Long = 1L
    ) = MaintenanceUiModel(
        settingId = settingId, name = name, status = MaintenanceStatus.NORMAL,
        remainingText = "", hasHistory = hasHistory,
        remainingKm = remainingKm, remainingDays = remainingDays
    )

    /* ── 주행 페이스 ── */

    @Test
    fun `진행 중인 이번 달은 평균에서 뺀다`() {
        // 8월은 아직 18일치라 넣으면 페이스가 확 낮아진다
        val pace = estimateDrivingPace(
            listOf(month(5, 1200), month(6, 1300), month(7, 1100), month(8, 300)),
            current
        )!!
        assertEquals(1200, pace.monthlyKm) // (1200+1300+1100)/3 — 최근 3개월
        assertEquals(3, pace.monthsUsed)
    }

    @Test
    fun `완전한 달이 하나뿐이면 페이스라고 부르지 않는다`() {
        assertNull(estimateDrivingPace(listOf(month(7, 1100), month(8, 300)), current))
    }

    @Test
    fun `주행거리를 계산할 수 없는 달은 평균에서 빠진다`() {
        // drivenKm = null 은 "기준점이 없어 모른다"는 뜻이지 0km 가 아니다
        val pace = estimateDrivingPace(
            listOf(month(5, 1000), month(6, null), month(7, 2000)),
            current
        )!!
        assertEquals(1500, pace.monthlyKm)
        assertEquals(2, pace.monthsUsed)
    }

    /* ── 항목별 예측 ── */

    @Test
    fun `거리와 기간 중 먼저 오는 쪽을 쓴다`() {
        val pace = DrivingPace(monthlyKm = 1200, monthsUsed = 3)

        // 4,800km 남음 = 120일 뒤 / 기간은 200일 남음 → 거리가 먼저
        val (dateA, basisA) = predictMaintenanceDate(4_800, 200L, pace, today)!!
        assertEquals(PredictionBasis.DISTANCE, basisA)
        assertEquals(today.plusDays(120), dateA)

        // 같은 거리인데 기간이 30일밖에 안 남았으면 기간이 먼저
        val (dateB, basisB) = predictMaintenanceDate(4_800, 30L, pace, today)!!
        assertEquals(PredictionBasis.PERIOD, basisB)
        assertEquals(today.plusDays(30), dateB)
    }

    @Test
    fun `페이스를 모르면 거리로는 계산하지 않는다`() {
        // 기간 주기가 없으면 아무것도 못 낸다
        assertNull(predictMaintenanceDate(4_800, null, null, today))
        // 기간 주기가 있으면 그건 페이스와 무관하게 계산된다
        val (date, basis) = predictMaintenanceDate(4_800, 90L, null, today)!!
        assertEquals(PredictionBasis.PERIOD, basis)
        assertEquals(today.plusDays(90), date)
    }

    @Test
    fun `기록 없는 항목과 이미 지난 항목은 예측하지 않는다`() {
        val pace = DrivingPace(monthlyKm = 1200, monthsUsed = 3)
        val result = buildMaintenancePredictions(
            listOf(
                item("기록 없음", remainingKm = 3_000, hasHistory = false, settingId = 1),
                item("이미 초과", remainingKm = -500, settingId = 2),
                item("기간 초과", remainingDays = -3L, settingId = 3),
                item("정상", remainingKm = 3_000, settingId = 4)
            ),
            pace, today
        )
        assertEquals(listOf("정상"), result.map { it.name })
    }

    @Test
    fun `가까운 순으로 정렬하고 세 개까지만`() {
        val pace = DrivingPace(monthlyKm = 1500, monthsUsed = 3)
        val result = buildMaintenancePredictions(
            listOf(
                item("먼 것", remainingKm = 30_000, settingId = 1),
                item("가까운 것", remainingKm = 1_500, settingId = 2),
                item("중간", remainingKm = 6_000, settingId = 3),
                item("네 번째", remainingKm = 45_000, settingId = 4)
            ),
            pace, today
        )
        assertEquals(listOf("가까운 것", "중간", "먼 것"), result.map { it.name })
    }

    /* ── 표시 ── */

    @Test
    fun `예측 해상도는 초·중순·말, 해가 바뀌면 달만`() {
        assertEquals("8월 중순", predictedDateLabel(LocalDate.of(2026, 8, 15), today))
        assertEquals("11월 초", predictedDateLabel(LocalDate.of(2026, 11, 3), today))
        assertEquals("12월 말", predictedDateLabel(LocalDate.of(2026, 12, 28), today))
        assertEquals("내년 6월", predictedDateLabel(LocalDate.of(2027, 6, 10), today))
        assertEquals("2029년 3월", predictedDateLabel(LocalDate.of(2029, 3, 10), today))
    }
}
