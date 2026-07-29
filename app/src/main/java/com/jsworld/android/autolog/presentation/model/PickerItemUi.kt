package com.jsworld.android.autolog.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector


enum class MaintenanceCategory(
    val label: String,
    val icon: ImageVector,
    val order: Int
) {
    ENGINE("엔진/필터", Icons.Default.Build, 0),
    BRAKE("브레이크", Icons.Default.Warning, 1),
    TIRE("타이어/하체", Icons.Default.Route, 2),
    TRANSMISSION("미션/구동", Icons.Default.Settings, 3),
    COOLING("냉각/에어컨", Icons.Default.AcUnit, 4),
    ELECTRIC("전기/점화", Icons.Default.ElectricBolt, 5),
    LIGHT("조명/와이퍼", Icons.Default.Lightbulb, 6),
    INSPECTION("검사/점검", Icons.AutoMirrored.Filled.FactCheck, 7),
    ETC("기타", Icons.Default.MoreHoriz, 99),
}

fun categoryOf(name: String): MaintenanceCategory {
    val n = name.replace(" ", "")
    return when {
        n.contains("엔진") || n.contains("에어클리너") || n.contains("흡기") || n.contains("연료필터") || n.contains("PCV") ->
            MaintenanceCategory.ENGINE
        n.contains("브레이크") ->
            MaintenanceCategory.BRAKE
        n.contains("타이어") || n.contains("얼라인") || n.contains("서스") || n.contains("부싱") || n.contains("조인트") || n.contains("하부") ->
            MaintenanceCategory.TIRE
        n.contains("미션") || n.contains("디퍼") || n.contains("트랜스퍼") || n.contains("구동") || n.contains("스티어링") ->
            MaintenanceCategory.TRANSMISSION
        n.contains("냉각") || n.contains("부동액") || n.contains("라디에이터") || n.contains("워터펌프") || n.contains("에어컨") || n.contains("냉매") ->
            MaintenanceCategory.COOLING
        n.contains("배터리") || n.contains("점화") || n.contains("코일") || n.contains("알터네이터") || n.contains("스타터") ->
            MaintenanceCategory.ELECTRIC
        n.contains("와이퍼") || n.contains("워셔") || n.contains("전조등") || n.contains("미등") || n.contains("브레이크등") ->
            MaintenanceCategory.LIGHT
        n.contains("검사") || n.contains("정기점검") || n.contains("점검") ->
            MaintenanceCategory.INSPECTION
        else -> MaintenanceCategory.ETC
    }
}

/**
 * 연료 타입별 정비 항목 적합성 판정.
 *
 * 확실히 해당 없는 항목만 숨기는 보수적 규칙을 쓴다(애매하면 표시).
 * - 전기/수소: 내연기관 전용 항목(엔진오일·점화·연료필터·미션오일·벨트류 등) 제외
 *   (냉각수·워터펌프·디퍼런셜 등은 전기차에도 있을 수 있어 유지)
 * - 디젤: 점화계(점화플러그/코일) 제외 — 디젤 엔진은 압축착화라 점화플러그가 없다
 * - 연료 미설정/기타: 전부 표시
 */
fun isItemApplicableToFuel(itemName: String, fuelType: String?): Boolean {
    val fuel = fuelType?.trim().orEmpty()
    if (fuel.isEmpty() || fuel == "기타") return true

    val n = itemName.replace(" ", "")

    // 내연기관 전용(전기/수소차에는 없음)
    val engineOnly = listOf(
        "엔진오일", "에어클리너", "흡기", "연료필터", "PCV", "스로틀",
        "점화", "알터네이터", "스타터",
        "미션오일", "파워스티어링", "보조벨트", "팬벨트", "타이밍벨트"
    )
    // 점화계(디젤 엔진에는 없음)
    val sparkOnly = listOf("점화")

    val evLike = fuel == "전기" || fuel == "수소"
    if (evLike && engineOnly.any { n.contains(it) }) return false
    if (fuel == "디젤" && sparkOnly.any { n.contains(it) }) return false
    return true
}

data class PickerItemUi(
    val typeId: Long,
    val typeName: String,
    val defaultKm: Int?,
    val defaultMonths: Int?,
    val settingId: Long?,   // 있으면 settingId
    val isActive: Boolean,
    // 추가: 현재 차량 설정 주기(커스텀)
    val intervalKm: Int?,
    val intervalMonths: Int?
) {
    val category: MaintenanceCategory = categoryOf(typeName)
}

data class CategoryGroup(
    val category: MaintenanceCategory,
    val items: List<PickerItemUi>
)

fun groupByCategory(list: List<PickerItemUi>): List<CategoryGroup> =
    list.groupBy { it.category }
        .toList()
        .sortedBy { (cat, _) -> cat.order }
        .map { (cat, items) -> CategoryGroup(cat, items.sortedBy { it.typeName }) }