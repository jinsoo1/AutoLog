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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.buildCareOverview
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
    onEditHistory: (Long) -> Unit,
    viewModel: CareDetailViewModel = hiltViewModel()
) {
    val records by viewModel.careRecordsState(carId).collectAsState()
    val itemNames by viewModel.careNamesState(carId).collectAsState()

    var showSheet by rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val overview = remember(records) { buildCareOverview(records, today) }
    val counts = remember(records) { careCounts(records, today) }
    val upkeep = remember(records) { upkeepLines(records, today) }

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

            if (upkeep.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "유지 관리",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                upkeep.joinToString(" · ") { (name, days) ->
                                    if (days == 0) "$name 오늘" else "$name ${days}일 전"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
                                    onClick = { onEditHistory(record.historyId) },
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
            onDismiss = { showSheet = false },
            onSave = { name, date, cost, place, memo ->
                viewModel.save(carId, name, date, cost, place, memo) { showSheet = false }
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
                            last.serviceDate?.toDisplayDateOrNull()?.let { add(it) }
                            last.memo?.takeIf { it.isNotBlank() }?.let { add(it) }
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
    record: CarMaintenanceRecord,
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
                    record.serviceDate?.toDisplayDateOrNull() ?: "날짜 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // 세차가 아닌 항목(코팅·왁스)은 이름을 함께 보여 구분한다.
                if (!isWashName(record.typeName)) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        record.typeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            val subtitle = buildList {
                record.memo?.takeIf { it.isNotBlank() }?.let { add(it) }
                record.place?.takeIf { it.isNotBlank() }?.let { add(it) }
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

/** 세차 방식 빠른 선택 — 메모에 그대로 들어가는 프리셋 */
private val WASH_METHODS = listOf("셀프세차", "자동세차", "손세차", "실내 클리닝")

/**
 * 3초 기록 시트 — 정비 기록 화면(주행거리·정비소…)은 세차엔 과하다.
 * 무엇을 + 날짜 + 비용 + 장소 + 메모만 받는다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CareRecordSheet(
    itemNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, date: String, cost: Int?, place: String?, memo: String?) -> Unit
) {
    var selectedName by rememberSaveable { mutableStateOf(itemNames.firstOrNull() ?: "세차") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var costText by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text("세차 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            if (itemNames.size > 1) {
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

            // 방식 프리셋 — 세차일 때만. 누르면 메모에 들어가고, 자유롭게 수정 가능.
            if (isWashName(selectedName)) {
                Text(
                    "방식 (선택 — 메모에 들어가요)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    WASH_METHODS.forEach { method ->
                        FilterChip(
                            selected = memo.startsWith(method),
                            onClick = {
                                memo = if (memo.startsWith(method)) {
                                    memo.removePrefix(method).trimStart(' ', '·').trim()
                                } else {
                                    val rest = WASH_METHODS.fold(memo) { acc, m ->
                                        acc.removePrefix(m).trimStart(' ', '·').trim()
                                    }
                                    if (rest.isBlank()) method else "$method · $rest"
                                }
                            },
                            label = { Text(method) }
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
