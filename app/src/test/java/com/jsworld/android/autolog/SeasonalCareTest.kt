package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.Season
import com.jsworld.android.autolog.domain.model.buildSeasonalCareRows
import com.jsworld.android.autolog.domain.model.lastCareLabel
import com.jsworld.android.autolog.domain.model.seasonKey
import com.jsworld.android.autolog.domain.model.seasonOf
import com.jsworld.android.autolog.domain.model.seasonalGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(Season.PRE_WINTER, seasonOf(10))
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
        val guide = seasonalGuide(LocalDate.of(2026, 10, 1))
        val rows = buildSeasonalCareRows(
            guide = guide,
            items = listOf(uiModel(1L, "배터리")),
            lastServiceDates = mapOf(1L to LocalDate.of(2024, 10, 1))
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
        val rows = buildSeasonalCareRows(
            guide = seasonalGuide(LocalDate.of(2026, 10, 1)),
            items = listOf(uiModel(1L, "배터리")),
            lastServiceDates = emptyMap()
        )
        assertNull(rows.first { it.itemName == "배터리" }.lastServiceDate)
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
        Season.PRE_WINTER -> 9
        Season.WINTER -> 12
    }

    private fun uiModel(settingId: Long, name: String) = MaintenanceUiModel(
        settingId = settingId,
        name = name,
        status = com.jsworld.android.autolog.domain.model.MaintenanceStatus.NORMAL,
        remainingText = ""
    )
}
