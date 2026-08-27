package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 홈의 임박·초과 카드 상한 — 카드는 3개까지, 나머지는 한 줄로 접는다.
 *
 * 로직 자체는 take/drop 이지만, "접힌 것 중 초과가 있으면 빨강"과
 * "정렬 덕분에 접히는 쪽이 덜 급하다"는 두 전제를 고정해 둔다.
 */
class UrgentCapTest {

    private val cap = 3

    private fun item(name: String, status: MaintenanceStatus) = MaintenanceUiModel(
        settingId = name.hashCode().toLong(),
        name = name,
        status = status,
        remainingText = "",
        hasHistory = true
    )

    /** 화면과 같은 방식으로 나눈다 */
    private fun split(all: List<MaintenanceUiModel>): Triple<Int, Int, Boolean> {
        val shown = all.take(cap)
        val hidden = all.drop(shown.size)
        return Triple(shown.size, hidden.size, hidden.any { it.status == MaintenanceStatus.OVERDUE })
    }

    @Test
    fun `3개 이하면 접지 않는다`() {
        val (shown, hidden, _) = split(List(3) { item("항목$it", MaintenanceStatus.SOON) })
        assertEquals(3, shown)
        assertEquals(0, hidden)
    }

    @Test
    fun `4개부터 한 줄로 접힌다`() {
        val (shown, hidden, _) = split(List(7) { item("항목$it", MaintenanceStatus.SOON) })
        assertEquals(3, shown)
        assertEquals(4, hidden)
    }

    @Test
    fun `접힌 것이 전부 임박이면 빨강을 쓰지 않는다`() {
        // 정렬이 남은 비율 오름차순이라 초과가 앞에 온다 — 초과 2개는 카드로 보인다.
        val all = listOf(
            item("초과A", MaintenanceStatus.OVERDUE),
            item("초과B", MaintenanceStatus.OVERDUE),
            item("임박C", MaintenanceStatus.SOON),
            item("임박D", MaintenanceStatus.SOON),
            item("임박E", MaintenanceStatus.SOON)
        )
        val (shown, hidden, hasOverdue) = split(all)
        assertEquals(3, shown)
        assertEquals(2, hidden)
        assertFalse(hasOverdue)
    }

    @Test
    fun `접힌 것에 초과가 있으면 빨강으로 알린다`() {
        val all = List(4) { item("초과$it", MaintenanceStatus.OVERDUE)}
        val (_, hidden, hasOverdue) = split(all)
        assertEquals(1, hidden)
        assertTrue(hasOverdue)
    }
}
