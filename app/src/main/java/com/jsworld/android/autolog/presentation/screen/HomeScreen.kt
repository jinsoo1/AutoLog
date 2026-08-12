package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.presentation.component.CarSwitcherChip
import com.jsworld.android.autolog.presentation.model.FuelAmountCalc
import com.jsworld.android.autolog.presentation.viewModel.HomeViewModel
import java.text.NumberFormat
import java.time.LocalDate

private const val NEXT_MAINTENANCE_PREVIEW = 3
private const val RECENT_RECORD_PREVIEW = 3

/**
 * 홈 탭 — "지금 이 차의 상태"를 보여준다.
 * 기록 열람은 정비 탭이 담당하고, 여기서는 요약과 임박 항목만 다룬다.
 */
@Composable
fun HomeScreen(
    car: Car?,
    onSwitchCar: () -> Unit,
    onNoticeClick: () -> Unit,
    onEditCar: (Long) -> Unit,
    onAddMaintenance: (carId: Long, settingId: Long?) -> Unit,
    onOpenItemDetail: (Long) -> Unit,
    onSeeAllRecords: () -> Unit,
    onSeeAllFuel: () -> Unit,
    onOpenReport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    Column(Modifier.fillMaxSize()) {

        // 상단 바 — 차량 전환 / 차량 정보 수정 / 공지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarSwitcherChip(car = car, onClick = onSwitchCar)
            Spacer(Modifier.weight(1f))
            if (car != null) {
                IconButton(onClick = { onEditCar(car.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "차량 정보 수정")
                }
            }
            IconButton(onClick = onNoticeClick) {
                Icon(Icons.Default.Campaign, contentDescription = "공지사항")
            }
        }

        if (car == null) {
            HomeEmptyView()
            return@Column
        }

        val overview by viewModel.overviewState(car.id).collectAsState()
        val records by viewModel.recordsState(car.id).collectAsState()
        val maxServiceMileage by viewModel.maxServiceMileageState(car.id).collectAsState()

        var showMileageDialog by rememberSaveable(car.id) { mutableStateOf(false) }

        // 기록이 없는 항목은 0km/오늘 기준 계산이라 "가짜 초과"로 뜬다 — 홈의 빨간 카드에서
        // 제외한다(알림과 같은 원칙). 대신 정비 탭 상단 배너가 첫 기록 입력을 안내한다.
        val urgent = remember(overview) {
            overview.filter { it.status != MaintenanceStatus.NORMAL && it.hasHistory }
        }
        val next = remember(overview) {
            overview.filter { it.status == MaintenanceStatus.NORMAL }.take(NEXT_MAINTENANCE_PREVIEW)
        }

        val fuelRecords by viewModel.fuelRecordsState(car.id).collectAsState()
        // 라벨은 **실제 기록에 있는 종류**까지 반영해야 한다.
        // 값은 전체 합계인데 라벨만 차량 설정을 따르면
        // "이번 달 충전비"라면서 주유비까지 더한 금액이 나온다.
        val displayUnits = remember(fuelRecords, car.fuelType) {
            FuelUnit.displayUnits(fuelRecords.map { it.unit }, car.fuelType)
        }
        val isMixed = displayUnits.size > 1

        // 이번 달 지출 = 주유·충전 + 정비·수리 + 세차(금액 입력된 기록만).
        // 상세 분해는 리포트가 담당하고, 여기서는 합계만 보여준다.
        val careRecords by viewModel.careRecordsState(car.id).collectAsState()
        val thisMonthExpense = remember(fuelRecords, records, careRecords) {
            val prefix = LocalDate.now().let { "%04d-%02d".format(it.year, it.monthValue) }
            fuelRecords.filter { it.filledAt.startsWith(prefix) }.sumOf { it.amount ?: 0 } +
                records.filter { it.serviceDate?.startsWith(prefix) == true }.sumOf { it.cost ?: 0 } +
                careRecords.filter { it.performedAt?.startsWith(prefix) == true }.sumOf { it.cost ?: 0 }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "주행거리",
                        value = car.mileage.formatThousands(),
                        unit = "km",
                        caption = "탭해서 업데이트",
                        modifier = Modifier.weight(1f),
                        onClick = { showMileageDialog = true }
                    )
                    StatCard(
                        label = "이번 달 지출",
                        value = thisMonthExpense.formatThousands(),
                        unit = "원",
                        caption = "탭해서 리포트 보기",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenReport
                    )
                }
            }

            if (urgent.isEmpty()) {
                item { AllGoodCard() }
            } else {
                items(items = urgent, key = { it.settingId }) { item ->
                    UrgentCard(
                        item = item,
                        onClick = { onAddMaintenance(car.id, item.settingId) }
                    )
                }
            }

            if (next.isNotEmpty()) {
                item { SectionLabel("다음 정비") }
                item {
                    ListCard {
                        next.forEachIndexed { index, item ->
                            NextMaintenanceRow(
                                item = item,
                                showDivider = index != next.lastIndex,
                                onClick = { onOpenItemDetail(item.settingId) }
                            )
                        }
                    }
                }
            }

            if (records.isNotEmpty()) {
                item {
                    SectionLabel(
                        title = "최근 정비",
                        actionLabel = "전체 보기",
                        onAction = onSeeAllRecords
                    )
                }
                item {
                    val recent = records.take(RECENT_RECORD_PREVIEW)
                    ListCard {
                        recent.forEachIndexed { index, record ->
                            RecentRecordRow(
                                record = record,
                                showDivider = index != recent.lastIndex
                            )
                        }
                    }
                }
            }

            if (fuelRecords.isNotEmpty()) {
                item {
                    SectionLabel(
                        title = if (isMixed) "최근 주유·충전" else "최근 ${displayUnits.first().actionLabel}",
                        actionLabel = "전체 보기",
                        onAction = onSeeAllFuel
                    )
                }
                item {
                    val recentFuel = fuelRecords.take(RECENT_RECORD_PREVIEW)
                    ListCard {
                        recentFuel.forEachIndexed { index, record ->
                            RecentFuelRow(
                                unit = record.unit,
                                dateLabel = record.filledAt.toDisplayDateOrNull() ?: record.filledAt,
                                detail = buildList {
                                    record.quantity?.let {
                                        add("${FuelAmountCalc.formatQuantity(it)}${record.unit.symbol}")
                                    }
                                    record.amount?.let { add("${it.formatThousands()}원") }
                                }.joinToString(" · "),
                                showKind = isMixed,
                                showDivider = index != recentFuel.lastIndex
                            )
                        }
                    }
                }
            }
        }

        if (showMileageDialog) {
            MileageQuickEditDialog(
                currentMileage = car.mileage,
                minAllowedMileage = maxServiceMileage,
                onDismiss = { showMileageDialog = false },
                onSave = { newMileage ->
                    viewModel.updateCarMileage(car.id, newMileage)
                    showMileageDialog = false
                }
            )
        }
    }
}

@Composable
private fun HomeEmptyView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text("등록된 차량이 없어요", fontWeight = FontWeight.Bold)
            Text(
                "위 차량 칩에서 차량을 추가해주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    caption: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (caption != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UrgentCard(
    item: MaintenanceUiModel,
    onClick: () -> Unit
) {
    val overdue = item.status == MaintenanceStatus.OVERDUE
    val accent =
        if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = accent, shape = CircleShape) {
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(15.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    // "타이어 교체"처럼 이름이 이미 '교체/점검'으로 끝나면 겹쳐 붙이지 않는다.
                    // ("타이어 교체 교체 초과"가 되는 것을 방지)
                    item.name.withActionSuffix(if (overdue) "초과" else "임박"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    item.remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent
            )
        }
    }
}

@Composable
private fun AllGoodCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    "상태 좋아요",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "지금 정비가 필요한 항목이 없어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun SectionLabel(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null) {
            Spacer(Modifier.weight(1f))
            Text(
                actionLabel,
                modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (onAction != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ListCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp)) { content() }
    }
}

@Composable
private fun NextMaintenanceRow(
    item: MaintenanceUiModel,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (item.remainingText.contains("일")) Icons.Default.CalendarMonth else Icons.Default.Route,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "정상",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (showDivider) RowDivider()
    }
}

@Composable
private fun RecentRecordRow(
    record: CarMaintenanceRecord,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    record.typeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    record.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (record.cost != null) {
                Text(
                    "${record.cost.formatThousands()}원",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (showDivider) RowDivider()
    }
}

@Composable
private fun RecentFuelRow(
    unit: FuelUnit,
    dateLabel: String,
    detail: String,
    showKind: Boolean,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                unit.icon(),
                contentDescription = if (showKind) unit.actionLabel else null,
                modifier = Modifier.size(17.dp),
                // 종류가 섞여 있을 때만 색으로 구분한다.
                tint = if (showKind) unit.accentColor() else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (showKind) "$dateLabel · ${unit.actionLabel}" else dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (detail.isNotBlank()) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (showDivider) RowDivider()
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}

/** "7월 12일 · 36,800km · 정비소" 형태의 부제. */
internal fun CarMaintenanceRecord.subtitle(): String = buildList {
    serviceDate?.toDisplayDateOrNull()?.let { add(it) }
    serviceMileage?.let { add("${it.formatThousands()}km") }
    place?.takeIf { it.isNotBlank() }?.let { add(it) }
}.joinToString(" · ").ifBlank { "기록" }

internal fun String.toDisplayDateOrNull(): String? = runCatching {
    val date = LocalDate.parse(this)
    "${date.monthValue}월 ${date.dayOfMonth}일"
}.getOrNull()

internal fun Int.formatThousands(): String = NumberFormat.getIntegerInstance().format(this)

/**
 * "엔진오일" → "엔진오일 교체 초과", "타이어 교체" → "타이어 교체 초과".
 * 항목 이름이 이미 동작('교체' 등)이나 괄호 설명으로 끝나면 '교체'를 겹쳐 붙이지 않는다.
 */
private fun String.withActionSuffix(status: String): String {
    val trimmed = trimEnd()
    val core = trimmed.substringBeforeLast('(').trimEnd()  // "PCV 밸브(점검/교환)" 대응
    val endsWithAction = listOf("교체", "교환", "점검", "보충").any {
        trimmed.endsWith(it) || core.endsWith(it) || trimmed.endsWith("$it)")
    }
    return if (endsWithAction) "$trimmed $status" else "$trimmed 교체 $status"
}
