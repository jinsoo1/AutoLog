package com.jsworld.android.autolog.ui.view.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.CarMaintenanceSetting
import com.jsworld.android.autolog.ui.data.item.MaintenanceSort
import com.jsworld.android.autolog.ui.data.item.MaintenanceStatus
import com.jsworld.android.autolog.ui.data.item.MaintenanceUiModel
import com.jsworld.android.autolog.ui.theme.Notice
import com.jsworld.android.autolog.ui.theme.StatusNormal
import com.jsworld.android.autolog.ui.theme.StatusOverdue
import com.jsworld.android.autolog.ui.theme.StatusSoon
import com.jsworld.android.autolog.ui.view.viewModel.CarDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(
    carId: Long,
    viewModel: CarDetailViewModel,
    onBack: () -> Unit,
    onGoToList: () -> Unit,
    onEditCar: (Long) -> Unit,
    onAddMaintenance: (Long) -> Unit,
    onAddMaintenanceItem: (Long) -> Unit,
    onEditMaintenanceSetting: (Long) -> Unit, // settingId
) {
    val listState = rememberLazyListState()

    var fabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                val scrollingDown = index > prevIndex || (index == prevIndex && offset > prevOffset)
                val scrollingUp = index < prevIndex || (index == prevIndex && offset < prevOffset)

                if (scrollingDown) fabVisible = false
                else if (scrollingUp) fabVisible = true

                prevIndex = index
                prevOffset = offset
            }
    }

    val car by viewModel.carState(carId).collectAsState()
    val sort by viewModel.sortState(carId).collectAsState()
    val settings by viewModel.sortedSettingsState(carId).collectAsState()
    val types by viewModel.maintenanceTypesState().collectAsState()
    val uiStatusList by viewModel.maintenanceStatusState(carId).collectAsState()

    val typeNameMap = remember(types) { types.associate { it.id to it.name } }
    val typeDefaultMap = remember(types) { types.associateBy({ it.id }, { it }) } // 기본주기까지 쓰고 싶으면


    val options by viewModel.settingOptionsState(carId).collectAsState()
    val optionMap = remember(options) { options.associateBy { it.settingId } }

    val minAllowedMileage: Int? = remember(options) {
        options.mapNotNull { it.lastServiceMileage }.maxOrNull()
    }

    var statusExpanded by rememberSaveable(carId) { mutableStateOf(false) }
    val worstStatus: MaintenanceStatus? = remember(uiStatusList) {
        when {
            uiStatusList.any { it.status == MaintenanceStatus.OVERDUE } -> MaintenanceStatus.OVERDUE
            uiStatusList.any { it.status == MaintenanceStatus.SOON } -> MaintenanceStatus.SOON
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(car?.name ?: "차량 정보") },
                navigationIcon = { /* 동일 */ },
                actions = {
                    // 기존 목록 버튼
                    IconButton(onClick = onGoToList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "목록으로")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onAddMaintenance(carId) },
                    icon = { Icon(Icons.Default.Build, null) },
                    text = { Text("정비 기록 추가") }
                )
            }
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
                CarHeader(
                    car = car!!,
                    onEditCar = { onEditCar(carId) },
                    minAllowedMileage = minAllowedMileage,
                    onUpdateMileage = { newMileage ->
                        viewModel.updateCarMileage(carId, newMileage)
                    }
                )
            }

            item {
                MaintenanceStatusHeader(
                    dangerCount = uiStatusList.size,
                    worstStatus = worstStatus,
                    expanded = statusExpanded,
                    onToggle = { statusExpanded = !statusExpanded }
                )
            }

            if (uiStatusList.isEmpty()) {
                item { GoodMaintenanceCard() }
            } else {
                if (statusExpanded) {
                    items(uiStatusList) { model ->
                        MaintenanceStatusCard(model)
                    }
                }
            }

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
                    onClick = { onEditMaintenanceSetting(setting.id) }
                )
            }
        }
    }
}

@Composable
fun CarHeader(
    car: Car,
    minAllowedMileage: Int?,
    onEditCar: () -> Unit,
    onUpdateMileage: (Int) -> Unit,   // ✅ 주행거리 저장 콜백
) {
    val hasNotes = !car.notes.isNullOrBlank()
    var notesExpanded by rememberSaveable(car.id) { mutableStateOf(false) }
    var detailExpanded by rememberSaveable(car.id) { mutableStateOf(false) }
    var showMileageDialog by rememberSaveable(car.id) { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 1) 상단: 차량명/번호판 + 편집
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        car.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        car.plate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onEditCar) {
                    Icon(Icons.Default.Edit, contentDescription = "차량 정보 편집")
                }
            }

            // 2) ✅ 현재 주행거리 "강조 블록" + 즉시 업데이트
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMileageDialog = true },
                shape = RectangleShape,
                tonalElevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(Modifier.weight(1f)) {
                        Text(
                            "현재 주행거리",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${car.mileage.formatKm()} km",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "탭해서 업데이트",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    // 오른쪽 작은 업데이트 버튼(탭 유도)
                    FilledTonalButton(
                        onClick = { showMileageDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("업데이트", fontWeight = FontWeight.SemiBold)
                    }
                }
            }


            if (hasNotes) {
                // ✅ 상세 토글 (연료/연식 + 메모를 한 번에)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailExpanded = !detailExpanded }
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (detailExpanded) "상세 접기" else "상세 보기",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (detailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = detailExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        // ✅ 연료/연식
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoText(label = "연료", value = car.fuelType ?: "-")
                            InfoText(label = "연식", value = car.year ?: "-")
                        }

                        // ✅ 메모(있을 때만)
                        if (hasNotes) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = car.notes.orEmpty(),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMileageDialog) {
        MileageQuickEditDialog(
            currentMileage = car.mileage,
            minAllowedMileage = minAllowedMileage,
            onDismiss = { showMileageDialog = false },
            onSave = { newMileage ->
                onUpdateMileage(newMileage)
                showMileageDialog = false
            }
        )
    }
}

@Composable
private fun RowScope.InfoText(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MileageQuickEditDialog(
    currentMileage: Int,
    minAllowedMileage: Int?,   // ✅ nullable
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by rememberSaveable(currentMileage) { mutableStateOf(currentMileage.toString()) }
    val parsed = text.toIntOrNull()

    val isBelowMin = minAllowedMileage != null && parsed != null && parsed < minAllowedMileage
    val canSave = parsed != null && parsed >= 0 && !isBelowMin

    val minText = minAllowedMileage?.formatKm()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주행거리 업데이트", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "현재 값: ${currentMileage.formatKm()} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit) },
                    label = { Text("새 주행거리(km)") },
                    singleLine = true,
                    isError = isBelowMin,
                    supportingText = {
                        when {
                            text.isBlank() -> Text("숫자만 입력해 주세요.")
                            parsed == null -> Text("올바른 숫자를 입력해 주세요.")
                            isBelowMin -> Text(
                                "가장 최근 정비 기록(${minText} km)보다 낮게 설정할 수 없어요.",
                                color = MaterialTheme.colorScheme.error
                            )
                            minAllowedMileage == null -> Text(
                                "정비 기록이 아직 없어서 자유롭게 입력할 수 있어요.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> Text(
                                "정비 기록(최소 ${minText} km) 이상으로 입력해 주세요.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val v = parsed ?: return@Button
                    if (minAllowedMileage != null && v < minAllowedMileage) return@Button // ✅ 방어
                    onSave(v)
                }
            ) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
fun MaintenanceStatusCard(item: MaintenanceUiModel) {

    val statusColor = when (item.status) {
        MaintenanceStatus.NORMAL -> StatusNormal
        MaintenanceStatus.SOON -> StatusSoon
        MaintenanceStatus.OVERDUE -> StatusOverdue
    }

    val container = statusColor.copy(alpha = 0.14f) // 배경 틴트(디자인 유지)
    val border = statusColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = border.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = when (item.status) {
                        MaintenanceStatus.NORMAL -> Icons.Default.Info
                        MaintenanceStatus.SOON -> Icons.Default.Warning
                        MaintenanceStatus.OVERDUE -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = border,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.remainingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val chipText = when (item.status) {
                MaintenanceStatus.NORMAL -> "정상"
                MaintenanceStatus.SOON -> "임박"
                MaintenanceStatus.OVERDUE -> "필요"
            }

            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(chipText) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun MaintenanceStatusHeader(
    dangerCount: Int,
    worstStatus: MaintenanceStatus?, // ✅ 추가 (OVERDUE / SOON / null)
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val canToggle = dangerCount > 0 // ✅ 위험 항목 있을 때만 토글 가능(버튼 노출)

    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "expandRotation"
    )

    val isDanger = dangerCount > 0
    val label = if (!isDanger) "정상" else "위험 $dangerCount"

    // ✅ 위험도에 따른 색상 (초과=빨강, 임박=노랑, 정상=초록/기본)
    val statusColor = when (worstStatus) {
        MaintenanceStatus.OVERDUE -> StatusOverdue
        MaintenanceStatus.SOON -> StatusSoon
        else -> StatusNormal
    }
    val container = statusColor.copy(alpha = 0.16f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canToggle) Modifier.clickable { onToggle() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "정비 상태",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ✅ GoodMaintenanceCard 상태면 버튼 자체를 숨김
        if (canToggle) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

        AssistChip(
            onClick = {},          // 클릭 안 쓰더라도 넣어야 함
            enabled = false,       // 계속 비활성로 둘 거면
            label = { Text(label) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = container,
                labelColor = statusColor,
                disabledContainerColor = container,
                disabledLabelColor = statusColor
            )
        )
    }
}

@Composable
fun GoodMaintenanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("차량 관리가 잘 되고 있어요", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "현재 위험 항목이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("정상") }
            )
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

            // ✅ 정렬 Chip (드롭다운)
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

            // ✅ 항목 선택 버튼
            FilledTonalButton(
                onClick = onPickItems,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("항목")
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "  ※ 정비내역이 없는 항목은 정렬 되지 않습니다.",
            color = Notice,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

    // ✅ 내역 없을 때 기준값
    val hasAnyHistory = (lastDate != null) || (lastServiceMileage != null)

    // ✅ km 기준: 내역 없으면 0km 기준
    val baseLastMileage = lastServiceMileage ?: 0

    // ✅ date 기준: 내역 없으면 "오늘" 기준
    val baseDateForCalc = lastDate ?: today

    // --------------------------
    // 예정/잔여 계산
    // --------------------------

    // ✅ km 예정/잔여
    val dueMileage = if (intervalKm != null) baseLastMileage + intervalKm else null
    val remainingKm = dueMileage?.let { it - carMileage }

    // ✅ 날짜 예정/잔여
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

    // ✅ 도래(임박) 기준: 주기 대비 15% 이하(0 포함)
    val soonKmThreshold: Int? = intervalKm?.let { interval ->
        max(1, (interval * SOON_RATIO).roundToInt())
    }

    // ✅ 날짜쪽은 "전체 주기 일수" 대비 15% 이하
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

    // ✅ 상태색(고정)
    val statusColor = when {
        isOverdue -> StatusOverdue
        isDue -> StatusSoon
        else -> StatusNormal
    }

    val statusContainer = statusColor.copy(alpha = 0.16f)
    val statusContent = statusColor
    val accent = statusColor

    val stripColor: Color? = when {
        isOverdue || isDue -> statusColor
        else -> null
    }

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
    // ✅ Progress 계산 (여기만 추가!)
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
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {

            if (stripColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                ) {
                    Surface(
                        color = stripColor,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
            } else {
                Spacer(Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
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

                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(statusLabel) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = statusContainer,
                            labelColor = statusContent
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                InfoLine(
                    icon = Icons.Default.History,
                    title = "마지막 정비",
                    value = lastInfoValue
                )

                Spacer(Modifier.height(8.dp))

                InfoLine(
                    icon = Icons.Default.Route,
                    title = "잔여",
                    value = "$remainKmText / $remainDayText",
                    valueColor = accent,
                    boldValue = isOverdue || isDue
                )

                // ✅ ProgressBar(거리/기간) 추가
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
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }

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
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)
private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()