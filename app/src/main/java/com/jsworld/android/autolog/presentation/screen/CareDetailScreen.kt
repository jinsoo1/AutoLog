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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
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
import com.jsworld.android.autolog.data.repository.DefaultCareItems
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
    onManageItems: (Long) -> Unit,
    viewModel: CareDetailViewModel = hiltViewModel()
) {
    val records by viewModel.careRecordsState(carId).collectAsState()
    val pickItems by viewModel.carePickItemsState(carId).collectAsState()

    var showSheet by rememberSaveable { mutableStateOf(false) }
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
                            .clickable { onManageItems(carId) }
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
                    // 주기 수정은 항목 관리 화면에서 한다(주기 설정이 거기 있다).
                    CareCycleCard(cycle = cycle, onClick = { onManageItems(carId) })
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
            // 함께 한 관리 후보 — 기본 세차만 빼고 전부(실내 세차 포함).
            // 넛지에서 온 단일 기록이면 다중 선택을 겹치지 않게 숨긴다.
            togetherCandidates = if (pendingName == null) {
                pickItems
                    .filter { it.enabled && it.name != DefaultCareItems.WASH }
                    .map { it.name }
            } else {
                emptyList()
            },
            initialName = pendingName,
            existing = null,
            onDismiss = {
                showSheet = false
                pendingName = null
            },
            onDelete = null,
            onSave = { name, date, cost, method, place, memo, together ->
                viewModel.save(carId, name, date, cost, method, place, memo, together) {
                    showSheet = false
                    pendingName = null
                    // 세차를 기록한 직후에만 — 세차 횟수 주기가 도달한 항목을 알려준다.
                    // 방금 함께 기록한 항목은 이미 했으니 넛지에서 뺀다.
                    if (isWashName(name)) {
                        nudge = careNudgeCandidates(cycles).filterNot { it.name in together }
                    }
                }
            }
        )
    }

    editTarget?.let { target ->
        CareRecordSheet(
            togetherCandidates = emptyList(), // 수정에서는 항목을 바꾸지 않는다
            initialName = target.itemName,
            existing = target,
            onDismiss = { editTarget = null },
            onDelete = { viewModel.delete(target.id) { editTarget = null } },
            onSave = { _, date, cost, method, place, memo, _ ->
                viewModel.update(target.id, date, cost, method, place, memo) {
                    editTarget = null
                }
            }
        )
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

/** 세차 항목 관리 — 켜고 끄기, 주기 칩, 직접 추가 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CareItemsSheet(
    items: List<CarePickItem>,
    onToggle: (name: String, enabled: Boolean) -> Unit,
    onAdd: (name: String) -> Unit,
    onEditInterval: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddField by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }

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
                "켠 항목은 기록할 때 고를 수 있어요. 주기 버튼을 누르면 " +
                    "'세차 3회마다'처럼 나만의 주기를 정할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            items.forEachIndexed { index, item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        // 주기는 버튼처럼 보여야 누른다 — 상태를 담은 칩으로.
                        if (item.enabled && item.itemId != null) {
                            AssistChip(
                                onClick = { onEditInterval(item.itemId) },
                                label = {
                                    Text(
                                        when {
                                            item.intervalWashCount != null ->
                                                "세차 ${item.intervalWashCount}회마다"
                                            item.intervalMonths != null ->
                                                "${item.intervalMonths}개월마다"
                                            else -> "주기 설정"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        }
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

            Spacer(Modifier.height(8.dp))

            // 직접 추가 — 하부 세차, 엔진룸 클리닝처럼 목록에 없는 항목
            if (!showAddField) {
                TextButton(
                    onClick = { showAddField = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("항목 직접 추가", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("예: 하부 세차") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        enabled = newName.trim().isNotEmpty() &&
                            items.none { it.name == newName.trim() },
                        onClick = {
                            onAdd(newName.trim())
                            newName = ""
                            showAddField = false
                        }
                    ) { Text("추가") }
                }
                if (items.any { it.name == newName.trim() }) {
                    Text(
                        "이미 있는 항목이에요 — 목록에서 켜주세요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** 세차 방식 빠른 선택 */
private val WASH_METHODS = listOf("셀프세차", "자동세차", "손세차")

/**
 * 3초 기록 시트 — 정비 기록 화면(주행거리·정비소…)은 세차엔 과하다.
 * 무엇을 + 방식 + 날짜 + 비용 + 장소 + 메모만 받는다. existing 이 있으면 수정 모드.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CareRecordSheet(
    /** 함께 한 관리를 다중 선택할 후보(기본 세차를 뺀 켜진 항목). 단일·수정 모드에선 비움 */
    togetherCandidates: List<String>,
    /** null 이면 기본 세차 기록. 넛지에서 오면 그 항목의 단일 기록 */
    initialName: String?,
    existing: CareRecord?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (
        name: String, date: String, cost: Int?,
        method: String?, place: String?, memo: String?,
        together: List<String>
    ) -> Unit
) {
    // "무엇을" 선택은 없다 — 이 시트는 기본이 세차 기록이다.
    val selectedName = existing?.itemName ?: initialName ?: DefaultCareItems.WASH
    var together by rememberSaveable { mutableStateOf(setOf<String>()) }
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
                    when {
                        existing != null -> "${existing.itemName} 기록 수정"
                        selectedName == DefaultCareItems.WASH -> "세차 기록"
                        else -> "$selectedName 기록"
                    },
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

                // 세차하면서 같이 한 관리 — 다중 선택하면 각각 기록이 남는다.
                if (togetherCandidates.isNotEmpty()) {
                    Text(
                        "선택 항목 (함께 했다면 골라주세요 — 각각 기록돼요)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        togetherCandidates.forEach { name ->
                            FilterChip(
                                selected = name in together,
                                onClick = {
                                    together =
                                        if (name in together) together - name
                                        else together + name
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                    if (together.isNotEmpty()) {
                        Text(
                            "비용은 세차 기록에 입력돼요 — 항목별 비용은 저장 후 각 기록에서 수정할 수 있어요.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
                        memo,
                        if (isWashName(selectedName)) together.toList() else emptyList()
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
