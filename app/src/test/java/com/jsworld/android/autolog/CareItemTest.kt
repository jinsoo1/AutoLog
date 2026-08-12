package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.DefaultCareItems
import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.isCareItemName
import com.jsworld.android.autolog.domain.model.isWashName
import org.junit.Assert.assertEquals
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
    fun `세차 항목은 정비 기본 항목에 없다`() {
        // 1.2.1 부터 세차는 정비 시스템에서 분리돼 DefaultCareItems 로 옮겨졌다.
        // 정비 목록에 남아 있으면 정비 탭·홈 긴급·알림에 세차가 다시 섞인다.
        val leaked = DefaultMaintenanceItems.items.map { it.first }.filter { isCareItemName(it) }
        assertTrue("정비 기본 항목에 세차류가 남아 있음: $leaked", leaked.isEmpty())
    }

    @Test
    fun `세차 기본 항목은 주기 없이 시딩되고 카운터 기준이 세차다`() {
        // 주기 기본값을 넣으면 켜자마자 초과로 보인다 — 주기는 사용자가 정한다.
        assertTrue(DefaultCareItems.items.isNotEmpty())
        assertEquals(DefaultCareItems.WASH, DefaultCareItems.items.first())
        // 카운터 기준 항목은 세차로 인식돼야 한다.
        assertTrue(isWashName(DefaultCareItems.WASH))
    }

    @Test
    fun `실내 클리닝은 이름만으로는 관리로 안 잡힌다 - 그래서 isCare 플래그가 필요하다`() {
        // 이름 기반 판정의 한계. DB 플래그(maintenance_types.isCare)로 분류하는 이유다.
        assertFalse(isCareItemName("실내 클리닝"))
        assertTrue("기본 세차 항목에는 포함돼 있어야 함", "실내 클리닝" in DefaultCareItems.items)
    }
}
