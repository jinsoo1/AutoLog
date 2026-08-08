package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.isItemApplicableToFuel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 온보딩 추천 팩.
 *
 * 팩은 **이름 문자열로** 정비 타입을 찾으므로, 이름이 기본 항목과
 * 한 글자라도 다르면 그 항목은 조용히 빠진다. 여기서 전부 대조한다.
 */
class MaintenanceStarterPackTest {

    private val defaultNames = DefaultMaintenanceItems.items.map { it.first }.toSet()

    @Test
    fun `팩의 모든 이름이 기본 항목에 실제로 존재한다`() {
        val missing = (DefaultMaintenanceItems.lightPack + DefaultMaintenanceItems.standardExtra)
            .filterNot { it in defaultNames }
        assertTrue("기본 항목에 없는 이름: $missing", missing.isEmpty())
    }

    @Test
    fun `단계가 올라갈수록 항목이 늘어난다`() {
        val light = DefaultMaintenanceItems.lightPack.size
        val standard = DefaultMaintenanceItems.standardPack.size
        val full = DefaultMaintenanceItems.fullPack.size
        assertTrue("가볍게($light) < 꼼꼼하게($standard)", light < standard)
        assertTrue("꼼꼼하게($standard) < 빈틈없이($full)", standard < full)
    }

    @Test
    fun `꼼꼼하게는 가볍게를 포함한다`() {
        // 상위 팩이 하위 팩을 포함해야 "단계"라는 말이 성립한다.
        assertTrue(
            DefaultMaintenanceItems.standardPack.containsAll(DefaultMaintenanceItems.lightPack)
        )
        assertTrue(
            DefaultMaintenanceItems.fullPack.containsAll(DefaultMaintenanceItems.standardPack)
        )
    }

    @Test
    fun `팩 안에 중복 이름이 없다`() {
        val standard = DefaultMaintenanceItems.standardPack
        assertEquals(standard.size, standard.distinct().size)
    }

    @Test
    fun `전기차에 적용하면 엔진 전용 항목이 걸러진다`() {
        val evLight = DefaultMaintenanceItems.lightPack
            .filter { isItemApplicableToFuel(it, "전기") }

        assertFalse("전기차 가볍게에 엔진오일이 있으면 안 됨", "엔진오일" in evLight)
        assertFalse("엔진오일 필터" in evLight)
        // 연료와 무관한 항목은 남아야 한다.
        assertTrue("에어컨(캐빈) 필터" in evLight)
        assertTrue("타이어 교체" in evLight)
        assertTrue(evLight.isNotEmpty())
    }

    @Test
    fun `가솔린차는 팩 전체가 그대로 적용된다`() {
        val filtered = DefaultMaintenanceItems.standardPack
            .filter { isItemApplicableToFuel(it, "가솔린") }
        assertEquals(DefaultMaintenanceItems.standardPack.size, filtered.size)
    }
}
