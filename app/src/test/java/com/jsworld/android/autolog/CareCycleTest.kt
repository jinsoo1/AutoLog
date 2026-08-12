package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.CareCycleUnit
import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.domain.model.buildCareCycles
import com.jsworld.android.autolog.domain.model.careNudgeCandidates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 세차 횟수 기반 주기 — km·개월로는 표현할 수 없는 단위라 이 앱의 차별점이다.
 * 계산이 틀리면 "이번엔 왁스도 할 때" 안내가 엉뚱한 때 뜨므로 여기서 잠근다.
 */
class CareCycleTest {

    private val today = LocalDate.of(2026, 8, 11)

    private fun item(
        name: String,
        settingId: Long? = 1L,
        washCount: Int? = null,
        months: Int? = null
    ) = CarePickItem(
        name = name,
        enabled = settingId != null,
        settingId = settingId,
        intervalMonths = months,
        intervalWashCount = washCount
    )

    @Test
    fun `주기 없는 항목과 꺼진 항목은 목록에 나오지 않는다`() {
        val cycles = buildCareCycles(
            items = listOf(item("왁스 코팅"), item("실내 클리닝", settingId = null, washCount = 3)),
            washDates = listOf("2026-08-01"),
            lastByName = emptyMap(),
            today = today
        )
        assertTrue(cycles.isEmpty())
    }

    @Test
    fun `카운터 기준인 세차 자체는 주기가 없으면 제외된다`() {
        val cycles = buildCareCycles(
            items = listOf(item("세차")),
            washDates = listOf("2026-08-01"),
            lastByName = mapOf("세차" to "2026-08-01"),
            today = today
        )
        assertTrue(cycles.isEmpty())
    }

    @Test
    fun `세차 3회마다 - 마지막 기록 이후 세차 2회면 1회 남음`() {
        val cycles = buildCareCycles(
            items = listOf(item("실내 클리닝", washCount = 3)),
            washDates = listOf("2026-07-01", "2026-07-20", "2026-08-05"),
            lastByName = mapOf("실내 클리닝" to "2026-07-10"),
            today = today
        )
        val c = cycles.single()
        assertEquals(CareCycleUnit.WASH_COUNT, c.unit)
        assertEquals("1회 남음", c.remainText)
        assertFalse(c.isOverdue)
        assertEquals("세차 3회마다 · 마지막 7월 10일", c.caption)
    }

    @Test
    fun `도달하면 이번에 할 때, 넘기면 N회 지남`() {
        val due = buildCareCycles(
            items = listOf(item("왁스 코팅", washCount = 2)),
            washDates = listOf("2026-07-20", "2026-08-05"),
            lastByName = mapOf("왁스 코팅" to "2026-07-10"),
            today = today
        ).single()
        assertEquals("이번에 할 때", due.remainText)
        assertTrue(due.isOverdue)

        val over = buildCareCycles(
            items = listOf(item("왁스 코팅", washCount = 2)),
            washDates = listOf("2026-07-20", "2026-08-01", "2026-08-05", "2026-08-09"),
            lastByName = mapOf("왁스 코팅" to "2026-07-10"),
            today = today
        ).single()
        assertEquals("2회 지남", over.remainText)
        assertTrue(over.isOverdue)
    }

    @Test
    fun `기록이 없으면 전체 세차 횟수로 센다`() {
        val c = buildCareCycles(
            items = listOf(item("왁스 코팅", washCount = 3)),
            washDates = listOf("2026-07-01", "2026-07-20", "2026-08-05"),
            lastByName = emptyMap(),
            today = today
        ).single()
        assertEquals("이번에 할 때", c.remainText)
        assertEquals("세차 3회마다 · 아직 기록 없음", c.caption)
    }

    @Test
    fun `기간 주기 - 마지막 기록 기준 남은 일수`() {
        val c = buildCareCycles(
            items = listOf(item("유리막 코팅", months = 6)),
            washDates = emptyList(),
            lastByName = mapOf("유리막 코팅" to "2026-06-27"),
            today = today
        ).single()
        assertEquals(CareCycleUnit.MONTHS, c.unit)
        assertEquals("138일 남음", c.remainText)
        assertFalse(c.isOverdue)
    }

    @Test
    fun `기간 주기 - 기록이 없으면 진행도를 만들지 않는다`() {
        val c = buildCareCycles(
            items = listOf(item("유리막 코팅", months = 6)),
            washDates = emptyList(),
            lastByName = emptyMap(),
            today = today
        ).single()
        assertNull(c.progress)
        assertEquals("첫 기록 필요", c.remainText)
        assertFalse(c.isOverdue)
    }

    @Test
    fun `넛지는 세차 횟수 주기의 초과 항목만 - 기간 주기는 제외`() {
        val cycles = buildCareCycles(
            items = listOf(
                item("왁스 코팅", settingId = 1L, washCount = 2),
                item("유리막 코팅", settingId = 2L, months = 1)
            ),
            washDates = listOf("2026-08-01", "2026-08-05"),
            lastByName = mapOf("왁스 코팅" to "2026-07-10", "유리막 코팅" to "2026-01-01"),
            today = today
        )
        // 둘 다 초과 상태지만 넛지는 세차 횟수 기반만
        assertEquals(2, cycles.count { it.isOverdue })
        val nudges = careNudgeCandidates(cycles)
        assertEquals(listOf("왁스 코팅"), nudges.map { it.name })
    }

    @Test
    fun `초과 항목이 목록 위로 정렬된다`() {
        val cycles = buildCareCycles(
            items = listOf(
                item("실내 클리닝", settingId = 1L, washCount = 10),
                item("왁스 코팅", settingId = 2L, washCount = 2)
            ),
            washDates = listOf("2026-08-01", "2026-08-05"),
            lastByName = mapOf("실내 클리닝" to "2026-07-30", "왁스 코팅" to "2026-07-10"),
            today = today
        )
        assertEquals(listOf("왁스 코팅", "실내 클리닝"), cycles.map { it.name })
    }
}
