package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.DefaultCareItems
import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.BASE_WASH_NAME
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
    fun `이름 규칙은 이관·레거시 백업 전용 - 키워드 없는 이름은 못 잡는다`() {
        // 이름 판정('실내 클리닝'처럼 키워드 없는 이름을 놓침)은 v3→v4 마이그레이션과
        // 구버전 백업 변환에만 쓴다. 새 데이터의 분류는 테이블 자체(care_items)가 담당한다.
        assertFalse(isCareItemName("실내 클리닝"))
    }

    @Test
    fun `기본 세차가 카운터 기준이고 나머지는 선택 항목이다`() {
        assertEquals(BASE_WASH_NAME, DefaultCareItems.items.first())
        // 사용자가 요청한 관리 항목들이 기본으로 제공된다
        listOf(
            "실내세차", "휠·타이어", "철분·타르 제거", "유막 제거",
            "발수코팅", "왁스코팅", "유리막코팅", "광택", "실내 클리닝"
        ).forEach { assertTrue("$it 누락", it in DefaultCareItems.items) }
    }

    @Test
    fun `옛 이름은 새 항목 이름으로 정규화된다`() {
        assertEquals(BASE_WASH_NAME, DefaultCareItems.normalizeLegacyName("실내/외 세차(관리)"))
        assertEquals("왁스코팅", DefaultCareItems.normalizeLegacyName("코팅/왁스(관리)"))
        // 그 밖의 이름은 그대로 둔다
        assertEquals("하부 세차", DefaultCareItems.normalizeLegacyName("하부 세차"))
    }
}
