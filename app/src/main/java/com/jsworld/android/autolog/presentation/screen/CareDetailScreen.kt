package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.CARE_MONTH_OPTIONS
import com.jsworld.android.autolog.domain.model.CARE_WASH_COUNT_OPTIONS
import com.jsworld.android.autolog.domain.model.CareCycleProgress
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.model.CareCycleUnit
import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.domain.model.buildCareCycles
import com.jsworld.android.autolog.domain.model.buildCareOverview
import com.jsworld.android.autolog.domain.model.careNudgeCandidates
import com.jsworld.android.autolog.domain.model.careCounts
import com.jsworld.android.autolog.domain.model.isWashName
import com.jsworld.android.autolog.domain.model.upkeepLines
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.presentation.viewModel.CareDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 세차·관리 허브 — 정비(마감 중심)와 달리 "마지막으로 한 지 얼마나 됐나"가 축이다.
 * 세차·코팅·왁스 등 관리 계열 기록을 한 화면에서 보고, 3초 시트로 빠르게 기록한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareDetailScreen(
    carId: Long,
    onBack: () -> Unit,
    viewModel: CareDetailViewModel = hiltViewModel()
) {
    val records by viewModel.careRecordsState(carId).collectAsState()
    val itemNames by viewModel.careNamesState(carId).collectAsState()
    val pickItems by viewModel.carePickItemsState(carId).collectAsState()

    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showItemSheet by rememberSaveable { mutableStateOf(false) }
    var intervalTarget by rememberSaveable { mutableStateOf<Long>(-1L) }
    // 저장 직후 "이번엔 왁스도 할 때예요" 안내 — 푸시가 아니라 화면 안 배너
    var nudge by remember { mutableStateOf<List<CareCycleProgress>>(emptyList()) }
    // 넛지에서 '왁스도 기록'을 누르면 그 항목이 미리 선택된 시트가 열린다
    var pendingName by remember { mutableStateOf<String?>(null) }
    // 기록 행을 누르면 그 기록을 수정하는 시트가 열린다
    var editTarget by remember { mutableStateOf<CareRecord?>(null) }

    val today = remember { LocalDate.now() }
    val overview = remember(records) { buildCareOverview(records, today) }
    val counts = remember(records) { careCounts(records, today) }

    val cycles = remember(pickItems, records) {
        buildCareCycles(
            items = pickItems,
            washDates = records.filter { isWashName(it.itemName) }.mapNotNull { it.performedAt },
            lastByName = records
                .filter { it.performedAt != null }
                .groupBy { it.itemName }
                .mapValues { (_, list) -> list.mapNotNull { it.performedAt }.max() },
            today = today
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("세차·관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("세차 기록") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CareHeroCard(overview) }

            if (nudge.isNotEmpty()) {
                item {
                    CareNudgeCard(
                        cycles = nudge,
                        onRecord = { name ->
                            nudge = emptyList()
                            pendingName = name
                            showSheet = true
                        },
                        onDismiss = { nudge = emptyList() }
                    )
                }
            }

            if (records.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CareStatCard("이번 달", "${counts.monthCount}회", Modifier.weight(1f))
                        CareStatCard("올해", "${counts.yearCount}회", Modifier.weight(1f))
                        CareStatCard(
                            "올해 비용",
                            "${formatCompactWon(counts.yearCost)}원",
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "관리 주기",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "항목 관리",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showItemSheet = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            if (cycles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "주기를 정한 관리 항목이 없어요",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "'항목 관리'에서 왁스·코팅 같은 항목을 켜고 " +
                                    "'세차 3회마다'처럼 나만의 주기를 정할 수 있어요.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(items = cycles, key = { it.itemId }) { cycle ->
                    CareCycleCard(cycle = cycle, onClick = { intervalTarget = cycle.itemId })
                }
            }

            if (records.isNotEmpty()) {
                item { SectionLabel("기록 · ${records.size}건") }
                item {
                    ListCard {
                        Column {
                            records.forEachIndexed { index, record ->
                                CareRecordRow(
                                    record = record,
                                    onClick = { editTarget = record },
                                    showDivider = index != records.lastIndex
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        CareRecordSheet(
            itemNames = itemNames,
            initialName = pendingName,
            existing = null,
            onDismiss = {
                showSheet = false
                pendingName = null
            },
            onDelete = null,
            onSave = { name, date, cost, method, place, memo ->
                viewModel.save(carId, name, date, cost, method, place, memo) {
                    showSheet = false
                    pendingName = null
                    // 세차를 기록한 직후에만 — 세차 횟수 주기가 도달한 항목을 알려준다.
                    if (isWashName(name)) nudge = careNudgeCandidates(cycles)
                }
            }
        )
    }

    editTarget?.let { target ->
        CareRecordSheet(
            itemNames = listOf(target.itemName), // 수정에서는 항목을 바꾸지 않는다
            initialName = target.itemName,
            existing = target,
            onDismiss = { editTarget = null },
            onDelete = { viewModel.delete(target.id) { editTarget = null } },
            onSave = { _, date, cost, method, place, memo ->
                viewModel.update(target.id, date, cost, method, place, memo) {
                    editTarget = null
                }
            }
        )
    }

    if (showItemSheet) {
        CareItemsSheet(
            items = pickItems,
            onToggle = { name, enabled -> viewModel.setItemEnabled(carId, name, enabled) },
            onEditInterval = { settingId ->
                showItemSheet = false
                intervalTarget = settingId
            },
            onDismiss = { showItemSheet = false }
        )
    }

    intervalTarget.takeIf { it > 0L }?.let { itemId ->
        val item = pickItems.firstOrNull { it.itemId == itemId }
        if (item == null) {
            intervalTarget = -1L
        } else {
            CareIntervalSheet(
                item = item,
                onSave = { months, washCount ->
                    viewModel.setInterval(itemId, months, washCount)
                    intervalTarget = -1L
                },
                onDismiss = { intervalTarget = -1L }
            )
        }
    }
}

/* ───────────────────────── 구성 요소 ───────────────────────── */

@Composable
private fun CareHeroCard(overview: com.jsworld.android.autolog.domain.model.CareOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val content = MaterialTheme.colorScheme.onTertiaryContainer

            if (overview.daysSinceWash == null) {
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "아직 세차 기록이 없어요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "아래 버튼으로 첫 세차를 기록해보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.8f)
                )
            } else {
                Text("마지막 세차", style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.8f))
                Text(
                    if (overview.daysSinceWash == 0) "오늘" else "${overview.daysSinceWash}일 전",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                overview.lastWash?.let { last ->
                    Text(
                        buildList {
                            last.performedAt?.toDisplayDateOrNull()?.let { add(it) }
                            last.method?.takeIf { it.isNotBlank() }?.let { add(it) }
                            last.cost?.let { add("${it.formatThousands()}원") }
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                overview.averageIntervalDays?.let { avg ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (overview.isDue) "평균 ${avg}일마다 세차했어요 — 슬슬 때가 됐네요"
                        else "평균 ${avg}일마다 세차하고 있어요",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = content
                    )
                }
            }
        }
    }
}

@Composable
private fun CareStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CareRecordRow(
    record: CareRecord,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.performedAt?.toDisplayDateOrNull() ?: "날짜 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // 세차가 아닌 항목(코팅·왁스)은 이름을 함께 보여 구분한다.
                if (!isWashName(record.itemName)) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        record.itemName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            val subtitle = buildList {
                record.method?.takeIf { it.isNotBlank() }?.let { add(it) }
                record.place?.takeIf { it.isNotBlank() }?.let { add(it) }
                record.memo?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        record.cost?.let {
            Text(
                "${it.formatThousands()}원",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 43.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

/** 관리 주기 한 줄 — 진행 막대는 경고가 아니라 리듬을 보여준다(초과도 호박색) */
@Composable
private fun CareCycleCard(cycle: CareCycleProgress, onClick: () -> Unit) {
    val accent =
        if (cycle.isOverdue) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cycle.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    cycle.remainText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { cycle.progress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(6.dp))
            Text(
                cycle.caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 세차 저장 직후 안내 — 세차장에 서 있는 그 순간에 "왁스도 할 때"를 알려준다.
 * 푸시 알림으로 귀찮게 하지 않는 게 핵심이다.
 */
@Composable
private fun CareNudgeCard(
    cycles: List<CareCycleProgress>,
    onRecord: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            val content = MaterialTheme.colorScheme.onTertiaryContainer
            val first = cycles.first()
            Text(
                if (cycles.size == 1) "이번엔 ${first.name}도 할 때예요"
                else "이번엔 ${first.name} 등 ${cycles.size}가지도 할 때예요",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
            Spacer(Modifier.height(2.dp))
            Text(
                cycles.joinToString(" · ") { "${it.name} ${it.remainText}" },
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cycles.take(2).forEach { cycle ->
                    FilledTonalButton(
                        onClick = { onRecord(cycle.name) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("${cycle.name} 기록", style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("다음에", style = MaterialTheme.typography.labelMedium, color = content)
                }
            }
        }
    }
}

/** 세차 항목 관리 — 켜고 끄기 + 주기 설정 진입 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CareItemsSheet(
    items: List<CarePickItem>,
    onToggle: (name: String, enabled: Boolean) -> Unit,
    onEditInterval: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text("관리 항목", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "켠 항목은 기록할 때 고를 수 있어요. 주기를 정하면 '세차 3회마다'처럼 진행도가 보입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            items.forEachIndexed { index, item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        val cycleText = when {
                            item.intervalWashCount != null -> "세차 ${item.intervalWashCount}회마다"
                            item.intervalMonths != null -> "${item.intervalMonths}개월마다"
                            item.enabled -> "주기 없음 · 기록만"
                            else -> null
                        }
                        cycleText?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (item.enabled && item.itemId != null) {
                        TextButton(onClick = { onEditInterval(item.itemId) }) { Text("주기") }
                    }
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = { onToggle(item.name, it) }
                    )
                }
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

/** 주기 설정 — 세차 횟수 / 기간 / 없음 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CareIntervalSheet(
    item: CarePickItem,
    onSave: (months: Int?, washCount: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    // 초기 단위 — 저장된 값이 있으면 그 단위, 없으면 세차 횟수를 권한다.
    var unit by rememberSaveable(item.name) {
        mutableStateOf(
            when {
                item.intervalWashCount != null -> CareCycleUnit.WASH_COUNT
                item.intervalMonths != null -> CareCycleUnit.MONTHS
                else -> CareCycleUnit.WASH_COUNT
            }
        )
    }
    var washCount by rememberSaveable(item.name) { mutableStateOf(item.intervalWashCount ?: 3) }
    var months by rememberSaveable(item.name) { mutableStateOf(item.intervalMonths ?: 6) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                "${item.name} 주기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = unit == CareCycleUnit.WASH_COUNT,
                    onClick = { unit = CareCycleUnit.WASH_COUNT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("세차 횟수") }
                SegmentedButton(
                    selected = unit == CareCycleUnit.MONTHS,
                    onClick = { unit = CareCycleUnit.MONTHS },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("기간") }
                SegmentedButton(
                    selected = unit == CareCycleUnit.NONE,
                    onClick = { unit = CareCycleUnit.NONE },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("없음") }
            }
            Spacer(Modifier.height(12.dp))

            when (unit) {
                CareCycleUnit.WASH_COUNT -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CARE_WASH_COUNT_OPTIONS.forEach { n ->
                            FilterChip(
                                selected = washCount == n,
                                onClick = { washCount = n },
                                label = { Text("${n}회") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "세차 ${washCount}회마다 ${item.name} — 세차 기록이 쌓이면 자동으로 세어드려요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CareCycleUnit.MONTHS -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CARE_MONTH_OPTIONS.forEach { m ->
                            FilterChip(
                                selected = months == m,
                                onClick = { months = m },
                                label = { Text("${m}개월") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "마지막 ${item.name} 기록에서 ${months}개월이 기준이 돼요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CareCycleUnit.NONE -> {
                    Text(
                        "주기 없이 기록만 남겨요. 진행도는 표시되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    when (unit) {
                        CareCycleUnit.WASH_COUNT -> onSave(null, washCount)
                        CareCycleUnit.MONTHS -> onSave(months, null)
                        CareCycleUnit.NONE -> onSave(null, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 세차 방식 빠른 선택 — 메모에 그대로 들어가는 프리셋 */
private val WASH_METHODS = listOf("셀프세차", "자동세차", "손세차", "실내 클리닝")

/**
 * 3초 기록 시트 — 정비 기록 화면(주행거리·정비소…)은 세차엔 과하다.
 * 무엇을 + 방식 + 날짜 + 비용 + 장소 + 메모만 받는다. existing 이 있으면 수정 모드.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CareRecordSheet(
    itemNames: List<String>,
    initialName: String?,
    existing: CareRecord?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (name: String, date: String, cost: Int?, method: String?, place: String?, memo: String?) -> Unit
) {
    var selectedName by rememberSaveable {
        mutableStateOf(initialName ?: itemNames.firstOrNull() ?: "세차")
    }
    var date by rememberSaveable {
        mutableStateOf(existing?.performedAt ?: LocalDate.now().toString())
    }
    var costText by rememberSaveable { mutableStateOf(existing?.cost?.toString() ?: "") }
    var method by rememberSaveable { mutableStateOf(existing?.method ?: "") }
    var place by rememberSaveable { mutableStateOf(existing?.place ?: "") }
    var memo by rememberSaveable { mutableStateOf(existing?.memo ?: "") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (existing == null) "세차 기록"
                    else "${existing.itemName} 기록 수정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (existing == null && itemNames.size > 1) {
                Text(
                    "무엇을 했나요?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    itemNames.forEach { name ->
                        FilterChip(
                            selected = selectedName == name,
                            onClick = { selectedName = name },
                            label = { Text(name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // 방식 — 전용 필드. 칩으로 고르거나, 그 외 방식은 메모에 적으면 된다.
            if (isWashName(selectedName)) {
                Text(
                    "방식 (선택)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    WASH_METHODS.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = if (method == m) "" else m },
                            label = { Text(m) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = date.toDisplayDateOrNull() ?: date,
                onValueChange = {},
                readOnly = true,
                label = { Text("날짜") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("변경") }
                }
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = costText,
                onValueChange = { input -> costText = input.filter { it.isDigit() }.take(9) },
                label = { Text("비용 (원)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorTransformation,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text("장소 (선택)") },
                placeholder = { Text("예: OO셀프세차장") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (선택)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    onSave(
                        selectedName,
                        date,
                        costText.toIntOrNull(),
                        method,
                        place,
                        memo
                    )
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saving) "저장하는 중…" else "저장", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            }.getOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
