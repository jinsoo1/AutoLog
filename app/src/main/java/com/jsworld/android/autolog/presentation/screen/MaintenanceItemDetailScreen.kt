package com.jsworld.android.autolog.presentation.screen

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.isCareItemName
import com.jsworld.android.autolog.presentation.state.MaintenanceItemDetailUiState
import com.jsworld.android.autolog.presentation.viewModel.MaintenanceItemDetailViewModel

/**
 * 정비 항목 상세 — 주기·현재 상태·교체 내역을 한 화면에 모았다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceItemDetailScreen(
    settingId: Long,
    viewModel: MaintenanceItemDetailViewModel,
    onBack: () -> Unit,
    onEditIntervals: () -> Unit,
    onAddRecord: (carId: Long) -> Unit,
    onEditHistory: (Long) -> Unit
) {
    // observeUi 는 호출마다 새 Flow 를 만든다. remember 없이 쓰면 리컴포지션마다 재구독되고,
    // onStart 의 loading=true 가 다시 흘러들어와 무한 리컴포지션이 된다.
    val uiFlow = remember(settingId) { viewModel.observeUi(settingId) }
    val ui by uiFlow.collectAsState(initial = MaintenanceItemDetailUiState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            ui.typeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            ui.intervalLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = onEditIntervals) {
                        Icon(Icons.Default.Edit, contentDescription = "교체 주기 수정")
                    }
                }
            )
        },
        bottomBar = {
            if (!ui.loading && ui.carId > 0L) {
                Surface {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAddRecord(ui.carId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("이 항목 기록 추가", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->

        if (ui.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.isRepair) {
                item { RepairInfoCard(isCare = isCareItemName(ui.typeName)) }
                item { PromoteToIntervalCard(onClick = onEditIntervals) }
            } else {
                item { StatusCard(ui) }
                // 주기는 상단바 부제에만 있으면 찾기 어렵다.
                // 어디를 눌러야 바꿀 수 있는지 화면 본문에서 직접 보여준다.
                item { IntervalRow(ui, onClick = onEditIntervals) }
            }

            item {
                SectionLabel(
                    title = when {
                        ui.isRepair && isCareItemName(ui.typeName) -> "관리 내역"
                        ui.isRepair -> "수리 내역"
                        else -> "교체 내역"
                    },
                    actionLabel = if (ui.histories.isNotEmpty()) "${ui.histories.size}건" else null
                )
            }

            if (ui.histories.isEmpty()) {
                item {
                    ListCard {
                        Text(
                            when {
                                ui.isRepair && isCareItemName(ui.typeName) -> "아직 관리 기록이 없어요."
                                ui.isRepair -> "아직 수리 기록이 없어요."
                                else -> "아직 교체 기록이 없어요."
                            },
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    ListCard {
                        ui.histories.forEachIndexed { index, history ->
                            HistoryRow(
                                history = history,
                                showDivider = index != ui.histories.lastIndex,
                                onClick = { onEditHistory(history.id) }
                            )
                        }
                    }
                }
            }

            val averages = ui.averageLabel()
            if (averages != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            averages,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 교체 주기를 본문에 명시적으로 보여주는 행. 탭하면 주기 수정으로 간다.
 * (상단바 연필 아이콘만으로는 주기를 어디서 바꾸는지 찾기 어렵다)
 */
@Composable
private fun IntervalRow(ui: MaintenanceItemDetailUiState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Autorenew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "교체 주기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                val parts = buildList {
                    ui.intervalKm?.let { add("${it.formatThousands()}km") }
                    ui.intervalMonths?.let { add("${it}개월") }
                }
                Text(
                    parts.joinToString(" · ") +
                        if (ui.usingDefaultIntervals) " (기본값)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "수정",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** 주기 없는 항목의 머리 카드 — 수리 또는 관리(세차·코팅류). "상태 좋아요"로 오해되지 않게 상태 카드를 대신한다. */
@Composable
private fun RepairInfoCard(isCare: Boolean) {
    val accent = if (isCare) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    val containerTint =
        if (isCare) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val onContainer =
        if (isCare) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerTint.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = accent, shape = CircleShape) {
                Icon(
                    if (isCare) Icons.Default.WaterDrop else Icons.Default.Handyman,
                    contentDescription = null,
                    tint = if (isCare) MaterialTheme.colorScheme.onTertiary
                    else MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(15.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    if (isCare) "세차·관리 기록" else "일회성 수리",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainer
                )
                Text(
                    "임박 알림·다음 정비에 나타나지 않아요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 수리 → 주기 관리 승격 안내. 냉각수처럼 알고 보니 반복 항목인 경우를 위해서다. */
@Composable
private fun PromoteToIntervalCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Autorenew,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Text(
                buildString {
                    append("반복해서 갈게 되는 부품이라면 주기를 설정해 관리 항목으로 바꿀 수 있어요.")
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "주기 설정",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusCard(ui: MaintenanceItemDetailUiState) {
    val normal = ui.status == MaintenanceStatus.NORMAL
    val accent = when (ui.status) {
        MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.error
        MaintenanceStatus.SOON -> MaterialTheme.colorScheme.tertiary
        MaintenanceStatus.NORMAL -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent, shape = CircleShape) {
                    Icon(
                        if (normal) Icons.Default.CheckCircle else Icons.Default.PriorityHigh,
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
                        when (ui.status) {
                            MaintenanceStatus.OVERDUE -> "교체 초과"
                            MaintenanceStatus.SOON -> "교체 임박"
                            MaintenanceStatus.NORMAL -> "상태 좋아요"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    if (ui.remainingText.isNotBlank()) {
                        Text(
                            ui.remainingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (ui.progressRatio != null) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { ui.progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f),
                    drawStopIndicator = {}
                )
            }

            val mileageLine = ui.mileageLabel()
            if (mileageLine != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    mileageLine,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    history: MaintenanceHistory,
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
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Icon(
                    Icons.Default.Autorenew,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(15.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    history.serviceDate ?: "날짜 미상",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                val sub = buildList {
                    history.serviceMileage?.let { add("${it.formatThousands()}km") }
                    history.place?.takeIf { it.isNotBlank() }?.let { add(it) }
                    history.memo?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (history.cost != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    history.cost.formatThousands(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (showDivider) RowDivider()
    }
}

private fun MaintenanceItemDetailUiState.intervalLabel(): String {
    val parts = buildList {
        intervalKm?.let { add("${it.formatThousands()}km") }
        intervalMonths?.let { add("${it}개월") }
    }
    if (parts.isEmpty()) {
        return if (isCareItemName(typeName)) "관리 기록 · 주기 없음" else "일회성 수리 · 주기 없음"
    }
    val suffix = if (usingDefaultIntervals) " 주기(기본값)" else " 주기"
    return parts.joinToString(" · ") + suffix
}

private fun MaintenanceItemDetailUiState.mileageLabel(): String? {
    val last = lastServiceMileage ?: return null
    val next = nextDueMileage
    return if (next != null) {
        "${last.formatThousands()}km 교체 · 다음 ${next.formatThousands()}km"
    } else {
        "${last.formatThousands()}km 교체"
    }
}

private fun MaintenanceItemDetailUiState.averageLabel(): String? {
    val parts = buildList {
        averageIntervalKm?.let { add("평균 ${it.formatThousands()}km마다 교체") }
        averageCost?.let { add("평균 ${it.formatThousands()}원") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
