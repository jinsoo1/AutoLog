package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.CARE_WASH_COUNT_OPTIONS
import com.jsworld.android.autolog.domain.model.CareCycleProgress
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.model.CareCycleUnit
import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.domain.model.CareSession
import com.jsworld.android.autolog.domain.model.buildCareCycles
import com.jsworld.android.autolog.domain.model.buildCareSessions
import com.jsworld.android.autolog.domain.model.buildCareOverview
import com.jsworld.android.autolog.domain.model.careNudgeCandidates
import com.jsworld.android.autolog.domain.model.careCounts
import com.jsworld.android.autolog.domain.model.BASE_WASH_NAME
import com.jsworld.android.autolog.domain.model.upkeepLines
import com.jsworld.android.autolog.data.repository.DefaultCareItems
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.presentation.viewModel.CareDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 접기 기준. "외 1개"가 생기는 게 더 어색해서, 접었을 때 최소 2개는 숨겨질 때부터 접는다.
 */
private const val CYCLE_COLLAPSED_COUNT = 3
private const val CYCLE_COLLAPSE_MIN = 4
private const val RECORD_COLLAPSED_COUNT = 10
private const val RECORD_COLLAPSE_MIN = 11

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
    // 여러 항목이 든 묶음은 먼저 "무엇을 했는지" 시트를 연다.
    // 묶음 자체가 아니라 key 를 들고 있어야 안에서 수정·삭제해도 내용이 갱신된다.
    var openSessionKey by rememberSaveable { mutableStateOf<String?>(null) }
    // 항목·기록이 쌓여도 첫 화면 길이는 그대로 — 필요할 때만 펼친다
    var cyclesExpanded by rememberSaveable { mutableStateOf(false) }
    var recordsExpanded by rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val overview = remember(records) { buildCareOverview(records, today) }
    val counts = remember(records) { careCounts(records, today) }
    val sessions = remember(records) { buildCareSessions(records) }

    val cycles = remember(pickItems, records) {
        buildCareCycles(
            items = pickItems,
            washDates = records.filter { it.itemName == BASE_WASH_NAME }
                .mapNotNull { it.performedAt },
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
            // 통계는 별도 카드가 아니라 히어로 안에 — 카드가 줄면 화면이 조용해진다
            item {
                CareHeroCard(
                    overview = overview,
                    counts = counts,
                    showStats = records.isNotEmpty()
                )
            }

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

            item {
                SectionLabel(
                    title = "관리 주기",
                    actionLabel = "항목 관리",
                    onAction = { onManageItems(carId) }
                )
            }

            item {
                if (cycles.isEmpty()) {
                    CareEmptyCard(
                        title = "주기를 정한 관리 항목이 없어요",
                        body = "'항목 관리'에서 왁스·코팅 같은 항목을 켜고 " +
                            "'세차 3회마다'처럼 나만의 주기를 정할 수 있어요."
                    )
                } else {
                    // 항목마다 카드를 띄우면 화면이 시끄럽다 — 한 카드 안에 줄로 모은다.
                    // 항목이 많아지면 기록이 저 아래로 밀리므로 임박한 3개만 남기고 접는다.
                    // 주기 수정은 항목 관리 화면에서 한다(주기 설정이 거기 있다).
                    val collapsible = cycles.size > CYCLE_COLLAPSE_MIN
                    val visible =
                        if (!collapsible || cyclesExpanded) cycles
                        else cycles.take(CYCLE_COLLAPSED_COUNT)

                    ListCard {
                        Column {
                            visible.forEachIndexed { index, cycle ->
                                CareCycleRow(
                                    cycle = cycle,
                                    onClick = { onManageItems(carId) },
                                    showDivider = index != visible.lastIndex || collapsible
                                )
                            }
                            if (collapsible) {
                                CareExpandRow(
                                    expanded = cyclesExpanded,
                                    collapsedLabel = "외 ${cycles.size - CYCLE_COLLAPSED_COUNT}개 모두 보기",
                                    onClick = { cyclesExpanded = !cyclesExpanded }
                                )
                            }
                        }
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item { SectionLabel("기록 · ${sessions.size}건") }

                // 오래된 기록까지 한 번에 쏟아내지 않는다 — 최근 것부터, 달로 끊어서.
                val collapsible = sessions.size > RECORD_COLLAPSE_MIN
                val visible =
                    if (!collapsible || recordsExpanded) sessions
                    else sessions.take(RECORD_COLLAPSED_COUNT)

                visible.groupBy { it.performedAt?.take(7) ?: "" }.forEach { (monthKey, group) ->
                    item(key = "care-month-$monthKey") {
                        Column {
                            CareMonthHeader(monthKey = monthKey, sessions = group, today = today)
                            Spacer(Modifier.height(6.dp))
                            ListCard {
                                Column {
                                    group.forEachIndexed { index, session ->
                                        CareSessionRow(
                                            session = session,
                                            onClick = {
                                                // 한 항목뿐이면 한 단계 건너뛰고 바로 수정으로
                                                if (session.records.size == 1) {
                                                    editTarget = session.primary
                                                } else {
                                                    openSessionKey = session.key
                                                }
                                            },
                                            showDivider = index != group.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (collapsible) {
                    item {
                        ListCard {
                            CareExpandRow(
                                expanded = recordsExpanded,
                                collapsedLabel = "지난 기록 ${sessions.size - RECORD_COLLAPSED_COUNT}건 더 보기",
                                onClick = { recordsExpanded = !recordsExpanded }
                            )
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
                    .filter { it.enabled && it.name != BASE_WASH_NAME }
                    .map { it.name }
            } else {
                emptyList()
            },
            // '매 세차마다'로 정한 항목(내부세차 등)은 매번 체크하게 두지 않는다 —
            // 미리 골라두고, 안 한 날만 빼면 된다.
            defaultTogether = if (pendingName == null) {
                pickItems
                    .filter { it.enabled && it.name != BASE_WASH_NAME && it.intervalWashCount == 1 }
                    .map { it.name }
                    .toSet()
            } else {
                emptySet()
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
                    if (name == BASE_WASH_NAME) {
                        nudge = careNudgeCandidates(cycles).filterNot { it.name in together }
                    }
                }
            }
        )
    }

    // 묶음 상세 — 그날 무엇을 했는지 펼쳐 보고, 항목을 누르면 그 기록을 수정한다
    sessions.firstOrNull { it.key == openSessionKey }?.let { session ->
        CareSessionSheet(
            session = session,
            onEditRecord = { record ->
                // 시트를 겹쳐 띄우지 않는다 — 묶음 시트를 닫고 수정 시트로 넘긴다
                openSessionKey = null
                editTarget = record
            },
            onDismiss = { openSessionKey = null }
        )
    }

    editTarget?.let { target ->
        CareRecordSheet(
            togetherCandidates = emptyList(), // 수정에서는 항목을 바꾸지 않는다
            defaultTogether = emptySet(),
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

/**
 * 히어로 — "세차한 지 며칠"이 이 화면의 한 문장이다.
 * 물방울 워터마크와 옅은 그라데이션으로 깊이만 주고, 숫자는 그대로 크게 둔다.
 */
@Composable
private fun CareHeroCard(
    overview: com.jsworld.android.autolog.domain.model.CareOverview,
    counts: com.jsworld.android.autolog.domain.model.CareCounts,
    showStats: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onTertiaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(scheme.tertiaryContainer, scheme.primaryContainer)
                    )
                )
        ) {
            // 워터마크 — 카드가 잘라주므로 밖으로 나간 부분은 자연스럽게 잘린다
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = content.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 26.dp)
                    .size(132.dp)
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (overview.daysSinceWash == null) {
                    Icon(
                        Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "아직 세차 기록이 없어요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "아래 버튼으로 첫 세차를 기록해보세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        "마지막 세차",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = content.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(2.dp))
                    // 숫자만 크게, 단위는 작게 — 한 덩어리로 읽히게 아래 정렬
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (overview.daysSinceWash == 0) "오늘" else "${overview.daysSinceWash}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = content
                        )
                        if (overview.daysSinceWash != 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "일 전",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = content.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
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
                        Spacer(Modifier.height(10.dp))
                        // 평균 리듬은 한 줄 배지로 — 배경이 있으면 워터마크 위에서도 읽힌다
                        Surface(
                            color = content.copy(alpha = 0.12f),
                            contentColor = content,
                            shape = CircleShape
                        ) {
                            Text(
                                if (overview.isDue) "평균 ${avg}일마다 · 슬슬 때가 됐어요"
                                else "평균 ${avg}일마다 세차하고 있어요",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (showStats) {
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = content.copy(alpha = 0.15f))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        HeroStat("이번 달", "${counts.monthCount}회", content, Modifier.weight(1f))
                        HeroStatDivider(content)
                        HeroStat("올해", "${counts.yearCount}회", content, Modifier.weight(1f))
                        HeroStatDivider(content)
                        HeroStat(
                            "올해 비용",
                            "${formatCompactWon(counts.yearCost)}원",
                            content,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    value: String,
    content: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun HeroStatDivider(content: Color) {
    VerticalDivider(
        modifier = Modifier.height(26.dp),
        color = content.copy(alpha = 0.15f)
    )
}

/** 비어 있는 섹션 안내 — 목록 카드와 같은 테두리 톤을 쓴다 */
@Composable
private fun CareEmptyCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                body,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 기록 한 줄 = 하루치 묶음 한 건.
 * 세차하면서 왁스·실내를 함께 했어도 목록에서는 1건이고, 눌러야 안이 펼쳐진다.
 */
@Composable
private fun CareSessionRow(
    session: CareSession,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val multi = session.records.size > 1

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CareLeadingIcon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    session.performedAt?.toDisplayDateOrNull() ?: "날짜 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // 세차가 아닌 항목만 있는 날은 이름을, 여러 항목이면 개수를 보여준다.
                val badge = when {
                    multi -> "항목 ${session.records.size}개"
                    session.primary.itemName != BASE_WASH_NAME -> session.primary.itemName
                    else -> null
                }
                badge?.let {
                    Spacer(Modifier.width(6.dp))
                    CareBadge(it)
                }
            }
            val subtitle = buildList {
                if (multi) add(session.itemNames.joinToString(" · "))
                session.primary.method?.takeIf { it.isNotBlank() }?.let { add(it) }
                session.primary.place?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (!multi) session.primary.memo?.takeIf { it.isNotBlank() }?.let { add(it) }
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
        session.totalCost?.let {
            Text(
                "${it.formatThousands()}원",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 44.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

/**
 * 기록 목록의 달 구분 — "8월 · 3건 · 32,000원".
 * 달이 바뀌는 지점을 눈으로 잡아주면 기록이 쌓여도 훑기 쉽다.
 */
@Composable
private fun CareMonthHeader(monthKey: String, sessions: List<CareSession>, today: LocalDate) {
    val title = remember(monthKey, today) {
        val month = runCatching { YearMonth.parse(monthKey) }.getOrNull()
        when {
            month == null -> "날짜 없음"
            month.year == today.year -> "${month.monthValue}월"
            else -> "${month.year}년 ${month.monthValue}월"
        }
    }
    val cost = sessions.sumOf { (it.totalCost ?: 0).toLong() }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            buildList {
                add("${sessions.size}건")
                if (cost > 0) add("${formatCompactWon(cost)}원")
            }.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 접기·펼치기 줄 — 리포트 지출 내역과 같은 모양을 쓴다 */
@Composable
private fun CareExpandRow(expanded: Boolean, collapsedLabel: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (expanded) "접기" else collapsedLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/** 물방울 아이콘 — 옅은 원 안에 넣으면 목록이 정돈돼 보인다 */
@Composable
private fun CareLeadingIcon(size: Dp = 32.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}

/** 날짜 옆 작은 표식 — '항목 3개' / '코팅' */
@Composable
private fun CareBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * 묶음 상세 — "그날 뭐 했더라"에 답하는 시트.
 * 항목을 누르면 그 항목만 수정한다(비용도 항목별로 나눠 적을 수 있다).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CareSessionSheet(
    session: CareSession,
    onEditRecord: (CareRecord) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        session.performedAt?.toDisplayDateOrNull() ?: "날짜 없음",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "이날 한 관리 ${session.records.size}가지",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                session.totalCost?.let {
                    Text(
                        "${it.formatThousands()}원",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            session.records.forEachIndexed { index, record ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEditRecord(record) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CareLeadingIcon(size = 28.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            record.itemName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val detail = buildList {
                            record.method?.takeIf { it.isNotBlank() }?.let { add(it) }
                            record.place?.takeIf { it.isNotBlank() }?.let { add(it) }
                            record.memo?.takeIf { it.isNotBlank() }?.let { add(it) }
                        }.joinToString(" · ")
                        if (detail.isNotBlank()) {
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        // 비용을 안 적은 항목은 "-" — 0원이라고 지어내지 않는다
                        record.cost?.let { "${it.formatThousands()}원" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.cost == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "수정",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index != session.records.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "항목을 누르면 그 항목만 수정·삭제할 수 있어요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 관리 주기 한 줄 — 진행 막대는 경고가 아니라 리듬을 보여준다(초과도 호박색) */
@Composable
private fun CareCycleRow(
    cycle: CareCycleProgress,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val accent =
        if (cycle.isOverdue) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primary

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                cycle.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 남은 양은 알약으로 — 초과한 줄이 목록에서 바로 눈에 든다
            Surface(color = accent.copy(alpha = 0.12f), shape = CircleShape) {
                Text(
                    cycle.remainText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { cycle.progress ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = accent,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            drawStopIndicator = {},
            gapSize = 0.dp
        )
        Spacer(Modifier.height(7.dp))
        Text(
            cycle.caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
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
    /** 처음부터 선택돼 있을 항목 — '매 세차마다'로 정한 것들 */
    defaultTogether: Set<String>,
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
    var together by rememberSaveable { mutableStateOf(defaultTogether) }
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
            if (selectedName == BASE_WASH_NAME) {
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
                        "선택 항목 (함께 했다면 골라주세요 — 기록은 한 건으로 묶여요)",
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
                    if (defaultTogether.isNotEmpty()) {
                        Text(
                            "'매 세차마다'로 정한 항목은 미리 골라뒀어요 — 안 한 날은 눌러서 빼주세요.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (together.isNotEmpty()) {
                        Text(
                            "비용은 세차에 들어가요 — 저장 후 기록을 열면 항목별로 나눠 적을 수 있어요.",
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
                        if (selectedName == BASE_WASH_NAME) together.toList() else emptyList()
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
        // 아직 하지 않은 세차를 미리 기록할 일은 없다 — 오늘 이후는 고를 수 없게 한다.
        val todayEndUtc = remember {
            LocalDate.now().plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            }.getOrNull(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis < todayEndUtc
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            }
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
