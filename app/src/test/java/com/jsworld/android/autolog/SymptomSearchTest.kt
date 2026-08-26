package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.Powertrain
import com.jsworld.android.autolog.domain.model.Symptom
import com.jsworld.android.autolog.domain.model.SymptomCategory
import com.jsworld.android.autolog.domain.model.SymptomRiskLevel
import com.jsworld.android.autolog.domain.model.appliesToAllPowertrains
import com.jsworld.android.autolog.domain.model.filterSymptoms
import com.jsworld.android.autolog.domain.model.matches
import com.jsworld.android.autolog.domain.model.powertrainLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 증상 검색 — 의성어·구어체 키워드와 띄어쓰기 차이를 흡수해야
 * "핸들떨림"으로도 "핸들이 떨려요"를 찾을 수 있다.
 */
class SymptomSearchTest {

    private fun symptom(
        id: String,
        category: SymptomCategory,
        title: String,
        keywords: List<String>
    ) = Symptom(
        id = id,
        category = category,
        title = title,
        keywords = keywords,
        riskLevel = SymptomRiskLevel.CAUTION,
        summary = "",
        checks = listOf("점검 항목"),
        distinguishPoints = emptyList(),
        observationPoints = emptyList(),
        escalations = emptyList(),
        immediateAction = null
    )

    private val handleShake = symptom(
        "st_shake", SymptomCategory.DRIVING_STEERING,
        "주행 중 핸들이 떨려요", listOf("핸들 떨림", "덜덜", "진동")
    )
    private val helicopter = symptom(
        "nv_heli", SymptomCategory.NOISE_VIBRATION,
        "주행 중 헬리콥터 같은 두두두 소음이 들려요", listOf("두두두", "헬리콥터", "웅웅")
    )

    @Test
    fun `제목 부분 일치`() {
        assertTrue(handleShake.matches("핸들"))
        assertFalse(helicopter.matches("핸들"))
    }

    @Test
    fun `의성어 키워드로 찾는다`() {
        assertTrue(helicopter.matches("두두두"))
        assertTrue(handleShake.matches("덜덜"))
    }

    @Test
    fun `띄어쓰기 차이를 무시한다`() {
        assertTrue(handleShake.matches("핸들떨림"))
        assertTrue(handleShake.matches("핸들이떨려요"))
    }

    @Test
    fun `빈 검색어는 전부 통과`() {
        assertTrue(handleShake.matches(""))
        assertTrue(handleShake.matches("  "))
    }

    @Test
    fun `카테고리와 검색어를 함께 거른다`() {
        val all = listOf(handleShake, helicopter)
        assertEquals(listOf(helicopter), filterSymptoms(all, SymptomCategory.NOISE_VIBRATION, ""))
        assertEquals(listOf(handleShake), filterSymptoms(all, null, "떨림"))
        // 카테고리가 다르면 키워드가 맞아도 빠진다
        assertEquals(emptyList<Symptom>(), filterSymptoms(all, SymptomCategory.BRAKE, "떨림"))
    }

    @Test
    fun `범위 밖 위험도는 보수적으로 해석한다`() {
        assertEquals(SymptomRiskLevel.STOP, SymptomRiskLevel.fromLevel(9))
        assertEquals(SymptomRiskLevel.CAUTION, SymptomRiskLevel.fromLevel(0))
    }

    @Test
    fun `파워트레인이 비어 있으면 전 차종 공통 - 배지를 띄우지 않는다`() {
        assertTrue(handleShake.appliesToAllPowertrains)
        assertNull(handleShake.powertrainLabel())
    }

    @Test
    fun `네 계통을 모두 지정한 것도 공통으로 본다`() {
        val all = handleShake.copy(powertrains = Powertrain.entries.toSet())
        assertTrue(all.appliesToAllPowertrains)
        assertNull(all.powertrainLabel())
    }

    @Test
    fun `표시 문구는 enum 선언 순서를 따른다`() {
        // 집합에 넣은 순서가 아니라 내연기관 → 하이브리드 → 전기 → 수소 순서로 나와야
        // 항목마다 배지 문구가 흔들리지 않는다.
        val engine = handleShake.copy(powertrains = setOf(Powertrain.HYBRID, Powertrain.ICE))
        assertEquals("내연기관 · 하이브리드", engine.powertrainLabel())

        val ev = handleShake.copy(powertrains = setOf(Powertrain.FCEV, Powertrain.EV))
        assertEquals("전기 · 수소", ev.powertrainLabel())
    }

    @Test
    fun `파워트레인은 목록을 걸러내지 않는다`() {
        // 알려주기만 하는 값이다 — 필터에 섞이면 라벨이 부정확한 사용자에게서
        // 안전 증상이 사라진다.
        val iceOnly = handleShake.copy(powertrains = setOf(Powertrain.ICE))
        assertEquals(
            listOf(iceOnly, helicopter),
            filterSymptoms(listOf(iceOnly, helicopter), null, "")
        )
    }
}
