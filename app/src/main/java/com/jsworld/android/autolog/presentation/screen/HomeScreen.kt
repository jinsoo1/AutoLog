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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.SCHEDULE_HOME_DAYS
import com.jsworld.android.autolog.domain.model.Season
import com.jsworld.android.autolog.domain.model.SeasonalCareGuide
import com.jsworld.android.autolog.domain.model.SeasonalCareRow
import com.jsworld.android.autolog.domain.model.SeasonalRowState
import com.jsworld.android.autolog.domain.model.buildSeasonalCareRows
import com.jsworld.android.autolog.domain.model.dDayLabel
import com.jsworld.android.autolog.domain.model.formatScheduleDate
import com.jsworld.android.autolog.domain.model.isSeasonalCardVisible
import com.jsworld.android.autolog.domain.model.lastCareLabel
import com.jsworld.android.autolog.domain.model.parseSnoozeDate
import com.jsworld.android.autolog.domain.model.seasonKey
import com.jsworld.android.autolog.domain.model.seasonOf
import com.jsworld.android.autolog.domain.model.seasonalDoneCount
import com.jsworld.android.autolog.domain.model.seasonalGuide
import com.jsworld.android.autolog.domain.model.seasonalSnoozeDate
import com.jsworld.android.autolog.domain.model.upcomingSchedules
import com.jsworld.android.autolog.presentation.component.CarSwitcherChip
import com.jsworld.android.autolog.presentation.component.TabContentTopPadding
import com.jsworld.android.autolog.presentation.component.StatCard
import com.jsworld.android.autolog.presentation.component.TabTopBar
import com.jsworld.android.autolog.presentation.model.FuelAmountCalc
import com.jsworld.android.autolog.presentation.viewModel.HomeViewModel
import kotlinx.coroutines.launch
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
    /** 계절 카드에서 아직 켜지 않은 항목을 눌렀을 때 — 항목 추가 화면으로 */
    onAddMaintenanceItem: (Long) -> Unit,
    /** 정기검사·보험 만기 등 날짜 일정 화면 */
    onOpenSchedule: (Long) -> Unit,
    onAddMaintenance: (carId: Long, settingId: Long?) -> Unit,
    onOpenItemDetail: (Long) -> Unit,
    onSeeAllRecords: () -> Unit,
    onSeeAllFuel: () -> Unit,
    onOpenReport: () -> Unit,
    /** '올해는 넘어가기'의 실행 취소를 띄울 자리 */
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {

        // 상단 바 — 차량 전환 / 차량 정보 수정 / 공지
        TabTopBar(
            leading = { CarSwitcherChip(car = car, onClick = onSwitchCar) },
            actions = {
                if (car != null) {
                    IconButton(onClick = { onEditCar(car.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "차량 정보 수정")
                    }
                }
                IconButton(onClick = onNoticeClick) {
                    Icon(Icons.Default.Campaign, contentDescription = "공지사항")
                }
            }
        )

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

        // 날짜 일정 — 놓치면 과태료인 것들이라 설정 탭에 묻어두지 않는다.
        // 다만 **임박했을 때만** 꺼낸다. 늘 떠 있으면 배경이 되고, 배경은 안 보인다.
        val schedules by viewModel.schedulesState(car.id).collectAsState()

        // 계절별 관리 — 주행거리로 안 잡히는 것들(배터리는 추워지면, 와이퍼는 장마 전에).
        // 기록이 없어도 카드를 숨기지 않는다. "무엇을 봐야 하나"가 이 카드의 값이고,
        // 각 줄의 버튼이 곧 첫 기록을 남기는 입구가 된다.
        val today = LocalDate.now()
        val seasonalKey = remember(today.monthValue) { seasonKey(today) }
        val dismissedSeasonKey by viewModel.seasonalCareDismissedKeyState(car.id).collectAsState()
        val snoozeUntilRaw by viewModel.seasonalCareSnoozeUntilState(car.id).collectAsState()
        val snoozeUntil = remember(snoozeUntilRaw) { parseSnoozeDate(snoozeUntilRaw) }
        var showSeasonalSkipDialog by rememberSaveable(car.id) { mutableStateOf(false) }
        val seasonalGuide = remember(today.monthValue) { seasonalGuide(today) }
        val dueSchedules = remember(schedules, today) {
            upcomingSchedules(schedules, today, SCHEDULE_HOME_DAYS)
        }
        val seasonalRows = remember(seasonalGuide, overview, records, today) {
            val lastDates = records
                .mapNotNull { rec ->
                    rec.serviceDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?.let { rec.settingId to it }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, dates) -> dates.max() }
            buildSeasonalCareRows(seasonalGuide, overview, lastDates, today)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = TabContentTopPadding, bottom = 24.dp
            ),
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

            if (dueSchedules.isNotEmpty()) {
                item {
                    UpcomingScheduleCard(
                        schedules = dueSchedules,
                        today = today,
                        onClick = { onOpenSchedule(car.id) }
                    )
                }
            }

            // 임박·초과 카드 아래에 둔다. 계절 카드는 읽는 콘텐츠라,
            // 지금 당장 해야 할 항목보다 위에 오면 급한 것을 밀어낸다.
            if (isSeasonalCardVisible(today, seasonalKey, dismissedSeasonKey, snoozeUntil)) {
                item {
                    SeasonalCareCard(
                        guide = seasonalGuide,
                        rows = seasonalRows,
                        today = today,
                        onRecord = { settingId -> onAddMaintenance(car.id, settingId) },
                        onAddItem = { onAddMaintenanceItem(car.id) },
                        onSkip = { showSeasonalSkipDialog = true }
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

        if (showSeasonalSkipDialog) {
            SeasonalSkipDialog(
                season = seasonalGuide.season,
                today = today,
                onDismiss = { showSeasonalSkipDialog = false },
                onSnooze = { until ->
                    showSeasonalSkipDialog = false
                    viewModel.snoozeSeasonalCare(car.id, until)
                    scope.launch { showSeasonalUndo(snackbarHostState, viewModel, car.id, "${until.monthValue}월 ${until.dayOfMonth}일에 다시 보여드려요") }
                },
                onSkipYear = {
                    showSeasonalSkipDialog = false
                    viewModel.dismissSeasonalCare(car.id, seasonalKey)
                    scope.launch { showSeasonalUndo(snackbarHostState, viewModel, car.id, "내년 ${seasonalGuide.season.label()}에 다시 보여드려요") }
                }
            )
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

/**
 * 다가오는 날짜 일정 — 정기검사·보험 만기·자동차세.
 *
 * 주행거리와 무관하게 날짜로만 오는 것들이라 정비 상태 어디에도 안 잡힌다.
 * 놓치면 과태료로 이어지므로, 지났으면 임박 카드와 같은 빨강을 쓴다.
 */
@Composable
private fun UpcomingScheduleCard(
    schedules: List<CarSchedule>,
    today: LocalDate,
    onClick: () -> Unit
) {
    val overdue = schedules.any { (it.remainingDays(today) ?: 0L) < 0L }
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
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent, shape = CircleShape) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(15.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Text(
                    "다가오는 일정",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent
                )
            }

            schedules.forEach { schedule ->
                val remaining = schedule.remainingDays(today) ?: return@forEach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            schedule.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatScheduleDate(schedule.dueDate, today),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        dDayLabel(remaining),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining < 0L) MaterialTheme.colorScheme.error else accent
                    )
                }
            }
        }
    }
}

/**
 * 계절별 관리 카드 — "이번 겨울 전에 확인할 3가지".
 *
 * 임박 카드(빨강)와 달리 **재촉하지 않는다**. 계절이 바뀔 때 한 번 읽고 넘기는
 * 콘텐츠라 포인트색(tertiary)을 옅게 깔고, 각 줄에 다음 행동만 붙인다.
 */
@Composable
private fun SeasonalCareCard(
    guide: SeasonalCareGuide,
    rows: List<SeasonalCareRow>,
    today: LocalDate,
    onRecord: (Long) -> Unit,
    onAddItem: () -> Unit,
    onSkip: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    // 세 항목 모두 기록이 없으면 안내 문구를 바꾼다 — 빈 화면을 사과하는 대신 다음 행동을 준다.
    val hasAnyRecord = rows.any { it.lastServiceDate != null }
    val doneCount = seasonalDoneCount(rows)
    val allDone = doneCount == rows.size && rows.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.09f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent.copy(alpha = 0.18f), shape = CircleShape) {
                    Icon(
                        guide.season.icon(),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        guide.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        // 이번 계절에 이미 처리한 줄이 있으면 그걸 먼저 말한다 —
                        // 다 해둔 사람에게 "확인하세요"만 반복하면 카드가 잔소리가 된다.
                        when {
                            allDone -> "이번 ${guide.season.label()} ${rows.size}가지를 다 기록했어요"
                            doneCount > 0 -> "${rows.size}가지 중 ${doneCount}가지 기록했어요"
                            hasAnyRecord -> guide.subtitle
                            else -> "지금 확인하고 기록해두면, 다음부터 알려드릴 수 있어요"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allDone) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (allDone) FontWeight.Bold else FontWeight.Normal
                    )
                }
                // 접기는 X 로. "올해는 넘어가기"는 한 번에 3개월을 없애는 무거운 선택인데
                // 문구만 보고는 그 무게를 알 수 없어서, 눌렀을 때 무엇을 고르는지 묻는다.
                IconButton(onClick = onSkip, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "이 안내 접기",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    SeasonalCareRowItem(
                        row = row,
                        today = today,
                        onClick = {
                            if (row.settingId != null) onRecord(row.settingId) else onAddItem()
                        }
                    )
                }
            }

        }
    }
}

/**
 * 접기 선택 — **한 달만** 미룰지, **이번 계절은 끝**낼지.
 *
 * 한 달씩 미루다 계절 창을 넘어가면 그때는 다음 계절 안내가 뜨고, 이번 계절 내용은
 * 저절로 내년으로 밀린다. 그래서 "다음 달" 이 실제로 어느 계절이 되는지도 밝힌다.
 */
@Composable
private fun SeasonalSkipDialog(
    season: Season,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSnooze: (LocalDate) -> Unit,
    onSkipYear: () -> Unit
) {
    val snoozeDate = remember(today) { seasonalSnoozeDate(today) }
    val snoozeSeason = remember(snoozeDate) { seasonOf(snoozeDate.monthValue) }
    val accent = MaterialTheme.colorScheme.tertiary

    AlertDialog(
        onDismissRequest = onDismiss,
        // 카드와 같은 계절 아이콘·포인트색을 쓴다 — 어느 카드를 접는지가 한눈에 보인다.
        icon = {
            Surface(color = accent.copy(alpha = 0.16f), shape = CircleShape) {
                Icon(
                    season.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
        },
        title = {
            Text(
                "이번 ${season.label()} 안내를 접을까요?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkipOptionRow(
                    icon = Icons.Default.Snooze,
                    title = "다음 달에 다시",
                    // 미룬 날짜가 다른 계절이면 그때는 그 계절 안내가 뜬다 — 미리 밝힌다.
                    subtitle = buildString {
                        append("${snoozeDate.monthValue}월 ${snoozeDate.dayOfMonth}일에 다시 보여드려요")
                        if (snoozeSeason != season) {
                            append("\n그때는 ${snoozeSeason.label()} 안내로 바뀌어요")
                        }
                    },
                    emphasized = true,
                    onClick = { onSnooze(snoozeDate) }
                )
                SkipOptionRow(
                    icon = Icons.Default.EventBusy,
                    title = "내년에 다시",
                    subtitle = "이번 ${season.label()}은 넘기고\n내년 같은 때에 알려드려요",
                    emphasized = false,
                    onClick = onSkipYear
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * 선택지 한 줄. 권하는 쪽(다음 달)만 포인트색 테두리를 주고 나머지는 조용히 둔다 —
 * 둘 다 강조하면 어느 쪽이 가벼운 선택인지 알 수 없다.
 */
@Composable
private fun SkipOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (emphasized) accent.copy(alpha = 0.07f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (emphasized) 1.5.dp else 1.dp,
            color = if (emphasized) accent.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (emphasized) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (emphasized) accent else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.labelSmall.fontSize * 1.45
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (emphasized) accent
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/** 접기 직후 안내 + 실행 취소. '내년에 다시'는 3개월을 없애는 선택이라 되돌릴 길을 남긴다 */
private suspend fun showSeasonalUndo(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel,
    carId: Long,
    message: String
) {
    val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = "실행 취소",
        duration = SnackbarDuration.Short
    )
    if (result == SnackbarResult.ActionPerformed) viewModel.undoSeasonalCareSkip(carId)
}

@Composable
private fun SeasonalCareRowItem(
    row: SeasonalCareRow,
    today: LocalDate,
    onClick: () -> Unit
) {
    val done = row.state == SeasonalRowState.DONE
    val overdue = row.status == MaintenanceStatus.OVERDUE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // 끝난 줄은 누를 이유가 없다 — 기록 화면으로 또 보내면 방금 한 일을 다시 시킨다.
            .then(if (done) Modifier else Modifier.clickable(onClick = onClick)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (done) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    row.itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    row.detailText(today),
                    style = MaterialTheme.typography.labelSmall,
                    // 주기를 넘긴 항목은 색으로도 구분한다 — 계절 안내와 별개로 급한 일이다.
                    color = if (overdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            when (row.state) {
                SeasonalRowState.DONE -> Text(
                    // '확인함'은 앱이 점검했다는 뜻으로 읽힌다. 앱이 아는 건 기록뿐이다.
                    "기록함",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                else -> {
                    val accent =
                        if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    Surface(shape = CircleShape, color = accent.copy(alpha = 0.10f)) {
                        Text(
                            if (row.state == SeasonalRowState.NOT_MANAGED) "추가" else "기록",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

/**
 * 줄의 부제. **상태에 따라 할 말이 다르다** —
 * 이번 계절에 이미 남긴 기록이 있으면 그 사실을 알리고, 주기를 넘겼으면 그게 먼저다.
 *
 * ⚠️ 앱이 아는 건 **기록뿐**이다. 8월 7일에 냉각수를 갈았다는 기록이 있어도 지금
 * 새고 있는지, 정비가 제대로 됐는지는 알 수 없다. 그래서 "안 해도 돼요" 같은
 * **판정 문장을 쓰지 않는다** — 기록이 있다는 사실만 말하고 판단은 사용자에게 남긴다.
 */
private fun SeasonalCareRow.detailText(today: LocalDate): String = when (state) {
    SeasonalRowState.NOT_MANAGED -> "$reason · 관리 목록에 없어요"
    SeasonalRowState.DONE -> {
        val date = lastServiceDate?.let { "${it.monthValue}월 ${it.dayOfMonth}일에" } ?: "이번 계절에"
        "$reason · $date 기록했어요"
    }
    SeasonalRowState.TODO -> when (status) {
        MaintenanceStatus.OVERDUE -> "$reason · 교체 시기가 지났어요"
        MaintenanceStatus.SOON -> "$reason · 교체 시기가 다가왔어요"
        else -> "$reason · ${lastCareLabel(lastServiceDate, today)}"
    }
}

/** 스낵바 문구용 — "이번 여름은 넘길게요" */
private fun Season.label(): String = when (this) {
    Season.SPRING -> "봄"
    Season.MONSOON -> "장마"
    Season.SUMMER -> "여름"
    Season.PRE_WINTER, Season.WINTER -> "겨울"
}

private fun Season.icon(): ImageVector = when (this) {
    Season.SPRING -> Icons.Default.LocalFlorist
    Season.MONSOON -> Icons.Default.Umbrella
    Season.SUMMER -> Icons.Default.WbSunny
    Season.PRE_WINTER, Season.WINTER -> Icons.Default.AcUnit
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
