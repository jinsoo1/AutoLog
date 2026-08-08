package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.presentation.component.CarSwitcherChip
import com.jsworld.android.autolog.presentation.component.MonthlyFuelCostChart
import com.jsworld.android.autolog.presentation.model.FuelAmountCalc
import com.jsworld.android.autolog.presentation.viewModel.FuelViewModel
import java.time.LocalDate

/**
 * 주유 탭 — 연비 대신 **지출**을 중심으로 본다.
 *
 * 차량이 쓰는 에너지 종류에 따라 화면이 달라진다.
 * - 한 종류(가솔린·디젤·전기 등): 그 종류만 보여준다. 전기를 안 쓰는 차량엔 충전 관련 표시가 없다.
 * - 두 종류(플러그인 하이브리드): 주유·충전 지출을 나란히 보여주고,
 *   그래프는 쌓은 막대로 월 합계를, 내역은 종류를 구분해 한 줄기로 보여준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTabScreen(
    car: Car?,
    onSwitchCar: () -> Unit,
    onAddFuel: (FuelUnit) -> Unit,
    onEditFuel: (Long) -> Unit,
    viewModel: FuelViewModel = hiltViewModel()
) {
    // 입력 기준: 지금 이 차량이 무엇을 넣을 수 있나
    val supportedUnits = remember(car?.fuelType) { FuelUnit.supportedUnits(car?.fuelType) }
    val canAddMultipleKinds = supportedUnits.size > 1
    val primaryUnit = supportedUnits.first()

    var showKindSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (car != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        // 종류가 하나면 바로 입력 화면으로, 둘이면 무엇을 넣을지 먼저 묻는다.
                        if (canAddMultipleKinds) showKindSheet = true else onAddFuel(primaryUnit)
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (canAddMultipleKinds) "기록 추가" else "${primaryUnit.actionLabel} 기록") }
                )
            }
        },
        containerColor = Color.Transparent,
        // 이 화면은 MainTabScreen 의 Scaffold 안에 들어간다. 바깥에서 이미
        // 탭바·시스템 내비게이션 인셋을 뺐으므로 여기서 또 빼면
        // 탭바 위에 빈 여백이 생기고 FAB 가 그만큼 떠오르며 스크롤 영역이 짧아진다.
        // (상단 상태바 여백은 아래 헤더 Row 에서 statusBarsPadding 으로 직접 처리한다)
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarSwitcherChip(car = car, onClick = onSwitchCar)
            }

            if (car == null) {
                FuelEmptyMessage("차량을 먼저 추가해주세요", "위 차량 칩에서 차량을 추가할 수 있어요.")
                return@Column
            }

            val records by viewModel.recordsState(car.id).collectAsState()
            val monthly by viewModel.monthlyCostState(car.id).collectAsState()

            if (records.isEmpty()) {
                FuelEmptyMessage(
                    if (canAddMultipleKinds) "아직 주유·충전 기록이 없어요"
                    else "아직 ${primaryUnit.actionLabel} 기록이 없어요",
                    "오른쪽 아래 버튼으로 첫 기록을 남겨보세요."
                )
                return@Column
            }

            val unitsInRecords = remember(records) { records.map { it.unit }.distinct() }

            // 표시 기준: 실제 기록에 있는 종류까지 포함한다.
            // 연료 타입을 바꿨더라도(예: PHEV → 전기) 과거 주유 기록을 숨기거나
            // 합계에서 빼먹으면 안 되기 때문이다.
            val displayUnits = remember(unitsInRecords, car.fuelType) {
                FuelUnit.displayUnits(unitsInRecords, car.fuelType)
            }
            val isMixed = displayUnits.size > 1

            val thisMonth = remember {
                LocalDate.now().let { "%04d-%02d".format(it.year, it.monthValue) }
            }

            // 이번 달 지출을 종류별로 나눠 둔다.
            val thisMonthByUnit = remember(records, thisMonth) {
                records
                    .filter { it.filledAt.startsWith(thisMonth) }
                    .groupBy { it.unit }
                    .mapValues { (_, list) -> list.sumOf { it.amount ?: 0 } }
            }

            var filter by rememberSaveable(car.id) { mutableStateOf<FuelUnit?>(null) }
            val activeFilter = filter?.takeIf { it in unitsInRecords }
            val shown = remember(records, activeFilter) {
                if (activeFilter == null) records else records.filter { it.unit == activeFilter }
            }

            // "이전 기록 이후 주행거리"는 같은 종류끼리 비교하면 의미가 없다.
            // 주유든 충전이든 그 사이에 실제로 달린 거리이므로 종류를 섞어 계산한다.
            val drivenByRecordId = remember(records) { records.drivenDistances() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (isMixed) {
                        // 주유비와 충전비는 단위가 달라 합쳐 평균낼 수 없으므로 나란히 보여준다.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            displayUnits.forEach { unit ->
                                FuelStatCard(
                                    label = "이번 달 ${unit.costLabel}",
                                    value = (thisMonthByUnit[unit] ?: 0).formatThousands(),
                                    unit = "원",
                                    accent = unit.accentColor(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // 단위가 하나뿐이라 평균 단가가 성립한다.
                        val onlyUnit = displayUnits.first()
                        val averageUnitPrice = remember(records) { records.averageUnitPrice() }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FuelStatCard(
                                label = "이번 달 ${onlyUnit.costLabel}",
                                value = (thisMonthByUnit[onlyUnit] ?: 0).formatThousands(),
                                unit = "원",
                                modifier = Modifier.weight(1f)
                            )
                            FuelStatCard(
                                label = "평균 단가",
                                value = averageUnitPrice?.formatThousands() ?: "-",
                                unit = "원/${onlyUnit.symbol}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (isMixed) {
                    item {
                        val total = thisMonthByUnit.values.sum()
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "이번 달 총 에너지비 ${total.formatThousands()}원",
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (monthly.isNotEmpty()) {
                    item {
                        SectionLabel(
                            if (isMixed) "월 에너지비" else "월 ${displayUnits.first().costLabel}"
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            MonthlyFuelCostChart(
                                data = monthly,
                                modifier = Modifier.padding(
                                    start = 10.dp, end = 10.dp, top = 12.dp, bottom = 10.dp
                                )
                            )
                        }
                    }
                }

                item { SectionLabel(if (isMixed) "내역" else "${displayUnits.first().actionLabel} 내역") }

                // 두 종류가 섞여 있을 때만 필터를 보여준다.
                if (unitsInRecords.size > 1) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            FilterChip(
                                selected = activeFilter == null,
                                onClick = { filter = null },
                                label = { Text("전체") }
                            )
                            unitsInRecords
                                .sortedBy { if (it.isElectric) 1 else 0 }
                                .forEach { unit ->
                                    FilterChip(
                                        selected = activeFilter == unit,
                                        onClick = {
                                            filter = if (activeFilter == unit) null else unit
                                        },
                                        label = { Text(unit.actionLabel) }
                                    )
                                }
                        }
                    }
                }

                item {
                    ListCard {
                        shown.forEachIndexed { index, record ->
                            FuelRecordRow(
                                record = record,
                                drivenKm = drivenByRecordId[record.id],
                                showKindBadge = isMixed,
                                showDivider = index != shown.lastIndex,
                                onClick = { onEditFuel(record.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKindSheet) {
        FuelKindSheet(
            units = supportedUnits,
            onSelect = { unit ->
                showKindSheet = false
                onAddFuel(unit)
            },
            onDismiss = { showKindSheet = false }
        )
    }
}

/**
 * 무엇을 기록할지 고르는 시트. 플러그인 하이브리드에서만 나온다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelKindSheet(
    units: List<FuelUnit>,
    onSelect: (FuelUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                "무엇을 기록할까요?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "주유와 충전을 따로 남길 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            units.forEach { unit ->
                val accent = unit.accentColor()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { onSelect(unit) },
                    color = accent.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = accent, shape = CircleShape) {
                            Icon(
                                unit.icon(),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "${unit.actionLabel} 기록",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Text(
                                "금액 · ${unit.quantityLabel}(${unit.symbol}) · 단가",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelStatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (accent != null) {
                    Surface(color = accent, shape = CircleShape) {
                        Spacer(Modifier.size(7.dp))
                    }
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FuelRecordRow(
    record: FuelRecord,
    drivenKm: Int?,
    showKindBadge: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val accent = record.unit.accentColor()

    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = accent.copy(alpha = 0.15f), shape = CircleShape) {
                Icon(
                    record.unit.icon(),
                    contentDescription = record.unit.actionLabel,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(15.dp),
                    tint = accent
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.titleLine(),
                        // 제목이 길어도 배지가 밀려나지 않도록 제목 쪽이 줄어들게 한다.
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 종류가 섞여 있을 때만 배지를 붙인다(단일 종류에선 군더더기다).
                    if (showKindBadge) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = accent.copy(alpha = 0.14f), shape = CircleShape) {
                            Text(
                                record.unit.actionLabel,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                    }
                }
                Text(
                    record.subLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (drivenKm != null) {
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "+${drivenKm.formatThousands()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "km 주행",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showDivider) RowDivider()
    }
}

@Composable
private fun FuelEmptyMessage(title: String, body: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                Icons.Default.LocalGasStation,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 충전은 tertiary, 주유는 primary 로 구분한다. */
@Composable
internal fun FuelUnit.accentColor(): Color =
    if (isElectric) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

internal fun FuelUnit.icon() =
    if (isElectric) Icons.Default.Bolt else Icons.Default.LocalGasStation

/** "8월 2일 · 71,400원" */
private fun FuelRecord.titleLine(): String {
    val date = filledAt.toDisplayDateOrNull() ?: filledAt
    val cost = amount?.let { "${it.formatThousands()}원" }
    return listOfNotNull(date, cost).joinToString(" · ")
}

/** "42.5L · 1,680원/L · SK 강남" */
private fun FuelRecord.subLine(): String = buildList {
    quantity?.let { add("${FuelAmountCalc.formatQuantity(it)}${unit.symbol}") }
    unitPrice?.let { add("${it.formatThousands()}원/${unit.symbol}") }
    station?.takeIf { it.isNotBlank() }?.let { add(it) }
}.joinToString(" · ").ifBlank { "기록" }

/**
 * 평균 단가. 단일 종류 차량에서만 쓴다(단위가 섞이면 의미가 없다).
 */
private fun List<FuelRecord>.averageUnitPrice(): Int? {
    val totalAmount = sumOf { it.amount ?: 0 }
    val totalQuantity = sumOf { it.quantity ?: 0.0 }
    if (totalAmount > 0 && totalQuantity > 0.0) {
        return (totalAmount / totalQuantity).toInt()
    }
    val prices = mapNotNull { it.unitPrice }.filter { it > 0 }
    return prices.takeIf { it.isNotEmpty() }?.average()?.toInt()
}

/**
 * 각 기록의 "이전 기록 이후 주행거리". 최신순 목록에서 바로 다음 항목이 이전 기록이다.
 * 주유·충전을 섞어서 계산한다 — 그 사이에 실제로 달린 거리이기 때문이다.
 * 주행거리가 비어 있거나 줄어든 경우(오타 등)는 표시하지 않는다.
 */
private fun List<FuelRecord>.drivenDistances(): Map<Long, Int> {
    val result = mutableMapOf<Long, Int>()
    forEachIndexed { index, record ->
        val current = record.mileage ?: return@forEachIndexed
        val previous = getOrNull(index + 1)?.mileage ?: return@forEachIndexed
        val delta = current - previous
        if (delta > 0) result[record.id] = delta
    }
    return result
}
