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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.presentation.model.FuelAmountCalc
import com.jsworld.android.autolog.presentation.model.FuelField
import com.jsworld.android.autolog.presentation.viewModel.FuelRecordEditViewModel
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 주유(충전) 기록 입력·수정.
 *
 * 금액 · 주유량 · 단가 중 **둘만 넣으면 나머지는 자동 계산**된다.
 * 자동 계산된 필드에는 "자동" 배지를 달아, 사용자가 넣은 값과 구분한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelRecordEditScreen(
    car: Car?,
    recordId: Long?,
    /** 새 기록으로 남길 에너지 종류. 수정 모드에서는 저장된 값으로 대체된다. */
    requestedUnit: FuelUnit,
    viewModel: FuelRecordEditViewModel,
    onBack: () -> Unit
) {
    val isEdit = recordId != null

    // 수정 모드에서는 기록에 저장된 종류를 따른다(차량 설정으로 되돌리면 안 된다).
    var unit by rememberSaveable(recordId) { mutableStateOf(requestedUnit) }

    var dateText by rememberSaveable(recordId) { mutableStateOf(LocalDate.now().toString()) }
    var mileageText by rememberSaveable(recordId) { mutableStateOf("") }
    var amountText by rememberSaveable(recordId) { mutableStateOf("") }
    var quantityText by rememberSaveable(recordId) { mutableStateOf("") }
    var unitPriceText by rememberSaveable(recordId) { mutableStateOf("") }
    var stationText by rememberSaveable(recordId) { mutableStateOf("") }
    var memoText by rememberSaveable(recordId) { mutableStateOf("") }

    // "직접 입력한 필드"는 따로 들고 있지 않고 텍스트가 비었는지로 판단한다.
    // 별도 상태로 두면 화면 회전 때 초기화돼 자동 계산된 값이 사라진다.
    val editedFields = buildSet {
        if (amountText.isNotBlank()) add(FuelField.AMOUNT)
        if (quantityText.isNotBlank()) add(FuelField.QUANTITY)
        if (unitPriceText.isNotBlank()) add(FuelField.UNIT_PRICE)
    }

    // 회전해도 다시 불러와 편집 중인 값을 덮어쓰지 않도록 saveable 로 둔다.
    var loadedExisting by rememberSaveable(recordId) { mutableStateOf(recordId == null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 수정 모드: 기존 값 채우기. 세 값이 이미 저장돼 있으므로 모두 "직접 입력"으로 취급한다.
    if (recordId != null) {
        val recordFlow = remember(recordId) { viewModel.observeRecord(recordId) }
        val record by recordFlow.collectAsState(initial = null)

        LaunchedEffect(record?.id) {
            val loaded = record ?: return@LaunchedEffect
            if (loadedExisting) return@LaunchedEffect
            unit = loaded.unit
            dateText = loaded.filledAt
            mileageText = loaded.mileage?.toString().orEmpty()
            amountText = loaded.amount?.toString().orEmpty()
            quantityText = loaded.quantity?.let { FuelAmountCalc.formatQuantity(it) }.orEmpty()
            unitPriceText = loaded.unitPrice?.toString().orEmpty()
            stationText = loaded.station.orEmpty()
            memoText = loaded.memo.orEmpty()
            // 세 값이 모두 채워지면 editedFields 도 자동으로 셋이 되므로
            // 저장된 값이 자동 계산으로 덮이지 않는다.
            loadedExisting = true
        }
    }

    // 주행거리를 직접 입력하기 시작하면 자동 제안을 멈춘다 — 사용자의 숫자가 항상 이긴다.
    var mileageAuto by rememberSaveable(recordId) { mutableStateOf(recordId == null) }

    var showDatePicker by rememberSaveable(recordId) { mutableStateOf(false) }

    // 새 기록: 오늘 날짜면 차량 현재 주행거리를 채워둔다.
    // 과거 날짜면 현재 값은 확실히 틀린 값이다(7월 기록에 8월 주행거리) —
    // 그 날짜 앞뒤 기록 사이에 들어가는 값을 제안하고, 근거가 없으면 비워둔다.
    LaunchedEffect(car?.id, recordId, dateText) {
        if (recordId != null || !mileageAuto) return@LaunchedEffect
        val targetCar = car ?: return@LaunchedEffect
        val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return@LaunchedEffect

        mileageText = if (date >= LocalDate.now()) {
            targetCar.mileage.takeIf { it > 0 }?.toString().orEmpty()
        } else {
            viewModel.suggestMileageFor(targetCar.id, dateText)?.toString().orEmpty()
        }
    }

    val stationsFlow = remember(car?.id, unit) {
        car?.id?.let { viewModel.observeRecentStations(it, unit) } ?: flowOf(emptyList())
    }
    val recentStations by stationsFlow.collectAsState(initial = emptyList())

    val autoField = FuelAmountCalc.autoField(editedFields)

    // 직접 입력된 값만 파싱한다. 자동 필드는 아래에서 계산해 채운다.
    val typedAmount = amountText.toIntOrNull()?.takeIf { FuelField.AMOUNT in editedFields }
    val typedQuantity = quantityText.toDoubleOrNull()?.takeIf { FuelField.QUANTITY in editedFields }
    val typedUnitPrice = unitPriceText.toIntOrNull()?.takeIf { FuelField.UNIT_PRICE in editedFields }

    val autoText = autoField?.let {
        FuelAmountCalc.computeDisplay(it, typedAmount, typedQuantity, typedUnitPrice)
    }.orEmpty()

    fun shownText(field: FuelField, own: String) =
        if (field == autoField) autoText else own

    val finalAmount = if (autoField == FuelField.AMOUNT) autoText.toIntOrNull() else typedAmount
    val finalQuantity =
        if (autoField == FuelField.QUANTITY) autoText.toDoubleOrNull() else typedQuantity
    val finalUnitPrice =
        if (autoField == FuelField.UNIT_PRICE) autoText.toIntOrNull() else typedUnitPrice

    val mileage = mileageText.toIntOrNull()
    val dateValid = runCatching { LocalDate.parse(dateText) }.isSuccess

    // 금액과 주유량 중 하나라도 있으면 기록으로서 의미가 있다.
    val hasValue = (finalAmount ?: 0) > 0 || (finalQuantity ?: 0.0) > 0.0

    val saveLabel = when {
        !dateValid -> "날짜를 확인해주세요"
        !hasValue -> "금액이나 ${unit.quantityLabel}을 입력해주세요"
        else -> "저장"
    }
    val canSave = dateValid && hasValue

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "${unit.actionLabel} 기록 수정" else "${unit.actionLabel} 기록",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val targetCar = car ?: return@Button
                            viewModel.save(
                                recordId = recordId,
                                carId = targetCar.id,
                                filledAt = dateText,
                                mileage = mileage,
                                amount = finalAmount,
                                quantity = finalQuantity,
                                unitPrice = finalUnitPrice,
                                unit = unit,
                                station = stationText,
                                memo = memoText,
                                photoPath = null,
                                onDone = onBack
                            )
                        },
                        enabled = canSave && car != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(saveLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 직접 타이핑하는 것보다 달력에서 고르는 게 훨씬 빠르다.
                    // readOnly 텍스트필드는 탭을 자체 소비하므로, 투명 오버레이로 받는다.
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateText.toDisplayDateOrNull() ?: dateText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("날짜") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                // 비활성처럼 흐려 보이면 안 된다 — 누를 수 있는 필드다
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = mileageText,
                        onValueChange = {
                            mileageText = it.filter(Char::isDigit)
                            mileageAuto = false
                        },
                        label = { Text("주행거리") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                        suffix = { Text("km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = shownText(FuelField.AMOUNT, amountText),
                    onValueChange = {
                        amountText = it.filter(Char::isDigit)
                    },
                    label = { Text("${unit.actionLabel} 금액") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    suffix = { Text("원") },
                    trailingIcon = { if (autoField == FuelField.AMOUNT) AutoBadge() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorTransformation,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = shownText(FuelField.QUANTITY, quantityText),
                        onValueChange = {
                            quantityText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        },
                        label = { Text(unit.quantityLabel) },
                        singleLine = true,
                        suffix = { Text(unit.symbol) },
                        trailingIcon = { if (autoField == FuelField.QUANTITY) AutoBadge() },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = shownText(FuelField.UNIT_PRICE, unitPriceText),
                        onValueChange = {
                            unitPriceText = it.filter(Char::isDigit)
                        },
                        label = { Text("단가") },
                        singleLine = true,
                        suffix = { Text("원") },
                        trailingIcon = { if (autoField == FuelField.UNIT_PRICE) AutoBadge() },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "금액 · ${unit.quantityLabel} · 단가 중 둘만 넣으면 나머지는 자동 계산돼요.",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = stationText,
                    onValueChange = { stationText = it },
                    label = { Text("${unit.placeLabel} (선택)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (recentStations.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentStations.take(3).forEach { station ->
                            AssistChip(
                                onClick = { stationText = station },
                                label = { Text(station, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("메모 (선택)") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteDialog && recordId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("기록을 삭제할까요?", fontWeight = FontWeight.Bold) },
            text = { Text("삭제한 ${unit.actionLabel} 기록은 되돌릴 수 없어요.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(recordId, onBack)
                    }
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }

    if (showDatePicker) {
        // 아직 넣지 않은 주유를 미리 기록할 일은 없다 — 오늘 이후는 고를 수 없게.
        val todayEndUtc = remember {
            LocalDate.now().plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(dateText).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
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
                        dateText = Instant.ofEpochMilli(millis)
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

/** 자동 계산된 값임을 알리는 배지. */
@Composable
private fun AutoBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            "자동",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
