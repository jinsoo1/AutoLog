package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.REPEAT_TAX
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.model.dDayLabel
import com.jsworld.android.autolog.domain.model.nextDueDateAfterDone
import com.jsworld.android.autolog.domain.model.sortSchedules
import com.jsworld.android.autolog.domain.model.suggestInspectionDate
import com.jsworld.android.autolog.domain.model.suggestTaxDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 날짜 일정 — 틀리면 과태료로 이어지는 숫자들이라 계산 규칙을 여기서 잠근다.
 */
class CarScheduleTest {

    private val today = LocalDate.of(2026, 8, 18)

    private fun schedule(
        due: String,
        repeat: Int? = null,
        type: ScheduleType = ScheduleType.CUSTOM,
        id: Long = 1L
    ) = CarSchedule(
        id = id, carId = 1L, type = type, title = "t",
        dueDate = due, repeatMonths = repeat, memo = null
    )

    /* ── 정기검사 제안 ── */

    @Test
    fun `정기검사는 신차 4년 뒤 첫 검사, 이후 2년 주기`() {
        // 2023년식 → 2027년 7월 (아직 안 지남)
        assertEquals(LocalDate.of(2027, 7, 1), suggestInspectionDate("2023", today))
        // 2018년식 → 2022 · 2024 · 2026(7월, 이미 지남) → 2028
        assertEquals(LocalDate.of(2028, 7, 1), suggestInspectionDate("2018", today))
    }

    @Test
    fun `연식을 모르거나 이상하면 제안하지 않는다`() {
        assertNull(suggestInspectionDate(null, today))
        assertNull(suggestInspectionDate("", today))
        assertNull(suggestInspectionDate("1900", today))   // 오타
        assertNull(suggestInspectionDate("2030", today))   // 미래 연식
    }

    /* ── 자동차세 ── */

    @Test
    fun `자동차세는 6월 16일과 12월 16일 중 다음 날짜`() {
        assertEquals(LocalDate.of(2026, 12, 16), suggestTaxDate(today))
        assertEquals(LocalDate.of(2026, 6, 16), suggestTaxDate(LocalDate.of(2026, 3, 1)))
        // 12월 20일이면 내년 6월
        assertEquals(LocalDate.of(2027, 6, 16), suggestTaxDate(LocalDate.of(2026, 12, 20)))
    }

    /* ── 완료 처리 ── */

    @Test
    fun `완료하면 원래 도래일 기준으로 다음 회차 - 날짜가 밀리지 않는다`() {
        // 12/16 세금을 12/20에 완료해도 다음은 6/16 (6/20 이 아니라)
        val done = nextDueDateAfterDone(
            schedule("2026-12-16", REPEAT_TAX),
            LocalDate.of(2026, 12, 20)
        )
        assertEquals(LocalDate.of(2027, 6, 16), done)
    }

    @Test
    fun `오래 방치한 일정은 오늘 이후가 될 때까지 굴린다`() {
        // 2024-06-16 세금(6개월)을 2026-08-18 에 완료 → 2026-12-16
        assertEquals(
            LocalDate.of(2026, 12, 16),
            nextDueDateAfterDone(schedule("2024-06-16", REPEAT_TAX), today)
        )
    }

    @Test
    fun `반복 없는 일정은 완료하면 사라진다`() {
        assertNull(nextDueDateAfterDone(schedule("2026-09-01", null), today))
        assertNull(nextDueDateAfterDone(schedule("2026-09-01", 0), today))
    }

    /* ── 목록 ── */

    @Test
    fun `가까운 순 정렬, 날짜가 깨진 행은 맨 뒤`() {
        val sorted = sortSchedules(
            listOf(
                schedule("2027-01-01", id = 1),
                schedule("깨짐", id = 2),
                schedule("2026-09-01", id = 3)
            ),
            today
        )
        assertEquals(listOf(3L, 1L, 2L), sorted.map { it.id })
    }

    @Test
    fun `남은 일수와 D-day 표기`() {
        assertEquals(14L, schedule("2026-09-01").remainingDays(today))
        assertEquals("D-14", dDayLabel(14))
        assertEquals("D-DAY", dDayLabel(0))
        assertEquals("D+3", dDayLabel(-3))
    }
}
