package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.CarMaintenanceSetting
import com.jsworld.android.autolog.domain.model.MaintenanceSort
import com.jsworld.android.autolog.presentation.theme.StatusNormal
import com.jsworld.android.autolog.presentation.theme.StatusOverdue
import com.jsworld.android.autolog.presentation.theme.StatusSoon
import com.jsworld.android.autolog.presentation.viewModel.CarDetailViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 정비 항목 관리 — 이 차량에 켜둔 항목과 각 항목의 주기를 다룬다.
 *
 * 차량 요약·정비 상태는 홈 탭, 기록 열람은 정비 탭이 담당하므로 여기서는 항목만 다룬다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(
    carId: Long,
    viewModel: CarDetailViewModel,
    onBack: () -> Unit,
    onAddMaintenanceItem: (Long) -> Unit,
    onOpenItemDetail: (Long) -> Unit, // settingId
) {
    val listState = rememberLazyListState()

    val car by viewModel.carState(carId).collectAsState()
    val sort by viewModel.sortState(carId).collectAsState()
    val settings by viewModel.sortedSettingsState(carId).collectAsState()
    val types by viewModel.maintenanceTypesState().collectAsState()

    val typeDefaultMap = remember(types) { types.associateBy({ it.id }, { it }) }

    val options by viewModel.settingOptionsState(carId).collectAsState()
    val optionMap = remember(options) { options.associateBy { it.settingId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "정비 항목",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            car?.name ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->

        if (car == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }


        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                MaintenanceSettingsHeader(
                    currentSort = sort,
                    onSortSelected = { selectedSort: MaintenanceSort ->
                        viewModel.setSort(carId, selectedSort)
                    },
                    onPickItems = { onAddMaintenanceItem(carId) }
                )
            }

            items(items = settings, key = { it.id }) { setting ->
                val opt = optionMap[setting.id]
                val type = typeDefaultMap[setting.maintenanceTypeId]
                val typeName = type?.name ?: "정비항목"

                MaintenanceSettingItem(
                    setting = setting,
                    typeName = typeName,
                    defaultKm = type?.defaultIntervalKm,
                    defaultMonths = type?.defaultIntervalMonths,
                    carMileage = car!!.mileage,
                    lastServiceDate = opt?.lastServiceDate,
                    lastServiceMileage = opt?.lastServiceMileage,
                    onClick = { onOpenItemDetail(setting.id) }
                )
            }
        }
    }
}




@Composable
fun MaintenanceSettingsHeader(
    currentSort: MaintenanceSort,
    onSortSelected: (MaintenanceSort) -> Unit,
    onPickItems: () -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 제목
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "정비 주기 설정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 정렬 Chip (드롭다운)
            Box {
                AssistChip(
                    onClick = { sortMenuExpanded = true },
                    label = { Text(currentSort.label) },
                    leadingIcon = {
                        Icon(Icons.Default.Sort, contentDescription = null)
                    }
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    MaintenanceSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            leadingIcon = {
                                if (option == currentSort) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            onClick = {
                                onSortSelected(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // 항목 선택 버튼
            FilledTonalButton(
                onClick = onPickItems,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("항목")
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "정비 내역이 없는 항목은 정렬되지 않아요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }
}


private const val SOON_RATIO = 0.15f

@Composable
fun MaintenanceSettingItem(
    setting: CarMaintenanceSetting,
    typeName: String,
    defaultKm: Int?,
    defaultMonths: Int?,
    carMileage: Int,
    lastServiceDate: String?,
    lastServiceMileage: Int?,
    onClick: () -> Unit
) {
    val intervalKm = setting.intervalKm ?: defaultKm
    val intervalMonths = setting.intervalMonths ?: defaultMonths

    val lastDate = lastServiceDate?.toLocalDateOrNull()
    val today = LocalDate.now()

    // 내역 없을 때 기준값
    val hasAnyHistory = (lastDate != null) || (lastServiceMileage != null)

    // km 기준: 내역 없으면 0km 기준
    val baseLastMileage = lastServiceMileage ?: 0

    // date 기준: 내역 없으면 "오늘" 기준
    val baseDateForCalc = lastDate ?: today

    // --------------------------
    // 예정/잔여 계산
    // --------------------------

    // km 예정/잔여
    val dueMileage = if (intervalKm != null) baseLastMileage + intervalKm else null
    val remainingKm = dueMileage?.let { it - carMileage }

    // 날짜 예정/잔여
    val dueDate =
        if (intervalMonths != null) baseDateForCalc.plusMonths(intervalMonths.toLong())
        else null

    val remainingDays = dueDate?.let { ChronoUnit.DAYS.between(today, it) }

    val kmText = intervalKm?.let { "${it.formatKm()} km" } ?: "-"
    val monthText = intervalMonths?.let { "${it} 개월" } ?: "-"

    // --------------------------
    // 상태 판단 (초과 / 도래(임박) / 정상)
    // --------------------------

    val isOverdue =
        (remainingKm != null && remainingKm < 0) ||
                (remainingDays != null && remainingDays < 0)

    // 도래(임박) 기준: 주기 대비 15% 이하(0 포함)
    val soonKmThreshold: Int? = intervalKm?.let { interval ->
        max(1, (interval * SOON_RATIO).roundToInt())
    }

    // 날짜쪽은 "전체 주기 일수" 대비 15% 이하
    val totalDaysOfCycle: Long? = if (intervalMonths != null && dueDate != null) {
        ChronoUnit.DAYS.between(baseDateForCalc, dueDate).coerceAtLeast(1)
    } else null

    val soonDaysThreshold: Long? = totalDaysOfCycle?.let { total ->
        max(1L, ceil(total * SOON_RATIO).toLong())
    }

    val isDue = !isOverdue && (
            (remainingKm != null && soonKmThreshold != null && remainingKm in 0..soonKmThreshold) ||
                    (remainingDays != null && soonDaysThreshold != null && remainingDays in 0..soonDaysThreshold)
            )

    val statusLabel = when {
        isOverdue -> "초과"
        isDue -> "도래" // "임박"도 추천
        else -> "정상"
    }

    // 상태색(고정)
    val statusColor = when {
        isOverdue -> StatusOverdue
        isDue -> StatusSoon
        else -> StatusNormal
    }

    val statusContainer = statusColor.copy(alpha = 0.14f)
    val accent = statusColor
    val isDanger = isOverdue || isDue

    // --------------------------
    // 표시 텍스트
    // --------------------------

    val lastInfoValue = if (!hasAnyHistory) {
        "없음 (0km / 오늘 기준)"
    } else {
        val d = lastDate?.toString() ?: "없음"
        val m = lastServiceMileage?.let { "${it.formatKm()} km" } ?: "없음"
        "$d · $m"
    }

    val remainKmText = remainingKm?.let {
        when {
            it < 0 -> "초과 ${abs(it).formatKm()} km"
            it == 0 -> "0 km(도래)"
            else -> "${it.formatKm()} km 남음"
        }
    } ?: "-"

    val remainDayText = remainingDays?.let {
        when {
            it < 0 -> "초과 ${abs(it)}일"
            it == 0L -> "0일(오늘)"
            else -> "${it}일 남음"
        }
    } ?: "-"

    val dueMileageText = dueMileage?.let { "${it.formatKm()} km" } ?: "-"
    val dueDateText = dueDate?.toString() ?: "-"

    // --------------------------
    // Progress 계산 (여기만 추가!)
    // --------------------------

    val kmProgress: Float? = if (intervalKm != null && intervalKm > 0 && dueMileage != null) {
        // 사용량(0 이상)
        val used = (carMileage - baseLastMileage).coerceAtLeast(0)
        val p = used.toFloat() / intervalKm.toFloat()
        if (isOverdue) 1f else p.coerceIn(0f, 1f)
    } else null

    val dayProgress: Float? = if (totalDaysOfCycle != null && dueDate != null) {
        val usedDays = ChronoUnit.DAYS.between(baseDateForCalc, today).coerceAtLeast(0)
        val p = usedDays.toFloat() / totalDaysOfCycle.toFloat()
        if (isOverdue) 1f else p.coerceIn(0f, 1f)
    } else null

    // --------------------------
    // UI
    // --------------------------

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            typeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "주기: $kmText / $monthText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 상태 배지: 위험(초과/도래)은 솔리드, 정상은 틴트
                    Surface(
                        color = if (isDanger) statusColor else statusContainer,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDanger) Color.White else statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))

                InfoLine(
                    icon = Icons.Default.History,
                    title = "마지막 정비",
                    value = lastInfoValue
                )

                // 진행바가 하나도 없을 때만 텍스트로 잔여 표시(진행바와 중복 방지)
                if (kmProgress == null && dayProgress == null) {
                    Spacer(Modifier.height(8.dp))
                    InfoLine(
                        icon = Icons.Default.Route,
                        title = "잔여",
                        value = "$remainKmText / $remainDayText",
                        valueColor = accent,
                        boldValue = isDanger
                    )
                }

                // ProgressBar(거리/기간) 추가
                if (kmProgress != null) {
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(
                        label = "거리",
                        valueText = remainKmText,
                        progress = kmProgress,
                        color = statusColor
                    )
                }
                if (dayProgress != null) {
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(
                        label = "기간",
                        valueText = remainDayText,
                        progress = dayProgress,
                        color = statusColor
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (dueMileage != null || dueDate != null) {
                    InfoLine(
                        icon = Icons.Default.Event,
                        title = "다음 예정",
                        value = "$dueMileageText · $dueDateText",
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}


@Composable
private fun InfoLine(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    boldValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                value,
                color = valueColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (boldValue) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ProgressLine(
    label: String,
    valueText: String,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)
private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()