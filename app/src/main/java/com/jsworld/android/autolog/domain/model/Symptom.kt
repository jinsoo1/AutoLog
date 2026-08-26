package com.jsworld.android.autolog.domain.model

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
 * 동력 계통 구분. 연료 타입(가솔린·디젤·LPG·하이브리드·전기…)보다 굵게 묶는다 —
 * 증상이 갈리는 기준은 "엔진이 있나, 변속기가 있나, 배기가 있나"이기 때문이다.
 */
enum class Powertrain(val label: String) {
    ICE("내연기관"),
    HYBRID("하이브리드"),
    EV("전기"),
    FCEV("수소");

    companion object {
        fun fromOrNull(raw: String): Powertrain? = entries.firstOrNull { it.name == raw }
    }
}

/** 전 차종 공통이면 true — 배지를 띄우지 않는다(50건 넘는 항목에 같은 배지는 소음이다). */
val Symptom.appliesToAllPowertrains: Boolean
    get() = powertrains.isEmpty() || powertrains.size == Powertrain.entries.size

/** "내연기관 · 하이브리드" 같은 표시 문구. 전 차종 공통이면 null. */
fun Symptom.powertrainLabel(): String? {
    if (appliesToAllPowertrains) return null
    return Powertrain.entries
        .filter { it in powertrains }
        .joinToString(" · ") { it.label }
}

data class SymptomEscalation(
    val condition: String,
    val level: SymptomRiskLevel
)

enum class SymptomCategory(val label: String) {
    NOISE_VIBRATION("소리·진동"),
    DRIVING_STEERING("주행·조향"),
    BRAKE("브레이크"),
    TIRE_WHEEL("타이어·휠"),
    SUSPENSION("하체·서스펜션"),
    ENGINE_START("엔진·시동"),
    COOLING("냉각"),
    TRANSMISSION("변속·구동"),
    ELECTRIC_WARNING("전기·경고등"),
    AC("에어컨"),
    SMELL_SMOKE_LEAK("냄새·연기·누유");

    companion object {
        fun fromOrNull(raw: String): SymptomCategory? = entries.firstOrNull { it.name == raw }
    }
}

/**
 * 주행위험도 — 수리비나 고장의 심각성이 아니라, 이 증상인 채로
 * 계속 운행했을 때 안전에 미칠 가능성을 나타내는 참고 지표.
 */
enum class SymptomRiskLevel(val level: Int, val label: String, val guidance: String) {
    CHECK(
        1, "점검 권장",
        "현재 설명만으로는 주행 안전에 직접 영향을 줄 가능성이 낮아 보이지만, 증상이 지속되면 점검을 받아보세요."
    ),
    CAUTION(
        2, "주의 · 빠른 점검",
        "고속·장거리 주행을 피하고 가까운 시일 내에 점검을 권장해요."
    ),
    MINIMIZE(
        3, "운행 최소화",
        "안전 문제나 추가 손상 가능성이 있어요. 필요한 이동 외에는 운행을 줄이고 빠르게 점검을 받아보세요."
    ),
    STOP(
        4, "운행 중지",
        "안전한 곳에 정차하고 계속 운행하지 않는 것을 권장해요. 필요하면 견인을 고려하세요."
    );

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
