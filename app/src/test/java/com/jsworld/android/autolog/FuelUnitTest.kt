package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.FuelUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 연료 타입 → 에너지 종류 판정.
 *
 * 이 판정이 틀리면 ⑴ 전기를 안 쓰는 차에 충전 UI 가 나오거나
 * ⑵ 플러그인 하이브리드가 충전을 기록할 수 없게 된다.
 */
class FuelUnitTest {

    @Test
    fun `플러그인 하이브리드는 주유와 충전을 모두 한다`() {
        assertEquals(
            listOf(FuelUnit.LITER, FuelUnit.KWH),
            FuelUnit.supportedUnits("플러그인 하이브리드")
        )
        // 표기 변형도 같이 받는다.
        assertEquals(
            listOf(FuelUnit.LITER, FuelUnit.KWH),
            FuelUnit.supportedUnits("PHEV")
        )
        assertEquals(
            listOf(FuelUnit.LITER, FuelUnit.KWH),
            FuelUnit.supportedUnits("플러그인하이브리드")
        )
    }

    @Test
    fun `일반 하이브리드는 외부 충전을 하지 않으므로 주유만`() {
        assertEquals(listOf(FuelUnit.LITER), FuelUnit.supportedUnits("하이브리드"))
        assertEquals(listOf(FuelUnit.LITER), FuelUnit.supportedUnits("HEV"))
        assertFalse(FuelUnit.usesElectricCharging("하이브리드"))
    }

    @Test
    fun `순수 전기차는 충전만`() {
        assertEquals(listOf(FuelUnit.KWH), FuelUnit.supportedUnits("전기"))
        assertEquals(listOf(FuelUnit.KWH), FuelUnit.supportedUnits("EV"))
        assertTrue(FuelUnit.usesElectricCharging("전기"))
    }

    @Test
    fun `수소차는 kg 으로 충전한다`() {
        assertEquals(listOf(FuelUnit.KG), FuelUnit.supportedUnits("수소"))
        // 전기 충전은 아니므로 전기 관련 표시는 나오지 않는다.
        assertFalse(FuelUnit.usesElectricCharging("수소"))
        assertEquals("충전", FuelUnit.KG.actionLabel)
        assertEquals("kg", FuelUnit.KG.symbol)
    }

    @Test
    fun `내연기관과 미설정은 주유만이고 전기 표시가 없다`() {
        listOf("가솔린", "디젤", "LPG", "기타", "", null).forEach { fuelType ->
            assertEquals(
                "fuelType=$fuelType",
                listOf(FuelUnit.LITER),
                FuelUnit.supportedUnits(fuelType)
            )
            assertFalse("fuelType=$fuelType", FuelUnit.usesElectricCharging(fuelType))
        }
    }

    @Test
    fun `기본 종류는 지원 목록의 첫 번째다`() {
        assertEquals(FuelUnit.LITER, FuelUnit.defaultFor("플러그인 하이브리드"))
        assertEquals(FuelUnit.KWH, FuelUnit.defaultFor("전기"))
        assertEquals(FuelUnit.KG, FuelUnit.defaultFor("수소"))
        assertEquals(FuelUnit.LITER, FuelUnit.defaultFor("가솔린"))
    }

    @Test
    fun `문구는 주유와 충전으로만 갈린다`() {
        assertEquals("주유", FuelUnit.LITER.actionLabel)
        assertEquals("주유소", FuelUnit.LITER.placeLabel)
        assertEquals("충전", FuelUnit.KWH.actionLabel)
        assertEquals("충전소", FuelUnit.KWH.placeLabel)
        assertEquals("충전비", FuelUnit.KWH.costLabel)
    }

    @Test
    fun `기록에 저장된 심볼로 종류를 되살린다`() {
        assertEquals(FuelUnit.LITER, FuelUnit.fromSymbol("L"))
        assertEquals(FuelUnit.KWH, FuelUnit.fromSymbol("kWh"))
        assertEquals(FuelUnit.KG, FuelUnit.fromSymbol("kg"))
        // 알 수 없는 값은 주유로 떨어뜨린다(과거 데이터 보호).
        assertEquals(FuelUnit.LITER, FuelUnit.fromSymbol("???"))
    }

    // --- 표시 기준: 연료 타입을 바꿔도 과거 기록이 숨지 않아야 한다 ---

    @Test
    fun `연료 타입을 전기로 바꿔도 과거 주유 기록은 계속 표시된다`() {
        // PHEV 로 주유·충전을 기록해 두고 전기차로 바꾼 상황.
        val recordUnits = listOf(FuelUnit.LITER, FuelUnit.KWH)

        val display = FuelUnit.displayUnits(recordUnits, "전기")

        // 차량 설정만 보면 [kWh] 뿐이지만, 주유 기록이 있으므로 둘 다 보여줘야 한다.
        assertEquals(listOf(FuelUnit.LITER, FuelUnit.KWH), display)
    }

    @Test
    fun `연료 타입을 가솔린으로 바꿔도 과거 충전 기록은 계속 표시된다`() {
        val display = FuelUnit.displayUnits(listOf(FuelUnit.KWH), "가솔린")
        assertEquals(listOf(FuelUnit.LITER, FuelUnit.KWH), display)
    }

    @Test
    fun `기록이 없으면 차량 설정만 따른다`() {
        assertEquals(listOf(FuelUnit.LITER), FuelUnit.displayUnits(emptyList(), "가솔린"))
        assertEquals(listOf(FuelUnit.KWH), FuelUnit.displayUnits(emptyList(), "전기"))
        assertEquals(
            listOf(FuelUnit.LITER, FuelUnit.KWH),
            FuelUnit.displayUnits(emptyList(), "플러그인 하이브리드")
        )
    }

    @Test
    fun `표시 순서는 항상 주유가 먼저다`() {
        // 충전 기록이 먼저 들어와도 순서를 뒤집지 않는다(색·범례 순서가 흔들리지 않도록).
        assertEquals(
            listOf(FuelUnit.LITER, FuelUnit.KWH),
            FuelUnit.displayUnits(listOf(FuelUnit.KWH, FuelUnit.LITER), "플러그인 하이브리드")
        )
    }

    @Test
    fun `단일 종류 차량은 표시도 단일이라 배지와 필터가 나오지 않는다`() {
        val display = FuelUnit.displayUnits(listOf(FuelUnit.LITER), "가솔린")
        assertEquals(1, display.size)
        assertFalse(display.any { it.isElectric })
    }
}
