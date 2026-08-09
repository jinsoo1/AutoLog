package com.jsworld.android.autolog.presentation.screen

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.presentation.component.CarSwitcherChip
import com.jsworld.android.autolog.presentation.viewModel.ReportViewModel
import java.text.NumberFormat
import java.time.YearMonth

/**
 * 지출 리포트 탭 — 월간/연간 총지출, 카테고리 분해, km당 유지비, 월별 추이.
 *
 * 원칙: 계산할 수 없는 값은 "-" 로 두고 이유를 밝힌다. 숫자를 지어내지 않는다.
 */
@Composable
fun ReportTabScreen(
    car: Car?,
    onSwitchCar: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    Column(Modifier.fillMaxSize()) {
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
                item {
                    TotalCard(
                        title = "${current.month.monthValue}월 총지출",
                        fuel = current.fuelCost,
                        maintenance = current.maintenanceCost,
                        care = current.careCost,
                        missingCostCount = current.missingCostCount
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
    missingCostCount: Int
) {
    val total = fuel + maintenance + care
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${total.formatWon()}원",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (total > 0L) {
                Spacer(Modifier.height(12.dp))
                CategoryBar(fuel = fuel, maintenance = maintenance, care = care)
                Spacer(Modifier.height(10.dp))
                CategoryLegendRow("주유·충전", fuel, MaterialTheme.colorScheme.primary)
                CategoryLegendRow("정비·수리", maintenance, MaterialTheme.colorScheme.secondary)
                CategoryLegendRow("세차·관리", care, MaterialTheme.colorScheme.tertiary)
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "이 기간엔 지출 기록이 없어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
private fun CategoryBar(fuel: Long, maintenance: Long, care: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val parts = listOf(
            fuel to MaterialTheme.colorScheme.primary,
            maintenance to MaterialTheme.colorScheme.secondary,
            care to MaterialTheme.colorScheme.tertiary
        ).filter { it.first > 0L }

        parts.forEachIndexed { index, (value, color) ->
            val shape = when {
                parts.size == 1 -> RoundedCornerShape(5.dp)
                index == 0 -> RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
                index == parts.lastIndex -> RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                Modifier
                    .weight(value.toFloat())
                    .height(10.dp)
                    .background(color, shape)
            )
            if (index != parts.lastIndex) Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
private fun CategoryLegendRow(label: String, value: Long, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
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
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value > 0L) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${value.formatWon()}원",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (value > 0L) FontWeight.SemiBold else FontWeight.Normal,
            color = if (value > 0L) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
 * 월별 지출 막대 — 막대를 누르면 그 달로 이동한다.
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
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEach { m ->
                val key = m.month.toString()
                val selected = key == selectedKey
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(key) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fraction = (m.total.toFloat() / max).coerceIn(0.04f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((72 * fraction).dp)
                            .background(
                                when {
                                    selected -> MaterialTheme.colorScheme.primary
                                    m.total > 0L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${m.month.monthValue}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
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
