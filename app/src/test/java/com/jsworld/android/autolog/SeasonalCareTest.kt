package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.Season
import com.jsworld.android.autolog.domain.model.buildSeasonalCareRows
import com.jsworld.android.autolog.domain.model.SeasonalRowState
import com.jsworld.android.autolog.domain.model.isSeasonalCardVisible
import com.jsworld.android.autolog.domain.model.seasonWindowStart
import com.jsworld.android.autolog.domain.model.seasonalDoneCount
import com.jsworld.android.autolog.domain.model.parseSnoozeDate
import com.jsworld.android.autolog.domain.model.seasonalSnoozeDate
import com.jsworld.android.autolog.domain.model.lastCareLabel
import com.jsworld.android.autolog.domain.model.seasonKey
import com.jsworld.android.autolog.domain.model.seasonOf
import com.jsworld.android.autolog.domain.model.seasonalGuide
import com.jsworld.android.autolog.domain.model.seasonalNotificationBody
import com.jsworld.android.autolog.domain.model.shouldNotifySeason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeasonalCareTest {

    /** 어느 달에 열어도 볼 카드가 하나는 있어야 한다 */
    @Test
    fun `12개월이 빈틈없이 계절에 배정된다`() {
        val guides = (1..12).map { seasonalGuide(LocalDate.of(2026, it, 15)) }
        assertEquals(12, guides.size)
        assertEquals(Season.entries.toSet(), guides.map { it.season }.toSet())
    }

    /** 항목 이름으로 사용자의 설정을 찾으므로 글자가 하나라도 다르면 카드가 죽는다 */
    @Test
    fun `계절 항목 이름은 기본 정비 항목에 모두 존재한다`() {
        val known = DefaultMaintenanceItems.items.map { it.first }.toSet()
        Season.entries.forEach { season ->
            val guide = seasonalGuide(LocalDate.of(2026, firstMonthOf(season), 15))
            guide.tips.forEach { tip ->
                assertTrue("${season}: ${tip.itemName}", tip.itemName in known)
            }
        }
    }

    @Test
    fun `장마 카드는 장마가 오기 전에 뜬다`() {
        assertEquals(Season.MONSOON, seasonOf(5))
        assertEquals(Season.MONSOON, seasonOf(6))
        assertEquals(Season.SUMMER, seasonOf(7))
    }

    /** 늦더위가 10월까지 간다 — 9·10월도 냉매·공기압이 그대로 유효한 구간이다 */
    @Test
    fun `여름 창은 10월까지다`() {
        assertEquals(Season.SUMMER, seasonOf(9))
        assertEquals(Season.SUMMER, seasonOf(10))
        assertEquals(Season.PRE_WINTER, seasonOf(11))
        assertEquals(Season.WINTER, seasonOf(12))
    }

    /** 12월에 넘긴 카드가 1월에 다시 뜨면 넘긴 게 아니다 */
    @Test
    fun `겨울 키는 해를 넘겨도 같다`() {
        assertEquals(
            seasonKey(LocalDate.of(2026, 12, 20)),
            seasonKey(LocalDate.of(2027, 1, 5))
        )
    }

    @Test
    fun `계절이 바뀌면 넘긴 카드가 돌아온다`() {
        assertTrue(seasonKey(LocalDate.of(2026, 6, 1)) != seasonKey(LocalDate.of(2026, 8, 1)))
        // 같은 계절이라도 해가 다르면 다시 뜬다
        assertTrue(seasonKey(LocalDate.of(2026, 8, 1)) != seasonKey(LocalDate.of(2027, 8, 1)))
    }

    @Test
    fun `관리 목록에 없는 항목도 줄은 남는다`() {
        val today = LocalDate.of(2026, 11, 20)
        val guide = seasonalGuide(today)
        val rows = buildSeasonalCareRows(
            guide = guide,
            items = listOf(uiModel(1L, "배터리")),
            lastServiceDates = mapOf(1L to LocalDate.of(2024, 10, 1)),
            today = today
        )

        assertEquals(guide.tips.size, rows.size)
        val battery = rows.first { it.itemName == "배터리" }
        assertEquals(1L, battery.settingId)
        assertNotNull(battery.lastServiceDate)
        // 켜두지 않은 항목은 기록할 곳이 없다 — 그래도 "무엇을 봐야 하나"는 남긴다
        assertTrue(rows.any { it.settingId == null })
    }

    @Test
    fun `기록이 없으면 마지막 날짜도 없다`() {
        val today = LocalDate.of(2026, 11, 20)
        val rows = buildSeasonalCareRows(
            guide = seasonalGuide(today),
            items = listOf(uiModel(1L, "배터리")),
            lastServiceDates = emptyMap(),
            today = today
        )
        assertNull(rows.first { it.itemName == "배터리" }.lastServiceDate)
        assertEquals(SeasonalRowState.TODO, rows.first { it.itemName == "배터리" }.state)
    }

    /* ── 줄 상태 (이번 계절에 이미 했는가) ── */

    @Test
    fun `계절 창 시작일`() {
        assertEquals(LocalDate.of(2026, 7, 1), seasonWindowStart(LocalDate.of(2026, 8, 20)))
        assertEquals(LocalDate.of(2026, 7, 1), seasonWindowStart(LocalDate.of(2026, 10, 31)))
        assertEquals(LocalDate.of(2026, 11, 1), seasonWindowStart(LocalDate.of(2026, 11, 5)))
        // 겨울은 해를 넘긴다 — 1월에 열어도 시작은 작년 12월 1일
        assertEquals(LocalDate.of(2025, 12, 1), seasonWindowStart(LocalDate.of(2026, 1, 15)))
    }

    /** 이번 달에 갈아놓은 항목까지 "기록하세요"라고 시키면 안 된다 */
    @Test
    fun `이번 계절에 기록이 있으면 확인함으로 잡힌다`() {
        val today = LocalDate.of(2026, 8, 20)   // 여름 창: 7월 1일 시작
        val rows = buildSeasonalCareRows(
            guide = seasonalGuide(today),
            items = listOf(uiModel(1L, "냉각수(부동액)")),
            lastServiceDates = mapOf(1L to LocalDate.of(2026, 8, 7)),
            today = today
        )
        val coolant = rows.first { it.itemName == "냉각수(부동액)" }
        assertEquals(SeasonalRowState.DONE, coolant.state)
        assertEquals(1, seasonalDoneCount(rows))
    }

    @Test
    fun `계절 창 이전 기록은 확인함이 아니다`() {
        val today = LocalDate.of(2026, 8, 20)
        val rows = buildSeasonalCareRows(
            guide = seasonalGuide(today),
            items = listOf(uiModel(1L, "냉각수(부동액)")),
            // 6월 30일 — 여름 창(7월 1일)보다 하루 전
            lastServiceDates = mapOf(1L to LocalDate.of(2026, 6, 30)),
            today = today
        )
        assertEquals(SeasonalRowState.TODO, rows.first { it.itemName == "냉각수(부동액)" }.state)
        assertEquals(0, seasonalDoneCount(rows))
    }

    @Test
    fun `관리 목록에 없으면 확인함이 될 수 없다`() {
        val today = LocalDate.of(2026, 8, 20)
        val rows = buildSeasonalCareRows(
            guide = seasonalGuide(today),
            items = emptyList(),
            lastServiceDates = emptyMap(),
            today = today
        )
        assertTrue(rows.all { it.state == SeasonalRowState.NOT_MANAGED })
        assertEquals(0, seasonalDoneCount(rows))
    }

    @Test
    fun `마지막 기록 문구`() {
        val today = LocalDate.of(2026, 10, 15)
        assertEquals("아직 기록 없음", lastCareLabel(null, today))
        assertEquals("마지막 이번 달", lastCareLabel(LocalDate.of(2026, 10, 1), today))
        assertEquals("마지막 3개월 전", lastCareLabel(LocalDate.of(2026, 7, 20), today))
        assertEquals("마지막 2년 전", lastCareLabel(LocalDate.of(2024, 10, 1), today))
    }

    private fun firstMonthOf(season: Season): Int = when (season) {
        Season.SPRING -> 3
        Season.MONSOON -> 5
        Season.SUMMER -> 7
        Season.PRE_WINTER -> 11
        Season.WINTER -> 12
    }

    private fun uiModel(settingId: Long, name: String) = MaintenanceUiModel(
        settingId = settingId,
        name = name,
        status = com.jsworld.android.autolog.domain.model.MaintenanceStatus.NORMAL,
        remainingText = ""
    )

    /* ── 계절 알림 ── */

    @Test
    fun `계절 알림은 계절마다 한 번만`() {
        val key = seasonKey(LocalDate.of(2026, 11, 1))
        assertTrue(shouldNotifySeason(key, notifiedKey = "", dismissedKey = ""))
        // 이미 보냈으면 그만
        assertFalse(shouldNotifySeason(key, notifiedKey = key, dismissedKey = ""))
        // 지난 계절에 보낸 것은 이번 계절을 막지 않는다
        assertTrue(shouldNotifySeason(key, notifiedKey = "SUMMER-2026", dismissedKey = ""))
    }

    /**
     * 화면에서 치운 것을 알림으로 다시 들이밀지 않는다.
     * 넘기기는 차량별이라 호출부가 "모든 차에서 넘겼는가"를 계산해 넘긴다.
     */
    @Test
    fun `카드를 넘긴 계절은 알림도 보내지 않는다`() {
        val key = seasonKey(LocalDate.of(2026, 11, 1))
        assertFalse(shouldNotifySeason(key, notifiedKey = "", dismissedKey = key))
        // 아직 안 넘긴 차가 한 대라도 있으면(= dismissedKey 가 비어 옴) 알림은 간다
        assertTrue(shouldNotifySeason(key, notifiedKey = "", dismissedKey = ""))
    }

    @Test
    fun `알림 본문은 항목 이름만 담는다`() {
        val guide = seasonalGuide(LocalDate.of(2026, 11, 1))
        val body = seasonalNotificationBody(guide)
        assertEquals(guide.tips.joinToString(" · ") { it.itemName }, body)
        // "마지막 언제"는 차량마다 달라서 알림에 넣지 않는다
        assertFalse(body.contains("마지막"))
    }

    /* ── 카드 접기 (X → 다음 달 / 내년) ── */

    @Test
    fun `접지 않았으면 보인다`() {
        val today = LocalDate.of(2026, 8, 20)
        val key = seasonKey(today)
        assertTrue(isSeasonalCardVisible(today, key, dismissedKey = "", snoozeUntil = null))
    }

    @Test
    fun `내년에 다시 를 고르면 이번 계절은 끝난다`() {
        val today = LocalDate.of(2026, 8, 20)
        val key = seasonKey(today)
        assertFalse(isSeasonalCardVisible(today, key, dismissedKey = key, snoozeUntil = null))

        // 계절이 바뀌면 키가 달라져 저절로 돌아온다
        val preWinter = LocalDate.of(2026, 11, 1)
        assertTrue(
            isSeasonalCardVisible(
                preWinter, seasonKey(preWinter), dismissedKey = key, snoozeUntil = null
            )
        )
    }

    @Test
    fun `다음 달에 다시 는 그 날짜까지만 숨긴다`() {
        val today = LocalDate.of(2026, 8, 20)
        val until = seasonalSnoozeDate(today)
        assertEquals(LocalDate.of(2026, 9, 20), until)

        val key = seasonKey(today)
        assertFalse(isSeasonalCardVisible(today, key, dismissedKey = "", snoozeUntil = until))
        // 하루 전날까지 숨고
        assertFalse(
            isSeasonalCardVisible(until.minusDays(1), seasonKey(until.minusDays(1)), "", until)
        )
        // 그날이 오면 보인다
        assertTrue(isSeasonalCardVisible(until, seasonKey(until), "", until))
    }

    /**
     * 한 달씩 미루다 계절 창을 넘어가면 그때는 **다음 계절 안내**가 뜬다.
     * 이번 계절 내용은 저절로 내년으로 밀린다 — 계절 키에 연도가 붙어 있어서다.
     */
    @Test
    fun `한 달씩 미루다 계절이 넘어가면 다음 계절 안내가 된다`() {
        // 10월(여름 창의 끝)에 미루면 11월 20일 — 그때는 겨울 전 창이다
        val october = LocalDate.of(2026, 10, 20)
        val until = seasonalSnoozeDate(october)
        assertEquals(Season.SUMMER, seasonOf(october.monthValue))
        assertEquals(Season.PRE_WINTER, seasonOf(until.monthValue))

        // 그날 카드는 보이고, 내용은 여름이 아니라 겨울 전 안내다
        assertTrue(isSeasonalCardVisible(until, seasonKey(until), "", until))
        assertEquals(Season.PRE_WINTER, seasonalGuide(until).season)

        // 여름 안내는 내년 여름에 돌아온다
        val nextSummer = LocalDate.of(2027, 7, 10)
        assertEquals(Season.SUMMER, seasonalGuide(nextSummer).season)
        assertTrue(seasonKey(october) != seasonKey(nextSummer))
    }

    /** 창 안에서 미루면 같은 계절 안내가 다시 온다 — 다이얼로그가 이걸로 문구를 가린다 */
    @Test
    fun `창 안에서 미루면 같은 계절이 다시 온다`() {
        val august = LocalDate.of(2026, 8, 20)
        val until = seasonalSnoozeDate(august)   // 9월 20일 — 아직 여름 창
        assertEquals(Season.SUMMER, seasonOf(until.monthValue))
        assertEquals(seasonKey(august), seasonKey(until))
    }

    @Test
    fun `미룬 날짜가 깨져 있으면 없는 것으로 본다`() {
        assertNull(parseSnoozeDate(null))
        assertNull(parseSnoozeDate(""))
        assertNull(parseSnoozeDate("몰라요"))
        assertEquals(LocalDate.of(2026, 9, 20), parseSnoozeDate("2026-09-20"))
    }
}
