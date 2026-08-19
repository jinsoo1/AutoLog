package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.ScheduleAlertStage
import com.jsworld.android.autolog.domain.model.formatScheduleDate
import com.jsworld.android.autolog.domain.model.scheduleAlertStage
import com.jsworld.android.autolog.domain.model.scheduleAlertText
import com.jsworld.android.autolog.domain.model.shouldNotifySchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 일정 알림 — 매일 울리면 사용자가 알림을 꺼버린다.
 * "단계가 넘어갈 때만" 규칙을 여기서 잠근다.
 */
class ScheduleAlertTest {

    @Test
    fun `남은 일수로 단계를 나눈다`() {
        assertEquals(ScheduleAlertStage.FAR, scheduleAlertStage(30))
        assertEquals(ScheduleAlertStage.FAR, scheduleAlertStage(15))
        assertEquals(ScheduleAlertStage.TWO_WEEKS, scheduleAlertStage(14))
        assertEquals(ScheduleAlertStage.TWO_WEEKS, scheduleAlertStage(8))
        assertEquals(ScheduleAlertStage.ONE_WEEK, scheduleAlertStage(7))
        assertEquals(ScheduleAlertStage.ONE_WEEK, scheduleAlertStage(1))
        assertEquals(ScheduleAlertStage.TODAY, scheduleAlertStage(0))
        assertEquals(ScheduleAlertStage.OVERDUE, scheduleAlertStage(-1))
    }

    @Test
    fun `아직 멀었으면 알리지 않는다`() {
        assertFalse(shouldNotifySchedule(ScheduleAlertStage.FAR, null))
        assertFalse(shouldNotifySchedule(ScheduleAlertStage.FAR, ScheduleAlertStage.FAR))
    }

    @Test
    fun `같은 단계에서는 매일 울리지 않는다`() {
        assertFalse(
            shouldNotifySchedule(ScheduleAlertStage.TWO_WEEKS, ScheduleAlertStage.TWO_WEEKS)
        )
        assertFalse(shouldNotifySchedule(ScheduleAlertStage.OVERDUE, ScheduleAlertStage.OVERDUE))
    }

    @Test
    fun `단계가 넘어갈 때만 알린다`() {
        assertTrue(shouldNotifySchedule(ScheduleAlertStage.TWO_WEEKS, null))
        assertTrue(shouldNotifySchedule(ScheduleAlertStage.TWO_WEEKS, ScheduleAlertStage.FAR))
        assertTrue(
            shouldNotifySchedule(ScheduleAlertStage.ONE_WEEK, ScheduleAlertStage.TWO_WEEKS)
        )
        assertTrue(shouldNotifySchedule(ScheduleAlertStage.TODAY, ScheduleAlertStage.ONE_WEEK))
        assertTrue(shouldNotifySchedule(ScheduleAlertStage.OVERDUE, ScheduleAlertStage.TODAY))
    }

    @Test
    fun `날짜를 미루면 되돌아가는 전이는 알리지 않는다`() {
        // 사용자가 방금 날짜를 직접 바꿨으니 알려줄 이유가 없다
        assertFalse(shouldNotifySchedule(ScheduleAlertStage.TWO_WEEKS, ScheduleAlertStage.TODAY))
        assertFalse(shouldNotifySchedule(ScheduleAlertStage.ONE_WEEK, ScheduleAlertStage.OVERDUE))
    }

    @Test
    fun `본문은 남은 일수를 사람 말로`() {
        // 제목 뒤에 조사가 붙으므로 띄우지 않는다
        assertEquals("정기검사까지 14일 남았어요", scheduleAlertText("정기검사", 14))
        assertEquals("내일이 보험 만기 날짜예요", scheduleAlertText("보험 만기", 1))
        assertEquals("오늘이 자동차세 날짜예요", scheduleAlertText("자동차세", 0))
        assertEquals("정기검사 날짜가 3일 지났어요", scheduleAlertText("정기검사", -3))
    }

    @Test
    fun `일정 날짜는 연도를 포함한다 - 올해만 생략`() {
        val today = java.time.LocalDate.of(2026, 8, 19)
        // 몇 년 뒤가 흔한 일정이라 연도가 빠지면 지난 날짜처럼 읽힌다
        assertEquals("2027년 7월 1일", formatScheduleDate("2027-07-01", today))
        assertEquals("12월 16일", formatScheduleDate("2026-12-16", today))
        // 깨진 값은 그대로 — 지어내지 않는다
        assertEquals("깨짐", formatScheduleDate("깨짐", today))
    }
}
