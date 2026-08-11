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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.isCareItemName
import com.jsworld.android.autolog.presentation.component.CarSwitcherChip
import com.jsworld.android.autolog.presentation.viewModel.MaintenanceTabViewModel
import java.time.LocalDate

/**
 * 정비 탭 — 항목 구분 없이 이 차량의 모든 정비 기록을 월별 타임라인으로 보여준다.
 *
 * 이전에는 기록을 보려면 차량 상세 → 항목 → 항목 수정 → 내역 보기로 네 번 들어가야 했다.
 * 이 화면이 그 경로를 대신한다.
 */
@Composable
fun MaintenanceTabScreen(
    car: Car?,
    onSwitchCar: () -> Unit,
    onManageItems: (Long) -> Unit,
    onAddMaintenance: (carId: Long, settingId: Long?) -> Unit,
    onEditHistory: (Long) -> Unit,
    viewModel: MaintenanceTabViewModel = hiltViewModel()
) {
    Scaffold(
        floatingActionButton = {
            if (car != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddMaintenance(car.id, null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("정비 기록") }
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
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarSwitcherChip(car = car, onClick = onSwitchCar)
                Spacer(Modifier.weight(1f))
                if (car != null) {
                    // 아이콘만 있으면 무슨 버튼인지 알 수 없다. 라벨을 함께 보여준다.
                    Row(
                        modifier = Modifier
                            .clickable { onManageItems(car.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "항목 관리",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (car == null) {
                EmptyMessage("차량을 먼저 추가해주세요", "위 차량 칩에서 차량을 추가할 수 있어요.")
                return@Column
            }

            val records by viewModel.recordsState(car.id).collectAsState()

            var filter by rememberSaveable(car.id) { mutableStateOf<String?>(null) }

            // 수리·관리는 건마다 이름이 달라 칩이 폭발하므로 각각 하나로 묶는다.
            // 관리 = 주기 없는 항목 중 세차·코팅류(수리가 아니므로 배지도 다르다).
            val typeNames = remember(records) {
                records.filterNot { it.isRepair || isCareItemName(it.typeName) }
                    .map { it.typeName }.distinct()
            }
            val hasRepairs = remember(records) {
                records.any { it.isRepair && !isCareItemName(it.typeName) }
            }
            val hasCare = remember(records) {
                records.any { isCareItemName(it.typeName) }
            }

            // 필터로 고른 항목이 기록에서 사라지면(삭제 등) 필터를 해제한다.
            val activeFilter = filter?.takeIf {
                it in typeNames ||
                    (it == REPAIR_FILTER && hasRepairs) ||
                    (it == CARE_FILTER && hasCare)
            }
            val shown = remember(records, activeFilter) {
                when (activeFilter) {
                    null -> records
                    REPAIR_FILTER -> records.filter { it.isRepair && !isCareItemName(it.typeName) }
                    CARE_FILTER -> records.filter { isCareItemName(it.typeName) }
                    else -> records.filter { it.typeName == activeFilter && !it.isRepair }
                }
            }

            if (records.isEmpty()) {
                EmptyMessage(
                    "아직 정비 기록이 없어요",
                    "오른쪽 아래 '정비 기록'으로 첫 기록을 남겨보세요."
                )
                return@Column
            }

            // 주기는 있는데 기록이 없는 항목 안내 — 이런 항목은 계산 기준이 없어
            // 홈·리포트·알림의 임박/초과에서 빠지므로, 여기서 조용히 알려준다.
            val noHistoryCount by viewModel.noHistoryCountState(car.id).collectAsState()
            if (noHistoryCount > 0) {
                NoHistoryHintBanner(
                    count = noHistoryCount,
                    onClick = { onAddMaintenance(car.id, null) }
                )
                Spacer(Modifier.height(10.dp))
            }

            if (typeNames.size > 1 || ((hasRepairs || hasCare) && typeNames.isNotEmpty())) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    item {
                        FilterChip(
                            selected = activeFilter == null,
                            onClick = { filter = null },
                            label = { Text("전체") }
                        )
                    }
                    if (hasRepairs) {
                        item {
                            FilterChip(
                                selected = activeFilter == REPAIR_FILTER,
                                onClick = {
                                    filter = if (activeFilter == REPAIR_FILTER) null else REPAIR_FILTER
                                },
                                label = { Text("수리") }
                            )
                        }
                    }
                    if (hasCare) {
                        item {
                            FilterChip(
                                selected = activeFilter == CARE_FILTER,
                                onClick = {
                                    filter = if (activeFilter == CARE_FILTER) null else CARE_FILTER
                                },
                                label = { Text("관리") }
                            )
                        }
                    }
                    items(items = typeNames, key = { it }) { name ->
                        FilterChip(
                            selected = activeFilter == name,
                            onClick = { filter = if (activeFilter == name) null else name },
                            label = { Text(name, maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // 월별로 묶는다. 기록은 이미 최신순이므로 순서를 유지하는 그룹핑이면 된다.
            val grouped = remember(shown) { shown.groupBy { it.monthLabel() } }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (month, monthRecords) ->
                    item(key = "month-$month") {
                        Text(
                            month,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item(key = "records-$month") {
                        ListCard {
                            monthRecords.forEachIndexed { index, record ->
                                TimelineRow(
                                    record = record,
                                    showDivider = index != monthRecords.lastIndex,
                                    onClick = { onEditHistory(record.historyId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    record: CarMaintenanceRecord,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    // 아이콘은 항목의 정체를, 배지는 기록의 성격을 말한다.
    // 세차류는 주기를 붙여도(승격) 물방울을 유지한다 — 세차가 사이클 아이콘이 되면 어색하다.
    val isCare = isCareItemName(record.typeName)
    val badgeLabel = when {
        isCare -> "관리"
        record.isRepair -> "수리"
        else -> null
    }
    val container = when {
        isCare -> MaterialTheme.colorScheme.tertiaryContainer
        record.isRepair -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        isCare -> MaterialTheme.colorScheme.onTertiaryContainer
        record.isRepair -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val icon = when {
        isCare -> Icons.Default.WaterDrop
        record.isRepair -> Icons.Default.Handyman
        else -> Icons.Default.Autorenew
    }

    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = container, shape = CircleShape) {
                Icon(
                    icon,
                    contentDescription = badgeLabel,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(15.dp),
                    tint = content
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.typeName,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (badgeLabel != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = container, shape = CircleShape) {
                            Text(
                                badgeLabel,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = content
                            )
                        }
                    }
                }
                Text(
                    record.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (record.cost != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    record.cost.formatThousands(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (showDivider) RowDivider()
    }
}

@Composable
private fun EmptyMessage(title: String, body: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                Icons.Default.Build,
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

/**
 * 기록 없는 항목 안내 배너 — 경고가 아니라 안내이므로 빨간색이 아닌 차분한 톤.
 * 누르면 기록 추가 흐름(항목 선택 시트)으로 이어진다.
 */
@Composable
private fun NoHistoryHintBanner(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        // 반투명 색을 그대로 쓰면 톤과 섞여 딤처럼 보인다 — compositeOver 로 불투명하게.
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            .compositeOver(MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "아직 기록이 없는 항목이 ${count}개 있어요",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "첫 기록을 남기면 교체 시기를 계산해 알려드려요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/** 필터 칩에서 "수리/관리 전체"를 뜻하는 값. 항목 이름과 겹치지 않게 제어문자를 쓴다. */
private const val REPAIR_FILTER = "\u0000repair"
private const val CARE_FILTER = "\u0000care"

/** "2026년 7월". 날짜가 없는 기록은 맨 아래 "날짜 미상"으로 모은다. */
private fun CarMaintenanceRecord.monthLabel(): String {
    val date = serviceDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return "날짜 미상"
    return "${date.year}년 ${date.monthValue}월"
}
