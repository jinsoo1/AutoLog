package com.jsworld.android.autolog.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.jsworld.android.autolog.R

/**
 * DB 에 저장된 **한국어 원문 이름**을 표시 언어로 바꾼다.
 *
 * 기본 정비 항목·세차 항목·연료 종류·일정 프리셋은 이름 자체가 식별자다 —
 * `(carId, name)` 유니크 인덱스, 온보딩 팩의 이름 조회, 연료 타입별 적용 판정,
 * 백업 포맷, 마이그레이션 SQL 이 모두 이름 문자열에 걸려 있다. 그래서 저장값은
 * 언어와 무관하게 한국어 원문으로 고정하고, 화면에 그릴 때만 여기서 바꾼다.
 *
 * 사용자가 직접 만든 항목은 표에 없으므로 입력한 그대로 보인다.
 * 한국어 로케일에서는 기본 리소스가 원문과 같아 결과가 달라지지 않는다.
 */
object LocalizedNames {

    private val maintenanceItems: Map<String, Int> = mapOf(
        "엔진오일" to R.string.item_engine_oil,
        "엔진오일 필터" to R.string.item_engine_oil_filter,
        "에어컨(캐빈) 필터" to R.string.item_cabin_filter,
        "에어클리너(엔진흡기 필터)" to R.string.item_air_cleaner,
        "연료필터" to R.string.item_fuel_filter,
        "PCV 밸브(점검/교환)" to R.string.item_pcv_valve,
        "스로틀바디 청소" to R.string.item_throttle_body,
        "흡기/밸브 카본 청소(선택)" to R.string.item_intake_carbon,
        "점화플러그" to R.string.item_spark_plug,
        "점화코일(점검/교환)" to R.string.item_ignition_coil,
        "배터리" to R.string.item_battery,
        "발전기/알터네이터 점검" to R.string.item_alternator,
        "스타터 모터 점검" to R.string.item_starter_motor,
        "냉각수(부동액)" to R.string.item_coolant,
        "라디에이터 캡/호스 점검" to R.string.item_radiator_cap_hose,
        "워터펌프(점검)" to R.string.item_water_pump,
        "에어컨 냉매 점검/보충" to R.string.item_ac_refrigerant,
        "브레이크패드" to R.string.item_brake_pad,
        "브레이크 디스크(로터)" to R.string.item_brake_disc,
        "브레이크오일" to R.string.item_brake_fluid,
        "브레이크 라인/누유 점검" to R.string.item_brake_line,
        "타이어 교체" to R.string.item_tire_replace,
        "타이어 위치교환" to R.string.item_tire_rotation,
        "휠 얼라인먼트 점검" to R.string.item_wheel_alignment,
        "타이어 공기압 점검" to R.string.item_tire_pressure,
        "서스펜션/부싱/조인트 점검" to R.string.item_suspension,
        "하부(누유/부식) 점검" to R.string.item_underbody,
        "미션오일(AT/CVT/DCT)" to R.string.item_transmission_oil,
        "미션오일 필터(있는 경우)" to R.string.item_transmission_filter,
        "디퍼런셜 오일(후륜/4륜)" to R.string.item_differential_oil,
        "트랜스퍼 케이스 오일(4WD)" to R.string.item_transfer_case_oil,
        "파워스티어링 오일(유압식)" to R.string.item_power_steering_oil,
        "와이퍼 블레이드" to R.string.item_wiper_blade,
        "워셔액 보충" to R.string.item_washer_fluid,
        "전조등/미등/브레이크등 점검" to R.string.item_lights,
        "정기점검(정비소 점검)" to R.string.item_periodic_check,
        "종합검사/정기검사(국가검사)" to R.string.item_national_inspection,
        "보조벨트(팬벨트) 점검/교환" to R.string.item_aux_belt,
        "타이밍벨트(벨트식 엔진만)" to R.string.item_timing_belt
    )

    private val careItems: Map<String, Int> = mapOf(
        "세차" to R.string.care_item_wash,
        "실내세차" to R.string.care_item_interior_wash,
        "휠·타이어" to R.string.care_item_wheel_tire,
        "철분·타르 제거" to R.string.care_item_iron_tar,
        "유막 제거" to R.string.care_item_oil_film,
        "발수코팅" to R.string.care_item_water_repellent,
        "왁스코팅" to R.string.care_item_wax,
        "유리막코팅" to R.string.care_item_glass_coating,
        "광택" to R.string.care_item_polish,
        "실내 클리닝" to R.string.care_item_interior_cleaning
    )

    private val fuelTypes: Map<String, Int> = mapOf(
        "가솔린" to R.string.fuel_type_gasoline,
        "디젤" to R.string.fuel_type_diesel,
        "LPG" to R.string.fuel_type_lpg,
        "하이브리드" to R.string.fuel_type_hybrid,
        "플러그인 하이브리드" to R.string.fuel_type_phev,
        "전기" to R.string.fuel_type_electric,
        "수소" to R.string.fuel_type_hydrogen,
        "기타" to R.string.fuel_type_other
    )

    private val schedulePresets: Map<String, Int> = mapOf(
        "정기검사" to R.string.schedule_preset_inspection,
        "보험 만기" to R.string.schedule_preset_insurance,
        "자동차세" to R.string.schedule_preset_tax
    )

    /** 정비·세차·일정 이름. 표에 없으면 입력한 그대로 */
    fun item(name: String): UiText =
        (maintenanceItems[name] ?: careItems[name] ?: schedulePresets[name])
            ?.let { UiText.Res(it) } ?: UiText.Raw(name)

    /**
     * "엔진오일" → "엔진오일 교체", "타이어 교체" → 그대로.
     * 이름이 이미 교체/교환/점검/보충 같은 동작으로 끝나면 '교체'를 겹쳐 붙이지 않는다.
     * 판정은 저장된 한국어 원문으로 하고, 표시는 현재 언어로 한다.
     */
    fun itemAction(name: String): UiText {
        val trimmed = name.trimEnd()
        val core = trimmed.substringBeforeLast('(').trimEnd()
        val endsWithAction = listOf("교체", "교환", "점검", "보충").any {
            trimmed.endsWith(it) || core.endsWith(it) || trimmed.endsWith("$it)")
        }
        return if (endsWithAction) item(name) else UiText.Res(R.string.alert_action_replace, item(name))
    }

    private val washMethods: Map<String, Int> = mapOf(
        "셀프세차" to R.string.care_method_self,
        "자동세차" to R.string.care_method_auto,
        "손세차" to R.string.care_method_hand
    )

    /** 세차 방식. 빠른 선택 칩으로 저장된 원문만 바꾸고, 직접 입력한 방식은 그대로 */
    fun washMethod(name: String): UiText =
        washMethods[name]?.let { UiText.Res(it) } ?: UiText.Raw(name)

    /** 연료 종류. 표에 없으면 입력한 그대로 */
    fun fuelType(name: String?): UiText =
        name?.let { fuelTypes[it]?.let { id -> UiText.Res(id) } ?: UiText.Raw(it) } ?: UiText.Empty
}

/** 화면에서 항목 이름을 그릴 때 — `Text(item.name.localized())` */
@Composable
@ReadOnlyComposable
fun String.localizedItemName(): String = LocalizedNames.item(this).asString()

@Composable
@ReadOnlyComposable
fun String.localizedWashMethod(): String = LocalizedNames.washMethod(this).asString()

@Composable
@ReadOnlyComposable
fun String?.localizedFuelType(): String = LocalizedNames.fuelType(this).asString()
