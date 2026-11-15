package com.jsworld.android.autolog.domain.model

import androidx.annotation.StringRes
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.UiText

/**
 * 증상별 점검 가이드의 한 증상.
 *
 * "진단"이 아니라 "점검 가이드"다 — 원인을 확정하는 데이터가 아니라
 * 점검 후보와 주행위험도, 관찰 조건을 담는다. 콘텐츠는 assets 의
 * symptom_guide.json 에서 읽으므로 문구 수정·증상 추가에 코드 변경이 없다.
 */
data class Symptom(
    val id: String,
    val category: SymptomCategory,
    val title: String,
    /** 의성어·구어체 검색어 ("두두두", "웅웅", "탄 냄새" 등) */
    val keywords: List<String>,
    /** 증상 단독 기준의 보수적인 기본 주행위험도 */
    val riskLevel: SymptomRiskLevel,
    val summary: String,
    /** 점검해볼 항목 — 흔하고 확인하기 쉬운 것부터 */
    val checks: List<String>,
    /** 구분 포인트 — 발생 조건으로 후보를 좁힐 수 있을 때만 ("조건 → 해석") */
    val distinguishPoints: List<String>,
    /** 확인 포인트 — 구분보다 직접 관찰이 유용할 때 */
    val observationPoints: List<String>,
    /** 동반 증상에 따라 위험도가 올라가는 조건 */
    val escalations: List<SymptomEscalation>,
    /** 안전 관련 증상에서 점검 정보보다 우선하는 행동 안내 */
    val immediateAction: String?,
    /**
     * 이 증상이 해당되는 동력 계통. **비어 있으면 전 차종 공통**이다.
     *
     * 목록을 걸러내지 않는다 — 알려주기만 한다. 연료 타입은 사용자가 주유 기록
     * 편의로 고르는 값이라(칩을 다시 누르면 해제되기까지 한다) 이 값으로 증상을
     * 숨기면 라벨이 부정확한 사용자에게서 안전 증상이 사라진다.
     */
    val powertrains: Set<Powertrain> = emptySet()
)

/**
 * 동력 계통 구분. 증상이 갈리는 기준은 "엔진이 있나, 변속기가 있나, 배기가 있나"라
 * 연료 타입 8종보다 굵게 묶는다. 다만 DPF·요소수(디젤)나 봄베(LPG)처럼 **한 연료에만
 * 있는 계통**은 `ICE` 로 묶으면 가솔린 차주가 자기 얘기로 읽으므로 따로 둔다.
 *
 * ⚠️ `ICE` 와 `DIESEL`·`LPG` 를 함께 붙이지 말 것 — "내연기관 · 디젤"이라는 이상한
 * 배지가 나온다. 내연기관 전반이면 `ICE`, 그 연료 전용이면 `DIESEL`/`LPG` 하나만.
 */
enum class Powertrain(@StringRes val labelRes: Int) {
    ICE(R.string.powertrain_ice),
    DIESEL(R.string.powertrain_diesel),
    LPG(R.string.powertrain_lpg),
    HYBRID(R.string.powertrain_hybrid),
    EV(R.string.powertrain_ev),
    FCEV(R.string.powertrain_fcev);

    val label: UiText get() = UiText.Res(labelRes)

    companion object {
        fun fromOrNull(raw: String): Powertrain? = entries.firstOrNull { it.name == raw }
    }
}

/** 연료 종류와 무관하게 모든 차량에 해당하면 true */
val Symptom.appliesToAllPowertrains: Boolean
    get() = powertrains.isEmpty() || powertrains.size == Powertrain.entries.size

/**
 * "내연기관 · 하이브리드" 같은 표시 문구. **모든 차량 해당이면 null.**
 * 목록에서 쓴다 — 절반이 넘는 항목에 같은 배지를 달면 배지가 신호를 잃는다.
 */
fun Symptom.powertrainLabel(): UiText? {
    if (appliesToAllPowertrains) return null
    return UiText.Joined(Powertrain.entries.filter { it in powertrains }.map { it.label })
}

/**
 * 상세 화면용 — 모든 차량 해당이면 "모든 차량"을 돌려주고 절대 null 이 아니다.
 *
 * 상세는 "내 차에 해당하나"를 판단하는 자리라 배지가 없으면 공통인지 태깅을
 * 안 한 건지 알 수 없다. 그래서 여기서는 늘 밝힌다.
 */
fun Symptom.powertrainScopeLabel(): UiText =
    powertrainLabel() ?: UiText.Res(R.string.powertrain_all_vehicles)

/** 배지 아래에 붙는 한 문장. 배지가 무슨 뜻인지 풀어준다. */
fun Symptom.powertrainNote(): UiText =
    powertrainLabel()?.let { UiText.Res(R.string.powertrain_note_specific, it) }
        ?: UiText.Res(R.string.powertrain_note_all)

data class SymptomEscalation(
    val condition: String,
    val level: SymptomRiskLevel
)

enum class SymptomCategory(@StringRes val labelRes: Int) {
    NOISE_VIBRATION(R.string.symptom_cat_noise_vibration),
    DRIVING_STEERING(R.string.symptom_cat_driving_steering),
    BRAKE(R.string.symptom_cat_brake),
    TIRE_WHEEL(R.string.symptom_cat_tire_wheel),
    SUSPENSION(R.string.symptom_cat_suspension),
    ENGINE_START(R.string.symptom_cat_engine_start),
    COOLING(R.string.symptom_cat_cooling),
    TRANSMISSION(R.string.symptom_cat_transmission),
    ELECTRIC_WARNING(R.string.symptom_cat_electric_warning),
    AC(R.string.symptom_cat_ac),
    SMELL_SMOKE_LEAK(R.string.symptom_cat_smell_smoke_leak);

    val label: UiText get() = UiText.Res(labelRes)

    companion object {
        fun fromOrNull(raw: String): SymptomCategory? = entries.firstOrNull { it.name == raw }
    }
}

/**
 * 주행위험도 — 수리비나 고장의 심각성이 아니라, 이 증상인 채로
 * 계속 운행했을 때 안전에 미칠 가능성을 나타내는 참고 지표.
 */
enum class SymptomRiskLevel(
    val level: Int,
    @StringRes val labelRes: Int,
    @StringRes val guidanceRes: Int
) {
    CHECK(1, R.string.risk_check_label, R.string.risk_check_guidance),
    CAUTION(2, R.string.risk_caution_label, R.string.risk_caution_guidance),
    MINIMIZE(3, R.string.risk_minimize_label, R.string.risk_minimize_guidance),
    STOP(4, R.string.risk_stop_label, R.string.risk_stop_guidance);

    val label: UiText get() = UiText.Res(labelRes)
    val guidance: UiText get() = UiText.Res(guidanceRes)

    companion object {
        /** 데이터 오류로 범위 밖 값이 오면 보수적으로 한 단계 높은 쪽을 택한다 */
        fun fromLevel(level: Int): SymptomRiskLevel =
            entries.firstOrNull { it.level == level }
                ?: if (level > 4) STOP else CAUTION
    }
}

/**
 * 증상명·검색 키워드에 대한 부분 일치 검색.
 * 공백을 무시해 "핸들떨림"과 "핸들 떨림"을 같게 취급한다.
 */
fun Symptom.matches(query: String): Boolean {
    val q = query.replace(" ", "")
    if (q.isBlank()) return true
    val inTitle = title.replace(" ", "").contains(q)
    return inTitle || keywords.any { it.replace(" ", "").contains(q) }
}

fun filterSymptoms(
    all: List<Symptom>,
    category: SymptomCategory?,
    query: String
): List<Symptom> = all
    .filter { category == null || it.category == category }
    .filter { it.matches(query) }
