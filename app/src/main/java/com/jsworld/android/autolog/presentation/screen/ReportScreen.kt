package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.model.ExpenseInsight
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.NarrativeTone
import com.jsworld.android.autolog.domain.model.ReportNarrative
import com.jsworld.android.autolog.domain.model.TopSpendItem
import com.jsworld.android.autolog.domain.model.YearHighlight
import com.jsworld.android.autolog.domain.model.buildExpenseInsight
import com.jsworld.android.autolog.domain.model.buildReportNarrative
import com.jsworld.android.autolog.domain.model.buildYearHighlights
import com.jsworld.android.autolog.domain.model.buildYearNarrative
import com.jsworld.android.autolog.domain.model.distanceLadderText
import com.jsworld.android.autolog.domain.model.earthLapsText
import com.jsworld.android.autolog.domain.model.personalRecordText
import com.jsworld.android.autolog.domain.model.topSpendItems
import com.jsworld.android.autolog.presentation.component.CarSwitcherChip
import com.jsworld.android.autolog.presentation.model.FuelAmountCalc
import com.jsworld.android.autolog.presentation.viewModel.ReportViewModel
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

/** 지출 내역 접힘 상태에서 보이는 건수 / 접기가 적용되는 최소 건수(초과 시) */
private const val ENTRY_COLLAPSED_COUNT = 6
private const val ENTRY_COLLAPSE_MIN = 8

/**
 * 지출 리포트 — 월간/연간 총지출, 카테고리 분해, km당 유지비, 월별 추이.
 *
 * 탭이 아니라 홈의 '이번 달 지출' 카드(와 설정)에서 진입하는 별도 화면이다.
 * 원칙: 계산할 수 없는 값은 "-" 로 두고 이유를 밝힌다. 숫자를 지어내지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    car: Car?,
    onBack: () -> Unit,
    onSwitchCar: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "지출 리포트",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { scaffoldPadding ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarSwitcherChip(car = car, onClick = onSwitchCar)
        }

        if (car == null) {
            ReportEmptyMessage("차량을 먼저 추가해주세요", "위 차량 칩에서 차량을 추가할 수 있어요.")
            return@Column
        }

        val expenses by viewModel.expensesState(car.id).collectAsState()

        val loaded = expenses ?: return@Column // 로딩 중 — 빈 상태 깜빡임 방지
        if (loaded.isEmpty()) {
            ReportEmptyMessage(
                "아직 리포트에 담을 기록이 없어요",
                "주유·정비 기록을 남기면 지출 리포트가 채워져요."
            )
            return@Column
        }

        var yearly by rememberSaveable(car.id) { mutableStateOf(false) }
        // 선택된 달 — 기본은 이번 달(목록의 마지막)
        var selectedMonth by rememberSaveable(car.id) {
            mutableStateOf(loaded.last().month.toString())
        }
        var selectedYear by rememberSaveable(car.id) {
            mutableStateOf(loaded.last().month.year)
        }

        val monthKeys = loaded.map { it.month.toString() }
        val current = loaded.firstOrNull { it.month.toString() == selectedMonth } ?: loaded.last()
        val years = loaded.map { it.month.year }.distinct()

        // 지출 내역 리스트·주유 요약용 원본 기록 (세차는 별도 테이블)
        val fuelRecords by viewModel.fuelRecordsState(car.id).collectAsState()
        val maintRecords by viewModel.maintenanceRecordsState(car.id).collectAsState()
        val careRecords by viewModel.careRecordsState(car.id).collectAsState()

        // 다가오는 지출 카드용 — 임박·초과 항목 + 항목별 지난 교체 비용.
        // 기록 없는 항목은 가짜 초과라 제외한다(홈·알림과 같은 원칙).
        val urgentAll by viewModel.urgentState(car.id).collectAsState()
        val urgentItems = remember(urgentAll) { urgentAll.filter { it.hasHistory } }
        val lastCosts by viewModel.lastCostsState(car.id).collectAsState()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !yearly,
                        onClick = { yearly = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("월간") }
                    SegmentedButton(
                        selected = yearly,
                        onClick = { yearly = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("연간") }
                }
            }

            if (!yearly) {
                item {
                    PeriodSelector(
                        label = "${current.month.year}년 ${current.month.monthValue}월",
                        canPrev = monthKeys.indexOf(current.month.toString()) > 0,
                        canNext = monthKeys.indexOf(current.month.toString()) < monthKeys.lastIndex,
                        onPrev = {
                            val i = monthKeys.indexOf(current.month.toString())
                            if (i > 0) selectedMonth = monthKeys[i - 1]
                        },
                        onNext = {
                            val i = monthKeys.indexOf(current.month.toString())
                            if (i < monthKeys.lastIndex) selectedMonth = monthKeys[i + 1]
                        }
                    )
                }
                val monthKey = current.month.toString()
                val isLatestMonth = monthKey == monthKeys.last()
                val monthFuel = fuelRecords.filter { it.filledAt.startsWith(monthKey) }
                val monthMaint = maintRecords.filter { it.serviceDate?.startsWith(monthKey) == true }
                val monthCare = careRecords.filter { it.performedAt?.startsWith(monthKey) == true }
                val prevMonthKey = current.month.minusMonths(1).toString()
                val prevMonthFuel = fuelRecords.filter { it.filledAt.startsWith(prevMonthKey) }

                item {
                    // 임박/초과 개수는 "지금" 상태라 이번 달을 볼 때만 반영한다.
                    val narrative = buildReportNarrative(
                        current = current,
                        previous = loaded.getOrNull(monthKeys.indexOf(monthKey) - 1),
                        overdueCount = if (isLatestMonth)
                            urgentItems.count { it.status == MaintenanceStatus.OVERDUE } else 0,
                        soonCount = if (isLatestMonth)
                            urgentItems.count { it.status == MaintenanceStatus.SOON } else 0
                    )
                    NarrativeCard(narrative)
                }

                item {
                    val prev = loaded.getOrNull(monthKeys.indexOf(current.month.toString()) - 1)
                    // 증가 원인을 항목명으로 짚기 위한 이번 달 최대 정비·수리 지출
                    val topMaint = monthMaint
                        .filter { (it.cost ?: 0) > 0 }
                        .maxByOrNull { it.cost!! }
                    TotalCard(
                        title = "${current.month.monthValue}월 총지출",
                        fuel = current.fuelCost,
                        maintenance = current.maintenanceCost,
                        care = current.careCost,
                        missingCostCount = current.missingCostCount,
                        insight = prev?.let {
                            buildExpenseInsight(current, it, topMaint?.typeName, topMaint?.cost)
                        }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            label = "km당 유지비",
                            value = current.costPerKm?.let { "${it.formatWon()}원" },
                            emptyHint = "주행거리 기록 필요",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "이 달 주행",
                            value = current.drivenKm?.let { "${it.formatWon()}km" },
                            emptyHint = "주행거리 기록 필요",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 재미 지표 — 누적 지구 바퀴 + 자기 기록(없으면 거리 비유)
                item {
                    val secondLine = personalRecordText(loaded, current)
                        ?: current.drivenKm?.let { distanceLadderText(it) }
                    FunFactCard(
                        earthLine = earthLapsText(car.mileage),
                        totalMileage = car.mileage,
                        secondLine = secondLine
                    )
                }

                // 다가오는 지출 — 이번 달을 보고 있을 때만. 과거 달에선 어색하다.
                if (isLatestMonth && urgentItems.isNotEmpty()) {
                    item { SectionLabel("다가오는 지출") }
                    item { UpcomingCard(urgentItems, lastCosts) }
                }

                if (monthFuel.isNotEmpty()) {
                    item { FuelSummaryCard(monthFuel, prevMonthFuel) }
                }

                val entries = buildMonthEntries(monthFuel, monthMaint, monthCare)
                if (entries.isNotEmpty()) {
                    item { SectionLabel("지출 내역 · ${entries.size}건") }
                    item {
                        // 기록이 많은 달엔 카드가 화면을 다 먹는다 — 6건까지만 보이고 접는다.
                        // 7~8건에 "외 1~2건" 펼치기가 생기는 게 더 어색해서 9건부터 적용.
                        var expanded by rememberSaveable(monthKey) { mutableStateOf(false) }
                        val collapsible = entries.size > ENTRY_COLLAPSE_MIN
                        val visible =
                            if (!collapsible || expanded) entries
                            else entries.take(ENTRY_COLLAPSED_COUNT)

                        ListCard {
                            Column {
                                visible.forEachIndexed { index, entry ->
                                    ExpenseEntryRow(
                                        entry,
                                        showDivider = index != visible.lastIndex || collapsible
                                    )
                                }
                                if (collapsible) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = !expanded }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (expanded) "접기"
                                            else "외 ${entries.size - ENTRY_COLLAPSED_COUNT}건 모두 보기",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            if (expanded) Icons.Filled.ExpandLess
                                            else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { SectionLabel("월별 추이") }
                item {
                    TrendCard(
                        months = loaded.takeLast(12),
                        selectedKey = current.month.toString(),
                        onSelect = { selectedMonth = it }
                    )
                }
            } else {
                val yearMonths = loaded.filter { it.month.year == selectedYear }
                val yearIndex = years.indexOf(selectedYear)
                val yearPrefix = "$selectedYear-"
                val prevYearPrefix = "${selectedYear - 1}-"
                val yearFuel = fuelRecords.filter { it.filledAt.startsWith(yearPrefix) }
                val yearMaint = maintRecords.filter { it.serviceDate?.startsWith(yearPrefix) == true }
                val yearCare = careRecords.filter { it.performedAt?.startsWith(yearPrefix) == true }
                val prevYearFuel = fuelRecords.filter { it.filledAt.startsWith(prevYearPrefix) }

                item {
                    PeriodSelector(
                        label = "${selectedYear}년",
                        canPrev = yearIndex > 0,
                        canNext = yearIndex < years.lastIndex,
                        onPrev = { if (yearIndex > 0) selectedYear = years[yearIndex - 1] },
                        onNext = { if (yearIndex < years.lastIndex) selectedYear = years[yearIndex + 1] }
                    )
                }

                item {
                    val prevYearTotal = loaded
                        .filter { it.month.year == selectedYear - 1 }
                        .takeIf { it.isNotEmpty() }
                        ?.sumOf { it.total }
                    val yearRepairs = yearMaint.filter { it.isRepair }
                    NarrativeCard(
                        buildYearNarrative(
                            yearTotal = yearMonths.sumOf { it.total },
                            prevYearTotal = prevYearTotal,
                            repairCount = yearRepairs.size,
                            isCompleteYear = selectedYear < loaded.last().month.year,
                            maxRepairCost = yearRepairs.maxOfOrNull { (it.cost ?: 0).toLong() } ?: 0L
                        )
                    )
                }

                item {
                    TotalCard(
                        title = "${selectedYear}년 총지출",
                        fuel = yearMonths.sumOf { it.fuelCost },
                        maintenance = yearMonths.sumOf { it.maintenanceCost },
                        care = yearMonths.sumOf { it.careCost },
                        missingCostCount = yearMonths.sumOf { it.missingCostCount }
                    )
                }
                item {
                    val knownKm = yearMonths.mapNotNull { it.drivenKm }
                    val totalKm = knownKm.sum()
                    val total = yearMonths.sumOf { it.total }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            label = "km당 유지비",
                            value = if (totalKm > 0) "${(total / totalKm).formatWon()}원" else null,
                            emptyHint = "주행거리 기록 필요",
                            caption = if (knownKm.size < yearMonths.size && totalKm > 0)
                                "주행거리 파악된 달 기준" else null,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "연 주행",
                            value = if (totalKm > 0) "${totalKm.formatWon()}km" else null,
                            emptyHint = "주행거리 기록 필요",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    val total = yearMonths.sumOf { it.total }
                    val top = yearMonths.maxByOrNull { it.total }?.takeIf { it.total > 0L }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            label = "월평균 지출",
                            value = if (yearMonths.isNotEmpty() && total > 0L)
                                "${(total / yearMonths.size).formatWon()}원" else null,
                            emptyHint = "지출 기록 필요",
                            caption = "기록 시작 후 ${yearMonths.size}개월 기준",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "지출 최다 달",
                            value = top?.let { "${it.month.monthValue}월" },
                            emptyHint = "지출 기록 필요",
                            caption = top?.let { "${it.total.formatWon()}원" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val topItems = topSpendItems(yearMaint, yearCare)
                if (topItems.isNotEmpty()) {
                    item { SectionLabel("항목별 지출 TOP") }
                    item { TopSpendCard(topItems) }
                }

                val highlights =
                    buildYearHighlights(yearMonths, yearFuel, yearMaint, prevYearFuel, yearCare)
                if (highlights.isNotEmpty()) {
                    item { SectionLabel("올해의 기록") }
                    item { YearHighlightsCard(highlights) }
                }

                item { SectionLabel("월별 지출") }
                item {
                    TrendCard(
                        months = yearMonths,
                        selectedKey = null,
                        onSelect = {
                            selectedMonth = it
                            yearly = false
                        }
                    )
                }
            }
        }
    }
    }
}

/* ───────────────────────── 구성 요소 ───────────────────────── */

@Composable
private fun PeriodSelector(
    label: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = canPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전")
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음")
        }
    }
}

@Composable
private fun TotalCard(
    title: String,
    fuel: Long,
    maintenance: Long,
    care: Long,
    missingCostCount: Int,
    /** 전월 대비 요약. null 이면 비교 대상 없음 */
    insight: ExpenseInsight? = null
) {
    val total = fuel + maintenance + care
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (total > 0L) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        fuel = fuel,
                        maintenance = maintenance,
                        care = care,
                        centerLabel = title
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        CategoryLegendRow("주유·충전", fuel, total, MaterialTheme.colorScheme.primary)
                        CategoryLegendRow("정비·수리", maintenance, total, MaterialTheme.colorScheme.secondary)
                        CategoryLegendRow("세차·관리", care, total, MaterialTheme.colorScheme.tertiary)
                    }
                }
            } else {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "0원",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "이 기간엔 지출 기록이 없어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (insight != null) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        insight.headline,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            insight.direction > 0 -> MaterialTheme.colorScheme.error
                            insight.direction < 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    insight.detail?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (missingCostCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "금액이 입력되지 않은 정비 기록 ${missingCostCount}건은 합계에서 빠져 있어요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 카테고리 비중 도넛 — 가운데에 총액. 얇은 막대보다 비중이 눈에 들어온다.
 */
@Composable
private fun DonutChart(
    fuel: Long,
    maintenance: Long,
    care: Long,
    centerLabel: String
) {
    val total = fuel + maintenance + care
    val segments = listOf(
        fuel to MaterialTheme.colorScheme.primary,
        maintenance to MaterialTheme.colorScheme.secondary,
        care to MaterialTheme.colorScheme.tertiary
    ).filter { it.first > 0L }

    Box(Modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 15.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            // 조각 사이 살짝 틈을 줘서 경계가 보이게 한다. 조각이 하나면 틈 없음.
            val gap = if (segments.size > 1) 3f else 0f
            var start = -90f
            segments.forEach { (value, color) ->
                val sweep = value.toFloat() / total * 360f
                drawArc(
                    color = color,
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatCompactWon(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(label: String, value: Long, total: Long, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (value > 0L) color
                    else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${value.formatWon()}원",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (value > 0L) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (value > 0L) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (value > 0L && total > 0L) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${(value * 100.0 / total).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 다가오는 지출 — 임박·초과 항목에 지난 교체 비용을 붙여 "곧 나갈 돈"을 보여준다.
 * 다음 비용 = 지난번 비용이라는 가정이므로 '약 ~' 근사로만 말한다.
 */
@Composable
private fun UpcomingCard(
    items: List<MaintenanceUiModel>,
    lastCosts: Map<Long, Int?>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "교체 시기가 다가온 항목의 지난 교체 비용 기준 예상이에요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                if (item.status == MaintenanceStatus.OVERDUE)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            item.remainingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val cost = lastCosts[item.settingId]
                    Text(
                        cost?.let { "약 ${formatCompactWon(it.toLong())}원" } ?: "비용 미상",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (cost != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (cost != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val known = items.mapNotNull { lastCosts[it.settingId] }
            if (known.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "예상 지출 합계",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "약 ${formatCompactWon(known.sumOf { it.toLong() })}원",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String?,
    emptyHint: String,
    caption: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (value != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (value != null) caption ?: " " else emptyHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 월별 지출 막대 — 카테고리 색으로 쌓아서 "이 달은 정비 때문에 튀었네"가 보이게 한다.
 * 점선은 표시 기간 평균. 막대를 누르면 그 달로 이동한다.
 */
@Composable
private fun TrendCard(
    months: List<MonthlyExpense>,
    selectedKey: String?,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val max = months.maxOfOrNull { it.total }?.coerceAtLeast(1L) ?: 1L
        val avg = if (months.isNotEmpty()) months.sumOf { it.total } / months.size else 0L
        val chartHeight = 88f

        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Box {
                // 평균 점선 — 어느 달이 평균보다 튀었는지 기준선이 된다.
                if (avg > 0L) {
                    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(chartHeight.dp)
                    ) {
                        val y = size.height * (1f - (avg.toFloat() / max).coerceIn(0f, 1f))
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    months.forEach { m ->
                        val key = m.month.toString()
                        val selected = key == selectedKey
                        val dimmed = selectedKey != null && !selected
                        val barHeight = (chartHeight * (m.total.toFloat() / max)).coerceAtLeast(3f)

                        Column(
                            Modifier
                                .weight(1f)
                                .height(chartHeight.dp)
                                .clickable { onSelect(key) },
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (m.total > 0L) {
                                // 위에서부터 세차 → 정비 → 주유. 주유가 바닥에 깔린다.
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(barHeight.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                ) {
                                    listOf(
                                        m.careCost to MaterialTheme.colorScheme.tertiary,
                                        m.maintenanceCost to MaterialTheme.colorScheme.secondary,
                                        m.fuelCost to MaterialTheme.colorScheme.primary
                                    ).filter { it.first > 0L }.forEach { (value, color) ->
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .weight(value.toFloat())
                                                .background(
                                                    if (dimmed) color.copy(alpha = 0.35f) else color
                                                )
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                months.forEach { m ->
                    val selected = m.month.toString() == selectedKey
                    Text(
                        "${m.month.monthValue}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            if (avg > 0L) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "점선은 표시 기간 평균 (월 ${formatCompactWon(avg)}원)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 427,300 → "42.7만", 12,000,000 → "1,200만", 150,000,000 → "1.5억" */
internal fun formatCompactWon(value: Long): String = when {
    value >= 100_000_000L -> {
        val eok = value / 100_000_000.0
        if (eok >= 10 || eok == eok.toLong().toDouble()) "%,.0f억".format(eok) else "%.1f억".format(eok)
    }
    value >= 10_000L -> {
        val man = value / 10_000.0
        if (man >= 100 || man == man.toLong().toDouble()) "%,.0f만".format(man) else "%.1f만".format(man)
    }
    else -> "%,d".format(value)
}

/**
 * 그 달의 주유·충전 요약 — 횟수, 총량, 평균 단가, 그리고 **내 지난달 단가와 비교**.
 * 전국 평균 같은 외부 데이터 없이도 "잘 넣고 있는지"를 자기 기준으로 말해준다.
 * 플러그인 하이브리드는 주유/충전이 섞이므로 종류(unit)별로 한 줄씩.
 */
@Composable
private fun FuelSummaryCard(
    monthFuel: List<FuelRecord>,
    prevMonthFuel: List<FuelRecord>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            monthFuel.groupBy { it.unit }.entries.forEachIndexed { index, (unit, list) ->
                if (index > 0) Spacer(Modifier.height(10.dp))

                val qty = list.mapNotNull { it.quantity }.sum()
                val avgPrice = avgUnitPrice(list)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${unit.actionLabel} ${list.size}회",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        buildList {
                            if (qty > 0) add("${FuelAmountCalc.formatQuantity(qty)}${unit.symbol}")
                            avgPrice?.let { add("평균 ${it.formatWon()}원/${unit.symbol}") }
                        }.joinToString(" · ").ifBlank { "수량 기록 없음" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 지난달 내 단가와 비교 — 둘 다 계산 가능할 때만
                val prevAvg = avgUnitPrice(prevMonthFuel.filter { it.unit == unit })
                if (avgPrice != null && prevAvg != null && avgPrice != prevAvg) {
                    val diff = avgPrice - prevAvg
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (diff < 0)
                            "지난달보다 ${unit.symbol}당 ${abs(diff).formatWon()}원 싸게 넣었어요"
                        else
                            "지난달보다 ${unit.symbol}당 ${diff.formatWon()}원 비싸게 넣었어요",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (diff < 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** 평균 단가 — 수량·금액이 모두 있는 기록으로만 계산해 반쪽 기록으로 왜곡하지 않는다 */
private fun avgUnitPrice(records: List<FuelRecord>): Int? {
    val priced = records.filter { it.quantity != null && it.amount != null }
    val totalQty = priced.sumOf { it.quantity!! }
    if (totalQty <= 0) return null
    return (priced.sumOf { it.amount!!.toDouble() } / totalQty).roundToInt()
}

/** 리포트 상단 내러티브 — 이 달의 표정 */
@Composable
private fun NarrativeCard(narrative: ReportNarrative) {
    val (container, content) = when (narrative.tone) {
        NarrativeTone.WARNING ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        NarrativeTone.SPIKE, NarrativeTone.PREP ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        NarrativeTone.CALM ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NarrativeTone.EMPTY ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (narrative.tone) {
                    NarrativeTone.WARNING -> Icons.Outlined.WarningAmber
                    NarrativeTone.SPIKE -> Icons.AutoMirrored.Outlined.TrendingUp
                    NarrativeTone.PREP -> Icons.Outlined.Schedule
                    NarrativeTone.CALM -> Icons.Outlined.AutoAwesome
                    NarrativeTone.EMPTY -> Icons.Outlined.Inbox
                },
                contentDescription = null,
                tint = content
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    narrative.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    narrative.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/** 재미 지표 — 누적 지구 바퀴 + 자기 기록(또는 이 달 주행 비유) */
@Composable
private fun FunFactCard(
    earthLine: String?,
    totalMileage: Int,
    secondLine: String?
) {
    if (earthLine == null && secondLine == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            earthLine?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "누적 ${totalMileage.formatWon()}km · 지구 둘레 40,075km 기준",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (earthLine != null && secondLine != null) Spacer(Modifier.height(10.dp))
            secondLine?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private enum class EntryKind { FUEL, MAINT, CARE }

private data class MonthEntry(
    val date: String,
    val title: String,
    val amount: Int?,
    val kind: EntryKind,
    /** 주기 없는 정비 항목의 기록 = 일회성 수리 (공구 아이콘) */
    val isRepair: Boolean = false,
    /** 충전 기록 (번개 아이콘) */
    val isElectric: Boolean = false
)

/** 그 달의 주유·정비·세차 기록을 하나의 지출 내역으로 합친다(최신순) */
private fun buildMonthEntries(
    fuel: List<FuelRecord>,
    maint: List<CarMaintenanceRecord>,
    care: List<CareRecord>
): List<MonthEntry> {
    val fuelEntries = fuel.map { record ->
        MonthEntry(
            date = record.filledAt,
            title = record.unit.actionLabel +
                (record.station?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
            amount = record.amount,
            kind = EntryKind.FUEL,
            isElectric = record.unit.isElectric
        )
    }
    val maintEntries = maint.map { record ->
        MonthEntry(
            date = record.serviceDate.orEmpty(),
            title = record.typeName,
            amount = record.cost,
            kind = EntryKind.MAINT,
            isRepair = record.isRepair
        )
    }
    val careEntries = care.map { record ->
        MonthEntry(
            date = record.performedAt.orEmpty(),
            title = record.itemName +
                (record.method?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
            amount = record.cost,
            kind = EntryKind.CARE
        )
    }
    return (fuelEntries + maintEntries + careEntries).sortedByDescending { it.date }
}

@Composable
private fun ExpenseEntryRow(entry: MonthEntry, showDivider: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 앱 공통 아이콘 체계 — 아이콘이 기록의 정체, 색이 리포트 카테고리.
        Icon(
            when (entry.kind) {
                EntryKind.FUEL ->
                    if (entry.isElectric) Icons.Filled.Bolt else Icons.Filled.LocalGasStation
                EntryKind.MAINT ->
                    if (entry.isRepair) Icons.Filled.Handyman else Icons.Filled.Autorenew
                EntryKind.CARE -> Icons.Filled.WaterDrop
            },
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = when (entry.kind) {
                EntryKind.FUEL -> MaterialTheme.colorScheme.primary
                EntryKind.MAINT -> MaterialTheme.colorScheme.secondary
                EntryKind.CARE -> MaterialTheme.colorScheme.tertiary
            }
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                entry.date.toDisplayDateOrNull() ?: entry.date.ifBlank { "날짜 없음" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            entry.amount?.let { "${it.formatWon()}원" } ?: "금액 없음",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (entry.amount != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (entry.amount != null) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDivider) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 43.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

/** 항목별 연간 지출 순위 — 막대 길이로 상대 크기가 보인다 */
@Composable
private fun TopSpendCard(items: List<TopSpendItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            val max = items.maxOf { it.total }.coerceAtLeast(1L)
            items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(20.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (item.count > 1) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${item.count}회",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(fraction = (item.total.toFloat() / max).coerceIn(0.04f, 1f))
                                .height(4.dp)
                                .background(
                                    if (item.isCare) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.secondary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${item.total.formatWon()}원",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** 올해의 기록 — 데이터에서 뽑은 팩트 모음 */
@Composable
private fun YearHighlightsCard(highlights: List<YearHighlight>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            highlights.forEachIndexed { index, highlight ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        highlight.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(104.dp)
                    )
                    Text(
                        highlight.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index != highlights.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportEmptyMessage(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun Long.formatWon(): String = NumberFormat.getIntegerInstance().format(this)
private fun Int.formatWon(): String = NumberFormat.getIntegerInstance().format(this)
