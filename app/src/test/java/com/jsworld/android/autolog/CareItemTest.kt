package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.isCareItemName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 세차·코팅류 "관리" 판정.
 * 주기 없는 항목의 배지가 이 판정으로 수리/관리로 갈리므로 오판이 곧 UI 오류다.
 */
class CareItemTest {

    @Test
    fun `기본 항목의 세차·코팅은 관리로 판정된다`() {
        assertTrue(isCareItemName("실내/외 세차(관리)"))
        assertTrue(isCareItemName("코팅/왁스(관리)"))
        // 사용자가 직접 적을 법한 변형
        assertTrue(isCareItemName("셀프 세차"))
        assertTrue(isCareItemName("유리막 코팅"))
        assertTrue(isCareItemName("광택 작업"))
    }

    @Test
    fun `일반 정비·수리는 관리가 아니다`() {
        assertFalse(isCareItemName("엔진오일"))
        assertFalse(isCareItemName("써모스탯 교체"))
        assertFalse(isCareItemName("타이어 교체"))
        assertFalse(isCareItemName("브레이크패드"))
    }

    @Test
    fun `기본값에서 세차·코팅은 주기가 없다`() {
        // 주기가 있으면 한 달 세차를 안 했다고 "초과" 경고가 떠서
        // 진짜 정비 경고를 묻어버린다. 기록 전용이 기본이어야 한다.
        val care = DefaultMaintenanceItems.items.filter { isCareItemName(it.first) }
        assertTrue(care.isNotEmpty())
        care.forEach { (name, interval) ->
            assertTrue("$name 은 주기가 없어야 함", interval.first == null && interval.second == null)
        }
    }
}
