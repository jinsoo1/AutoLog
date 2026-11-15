package com.jsworld.android.autolog.presentation.screen

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.asString
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.TextButton
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
import com.jsworld.android.autolog.presentation.component.StatCard
import com.jsworld.android.autolog.presentation.component.TabTopBar
import com.jsworld.android.autolog.presentation.component.TabContentTopPadding
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
                    text = { Text(if (canAddMultipleKinds) stringResource(R.string.fuel_add_record) else stringResource(R.string.fuel_add_record_of, primaryUnit.actionLabel.asString())) }
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
            TabTopBar(leading = { CarSwitcherChip(car = car, onClick = onSwitchCar) })

            if (car == null) {
                FuelEmptyMessage(stringResource(R.string.empty_no_car_title), stringResource(R.string.empty_no_car_body))
                return@Column
            }

            val records by viewModel.recordsState(car.id).collectAsState()
            val monthly by viewModel.monthlyCostState(car.id).collectAsState()

            if (records.isEmpty()) {
                FuelEmptyMessage(
                    if (canAddMultipleKinds) stringResource(R.string.fuel_empty_title_mixed)
                    else stringResource(R.string.fuel_empty_title_of, primaryUnit.actionLabel.asString()),
                    stringResource(R.string.fuel_empty_body)
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

            // 카드 캡션용 — 금액만 있으면 "많이 쓴 건지"를 알 수 없다.
            // 몇 번 넣어서 나온 금액인지가 함께 있어야 읽힌다.
            val thisMonthCountByUnit = remember(records, thisMonth) {
                records
                    .filter { it.filledAt.startsWith(thisMonth) }
                    .groupingBy { it.unit }
                    .eachCount()
            }

            var filter by rememberSaveable(car.id) { mutableStateOf<FuelUnit?>(null) }
            val activeFilter = filter?.takeIf { it in unitsInRecords }

            // 기간 — 기본은 이번 달. 첫 기록부터 전부 늘어놓으면 쌓일수록 길어지기만 한다.
            var period by rememberSaveable(car.id) { mutableStateOf(FuelHistoryPeriod.THIS_MONTH) }
            val shown = remember(records, activeFilter, period, thisMonth) {
                records.filter { record ->
                    (activeFilter == null || record.unit == activeFilter) &&
                        (period == FuelHistoryPeriod.ALL || record.filledAt.startsWith(thisMonth))
                }
            }
            // 전체 보기는 달별로 끊어 보여준다(기록은 최신순이라 묶어도 순서가 유지된다).
            val shownByMonth = remember(shown) { shown.groupBy { it.filledAt.take(7) } }

            // "이전 기록 이후 주행거리"는 같은 종류끼리 비교하면 의미가 없다.
            // 주유든 충전이든 그 사이에 실제로 달린 거리이므로 종류를 섞어 계산한다.
            val drivenByRecordId = remember(records) { records.drivenDistances() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = TabContentTopPadding, bottom = 96.dp
                ),
                // 간격은 항목마다 직접 준다 — 기록 줄은 한 장의 카드처럼 붙어 있어야 해서 spacedBy 를 쓸 수 없다.
            ) {
                item {
                    if (isMixed) {
                        // 주유비와 충전비는 단위가 달라 합쳐 평균낼 수 없으므로 나란히 보여준다.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            displayUnits.forEach { unit ->
                                StatCard(
                                    label = stringResource(R.string.fuel_month_cost_of, unit.costLabel.asString()),
                                    value = (thisMonthByUnit[unit] ?: 0).formatThousands(),
                                    unit = stringResource(R.string.money_unit),
                                    caption = (thisMonthCountByUnit[unit] ?: 0).let {
                                        if (it > 0) stringResource(R.string.count_times, it) else stringResource(R.string.fuel_none_yet)
                                    },
                                    accentDot = unit.accentColor(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // 단위가 하나뿐이라 평균 단가가 성립한다.
                        val onlyUnit = displayUnits.first()
                        val averageUnitPrice = remember(records) { records.averageUnitPrice() }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                label = stringResource(R.string.fuel_month_cost_of, onlyUnit.costLabel.asString()),
                                value = (thisMonthByUnit[onlyUnit] ?: 0).formatThousands(),
                                unit = stringResource(R.string.money_unit),
                                caption = (thisMonthCountByUnit[onlyUnit] ?: 0).let {
                                    if (it > 0) stringResource(R.string.count_times, it) else stringResource(R.string.fuel_none_yet)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = stringResource(R.string.fuel_avg_price),
                                value = averageUnitPrice?.formatThousands() ?: "-",
                                unit = stringResource(R.string.fuel_price_unit, onlyUnit.symbol),
                                // 이번 달이 아니라 **전체 기록** 평균이다 — 그 사실을 밝힌다.
                                caption = stringResource(R.string.fuel_records_basis, records.size),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (isMixed) {
                    fuelGap()
                    item {
                        val total = thisMonthByUnit.values.sum()
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.fuel_month_total_energy, total),
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (monthly.isNotEmpty()) {
                    fuelGap()
                    item {
                        SectionLabel(
                            if (isMixed) stringResource(R.string.fuel_monthly_energy)
                            else stringResource(R.string.fuel_monthly_cost_of, displayUnits.first().costLabel.asString())
                        )
                    }
                    fuelGap()
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

                fuelGap()

                item {
                    val showingThisMonth = period == FuelHistoryPeriod.THIS_MONTH
                    val actionName = displayUnits.first().actionLabel.asString()
                    SectionLabel(
                        title = when {
                            showingThisMonth && isMixed -> stringResource(R.string.fuel_history_this_month)
                            showingThisMonth -> stringResource(R.string.fuel_history_this_month_of, actionName)
                            isMixed -> stringResource(R.string.fuel_history)
                            else -> stringResource(R.string.fuel_history_of, actionName)
                        },
                        actionLabel = stringResource(
                            if (showingThisMonth) R.string.fuel_history_show_all else R.string.fuel_history_show_this_month
                        ),
                        onAction = {
                            period = if (showingThisMonth) FuelHistoryPeriod.ALL else FuelHistoryPeriod.THIS_MONTH
                        }
                    )
                }

                // 두 종류가 섞여 있을 때만 필터를 보여준다.
                if (unitsInRecords.size > 1) {
                    fuelGap()
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            FilterChip(
                                selected = activeFilter == null,
                                onClick = { filter = null },
                                label = { Text(stringResource(R.string.common_all)) }
                            )
                            unitsInRecords
                                .sortedBy { if (it.isElectric) 1 else 0 }
                                .forEach { unit ->
                                    FilterChip(
                                        selected = activeFilter == unit,
                                        onClick = {
                                            filter = if (activeFilter == unit) null else unit
                                        },
                                        label = { Text(unit.actionLabel.asString()) }
                                    )
                                }
                        }
                    }
                }

                when {
                    // 월초처럼 이번 달 기록이 아직 없을 때 — 빈 화면 대신 전체로 가는 길을 준다
                    shown.isEmpty() -> {
                        fuelGap()
                        item {
                            ListCard {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        stringResource(R.string.fuel_history_this_month_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (period == FuelHistoryPeriod.THIS_MONTH) {
                                        TextButton(onClick = { period = FuelHistoryPeriod.ALL }) {
                                            Text(stringResource(R.string.fuel_history_show_all))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    period == FuelHistoryPeriod.THIS_MONTH -> {
                        fuelGap()
                        fuelRecordRows(shown, drivenByRecordId, isMixed, onEditFuel)
                    }

                    // 기록을 한 줄씩 lazy 항목으로 둔다. 달 묶음을 통째로 한 항목에 넣으면
                    // 그 달이 화면에 들어오는 순간 모든 줄을 한꺼번에 그려 스크롤이 끊긴다.
                    else -> shownByMonth.forEach { (monthKey, monthRecords) ->
                        fuelGap()
                        item(key = "fuel-month-header-$monthKey", contentType = "fuel-month-header") {
                            Text(
                                monthKey.toYearMonthLabel(),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        fuelRecordRows(monthRecords, drivenByRecordId, isMixed, onEditFuel)
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
                stringResource(R.string.fuel_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.fuel_sheet_subtitle),
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
                                stringResource(R.string.fuel_add_record_of, unit.actionLabel.asString()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Text(
                                stringResource(R.string.fuel_sheet_row_detail, unit.quantityLabel.asString(), unit.symbol),
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
                    contentDescription = record.unit.actionLabel.asString(),
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
                                record.unit.actionLabel.asString(),
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
                        stringResource(R.string.fuel_km_driven),
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
@Composable
@ReadOnlyComposable
private fun FuelRecord.titleLine(): String {
    val date = filledAt.toDisplayDateOrNull() ?: filledAt
    val cost = amount?.let { stringResource(R.string.money_amount, it) }
    return listOfNotNull(date, cost).joinToString(" · ")
}

/** "42.5L · 1,680원/L · SK 강남" */
@Composable
@ReadOnlyComposable
private fun FuelRecord.subLine(): String = buildList {
    quantity?.let { add("${FuelAmountCalc.formatQuantity(it)}${unit.symbol}") }
    unitPrice?.let { add(stringResource(R.string.fuel_price_per_unit, it, unit.symbol)) }
    station?.takeIf { it.isNotBlank() }?.let { add(it) }
}.joinToString(" · ").ifBlank { stringResource(R.string.common_record) }

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

/** 주유 내역 기간 */
private enum class FuelHistoryPeriod { THIS_MONTH, ALL }

/** 주유 탭 목록의 항목 간격 — LazyColumn 의 spacedBy 대신 */
private fun LazyListScope.fuelGap() {
    item(contentType = "fuel-gap") { Spacer(Modifier.height(12.dp)) }
}

/** 한 묶음(이번 달, 또는 전체 보기의 한 달)의 기록 — 줄마다 lazy 항목, 겉보기는 한 장의 카드 */
private fun LazyListScope.fuelRecordRows(
    records: List<FuelRecord>,
    drivenByRecordId: Map<Long, Int>,
    showKindBadge: Boolean,
    onEditFuel: (Long) -> Unit
) {
    itemsIndexed(records, key = { _, r -> "fuel-${r.id}" }, contentType = { _, _ -> "fuel-row" }) { index, record ->
        CardSegment(isFirst = index == 0, isLast = index == records.lastIndex) {
            FuelRecordRow(
                record = record,
                drivenKm = drivenByRecordId[record.id],
                showKindBadge = showKindBadge,
                showDivider = index != records.lastIndex,
                onClick = { onEditFuel(record.id) }
            )
        }
    }
}

/**
 * [ListCard] 를 줄 단위로 쪼갠 조각. 위아래로 붙여 놓으면 한 장의 카드처럼 보인다.
 * 첫 줄만 위 모서리, 끝 줄만 아래 모서리가 둥글고 테두리도 이어진다.
 * 가운데 조각은 위아래로 넉넉히 늘린 카드를 그린 뒤 자기 높이로 잘라, 둥근 모서리가 안 보이게 한다.
 */
@Composable
private fun CardSegment(isFirst: Boolean, isLast: Boolean, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.large
    val background = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                val extend = 48.dp.toPx() // 모서리 반경보다 넉넉히
                val top = if (isFirst) 0f else -extend
                val bottom = if (isLast) size.height else size.height + extend
                clipRect {
                    translate(left = stroke / 2, top = top + stroke / 2) {
                        val outline = shape.createOutline(
                            Size(size.width - stroke, bottom - top - stroke),
                            layoutDirection,
                            this
                        )
                        drawOutline(outline, background)
                        drawOutline(outline, borderColor, style = Stroke(width = stroke))
                    }
                }
            }
            .padding(horizontal = 14.dp)
    ) {
        content()
    }
}

/** "2026-11" → "2026년 11월" (형식이 어긋나면 그대로) */
@Composable
@ReadOnlyComposable
private fun String.toYearMonthLabel(): String {
    val ym = runCatching { java.time.YearMonth.parse(this) }.getOrNull() ?: return this
    return stringResource(R.string.date_year_month, ym.year, ym.monthValue)
}
