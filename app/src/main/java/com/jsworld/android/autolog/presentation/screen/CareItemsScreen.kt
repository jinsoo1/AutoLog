package com.jsworld.android.autolog.presentation.screen

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.data.repository.DefaultCareItems
import com.jsworld.android.autolog.domain.model.CARE_DAY_OPTIONS
import com.jsworld.android.autolog.domain.model.CARE_WASH_COUNT_OPTIONS
import com.jsworld.android.autolog.domain.model.careDaysLabel
import com.jsworld.android.autolog.domain.model.CareCycleUnit
import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.presentation.viewModel.CareDetailViewModel

/**
 * 세차·관리 항목 관리 — 전체 화면.
 *
 * 바텀시트로 두면 목록이 길어질 때 시트 드래그와 내부 스크롤이 겹쳐 조작이 어렵다.
 * 정비 항목 화면과 같은 방식(TopAppBar + LazyColumn + FAB)으로 맞춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareItemsScreen(
    carId: Long,
    onBack: () -> Unit,
    viewModel: CareDetailViewModel = hiltViewModel()
) {
    // 기본 세차는 이 허브의 기준(경과일·세차 횟수)이라 끄거나 주기를 줄 수 없다 —
    // 목록에 두면 "왜 있는지" 혼란만 주므로 아예 보여주지 않는다.
    val allItems by viewModel.carePickItemsState(carId).collectAsState()
    val items = remember(allItems) { allItems.filterNot { it.name == DefaultCareItems.WASH } }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var intervalTarget by rememberSaveable { mutableStateOf(-1L) }

    val enabled = remember(items) { items.filter { it.enabled } }
    val disabled = remember(items) { items.filterNot { it.enabled } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "관리 항목",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("항목 추가") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "세차는 항상 기록되고, 여기서 켠 항목이 세차 기록의 '선택 항목'에 나타나요. " +
                        "주기 버튼을 누르면 '세차 3회마다'처럼 나만의 주기를 정할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (enabled.isNotEmpty()) {
                item { CareItemsSectionLabel("사용 중", enabled.size) }
                items(items = enabled, key = { "on-${it.name}" }) { item ->
                    CareItemCard(
                        item = item,
                        onToggle = { viewModel.setItemEnabled(carId, item.name, it) },
                        onEditInterval = { item.itemId?.let { id -> intervalTarget = id } }
                    )
                }
            }

            if (disabled.isNotEmpty()) {
                item { CareItemsSectionLabel("사용 안 함", disabled.size) }
                items(items = disabled, key = { "off-${it.name}" }) { item ->
                    CareItemCard(
                        item = item,
                        onToggle = { viewModel.setItemEnabled(carId, item.name, it) },
                        onEditInterval = {}
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCareItemDialog(
            existingNames = items.map { it.name },
            onAdd = { name ->
                viewModel.setItemEnabled(carId, name, true)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    intervalTarget.takeIf { it > 0L }?.let { itemId ->
        val item = items.firstOrNull { it.itemId == itemId }
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

@Composable
private fun CareItemsSectionLabel(title: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CareItemCard(
    item: CarePickItem,
    onToggle: (Boolean) -> Unit,
    onEditInterval: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.enabled && item.itemId != null) {
                    val hasCycle = item.intervalWashCount != null || item.intervalDays != null

                    // 현재 주기를 값으로 보여주고(읽기), 칩은 편집 버튼 역할만 한다.
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            item.intervalWashCount != null ->
                                "주기 · 세차 ${item.intervalWashCount}회마다"
                            item.intervalDays != null ->
                                "주기 · ${careDaysLabel(item.intervalDays)}마다"
                            else -> "주기 없음 · 기록만 남겨요"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasCycle) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.outline
                    )

                    Spacer(Modifier.height(4.dp))
                    AssistChip(
                        onClick = onEditInterval,
                        label = {
                            Text(
                                if (hasCycle) "주기 변경" else "주기 설정",
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
            Switch(checked = item.enabled, onCheckedChange = onToggle)
        }
    }
}

/** 항목 직접 추가 — 하부 세차처럼 기본 목록에 없는 항목 */
@Composable
private fun AddCareItemDialog(
    existingNames: List<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val trimmed = name.trim()
    val duplicated = existingNames.any { it == trimmed }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("관리 항목 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("항목 이름") },
                    placeholder = { Text("예: 하부 세차") },
                    singleLine = true,
                    isError = duplicated,
                    modifier = Modifier.fillMaxWidth()
                )
                if (duplicated) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "이미 있는 항목이에요 — 목록에서 켜주세요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = trimmed.isNotEmpty() && !duplicated,
                onClick = { onAdd(trimmed) }
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

/**
 * 주기 설정 — 세차 횟수 / 기간 / 없음.
 * 내용이 짧아 시트 안에서 스크롤이 걸리지 않는다(목록과 달리 겹침 문제가 없다).
 */
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
                item.intervalDays != null -> CareCycleUnit.MONTHS
                else -> CareCycleUnit.WASH_COUNT
            }
        )
    }
    // 값 자체를 상태로 둔다 — 칩은 값과 같을 때만 선택 표시되고, 직접 입력도 같은 값을 고친다.
    var washCountText by rememberSaveable(item.name) {
        mutableStateOf((item.intervalWashCount ?: 3).toString())
    }
    var daysText by rememberSaveable(item.name) {
        mutableStateOf((item.intervalDays ?: 180).toString())
    }
    val washCount = washCountText.toIntOrNull()
    val days = daysText.toIntOrNull()

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
                                onClick = { washCountText = n.toString() },
                                label = { Text("${n}회") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = washCountText,
                        onValueChange = { input ->
                            washCountText = input.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("직접 입력") },
                        suffix = { Text("회마다") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (washCount != null && washCount > 0) {
                            "세차 ${washCount}회마다 ${item.name} — 세차 기록이 쌓이면 자동으로 세어드려요."
                        } else {
                            "1 이상의 횟수를 입력해주세요."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CareCycleUnit.MONTHS -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CARE_DAY_OPTIONS.forEach { d ->
                            FilterChip(
                                selected = days == d,
                                onClick = { daysText = d.toString() },
                                label = { Text(careDaysLabel(d)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { input ->
                            daysText = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("직접 입력") },
                        suffix = { Text("일마다") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (days != null && days > 0) {
                            "마지막 ${item.name} 기록에서 ${days}일" +
                                (careDaysLabel(days).takeIf { it != "${days}일" }?.let { "($it)" } ?: "") +
                                "이 기준이 돼요."
                        } else {
                            "1 이상의 일수를 입력해주세요."
                        },
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
                        CareCycleUnit.MONTHS -> onSave(days, null)
                        CareCycleUnit.NONE -> onSave(null, null)
                    }
                },
                enabled = when (unit) {
                    CareCycleUnit.WASH_COUNT -> washCount != null && washCount > 0
                    CareCycleUnit.MONTHS -> days != null && days > 0
                    CareCycleUnit.NONE -> true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장", fontWeight = FontWeight.Bold)
            }
        }
    }
}
