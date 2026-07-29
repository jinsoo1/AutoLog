package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.domain.model.SettingOption
import com.jsworld.android.autolog.presentation.viewModel.AddMaintenanceViewModel
import com.jsworld.android.autolog.presentation.viewModel.PendingMaintenanceSave
import com.jsworld.android.autolog.presentation.viewModel.UpdateMileageDecision
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceScreen(
    carId: Long,
    viewModel: AddMaintenanceViewModel,
    onGoToItemPicker: () -> Unit,
    onBack: () -> Unit
) {
    val options by viewModel.observeSettingOptions(carId).collectAsState(initial = emptyList())
    val car by viewModel.getCar(carId).collectAsState(initial = null)
    val currentMileage = car?.mileage

    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SettingOption?>(null) }

    var dateText by rememberSaveable { mutableStateOf("") }
    var mileageTextFieldValue by remember {
        mutableStateOf(TextFieldValue(""))
    }

    var mileageRaw by remember {
        mutableStateOf<Int?>(null)
    }
    // 사용자가 주행거리를 직접 입력했는지 여부(자동 채움이 사용자 입력을 덮어쓰지 않도록)
    var mileageTouched by remember { mutableStateOf(false) }
    var placeText by rememberSaveable { mutableStateOf("") }
    var costText by rememberSaveable { mutableStateOf("") }
    var memoText by rememberSaveable { mutableStateOf("") }

    var triedSave by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 선택된 항목의 “이전 정비” 정보
    val lastDate = selected?.lastServiceDate?.toLocalDateOrNull()
    val lastMileage = selected?.lastServiceMileage

    var showMileageDialog by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<PendingMaintenanceSave?>(null) }
    var mileageDecision by remember { mutableStateOf<UpdateMileageDecision?>(null) }

    val autoMileageUpdate by viewModel.observeAutoMileageUpdate(carId).collectAsState(initial = false)

    // 선택 변경 시 자동 보정 로직(기존 그대로)
    LaunchedEffect(selected?.settingId) {
        val base = java.time.LocalDate.now()
        val suggested = if (lastDate != null) {
            val min = lastDate.plusDays(1)
            if (base.isAfter(min)) base else min
        } else base

        val currentDate = dateText.toLocalDateOrNull()
        if (currentDate == null || (lastDate != null && !currentDate.isAfter(lastDate))) {
            dateText = suggested.toString()
        }

    }

    // 주행거리 기본값 자동 채움: 사용자가 아직 직접 입력하지 않았다면 현재 차량 주행거리로 채운다.
    LaunchedEffect(currentMileage, selected?.settingId) {
        if (mileageTouched) return@LaunchedEffect
        val cm = currentMileage ?: return@LaunchedEffect
        // 이전 정비 기록보다는 커야 하므로 필요 시 보정
        val base = if (lastMileage != null && cm <= lastMileage) lastMileage + 1 else cm
        if (base <= 0) return@LaunchedEffect
        mileageRaw = base
        val formatted = base.formatKm()
        mileageTextFieldValue = TextFieldValue(
            text = formatted,
            selection = TextRange(formatted.length)
        )
    }

    // 검증(기존 그대로)
    val itemValid = selected != null

    val pickedDate = dateText.toLocalDateOrNull()
    val dateValid = pickedDate != null && (lastDate == null || pickedDate.isAfter(lastDate))

    val mileageValue = mileageRaw
    val mileageValid = mileageRaw != null &&
            mileageValue!! > 0 &&
                (lastMileage == null || mileageValue > lastMileage)

    // 자릿수 오타(예: 38,950 → 389,500) 감지용 소프트 경고 (저장은 막지 않음)
    val mileageSuspiciousHigh = mileageValue != null && mileageValid && (
            (currentMileage != null && currentMileage > 0 && mileageValue >= currentMileage * 5) ||
                    mileageValue > 1_000_000
            )

    val canSave = itemValid && dateValid && mileageValid

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("정비 기록 추가") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        },
        // 하단 고정 저장 버튼(키보드/네비게이션 바 대응)
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            triedSave = true
                            if (!canSave) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("필수 항목/조건(이전 기록보다 큰 값)을 확인해주세요.")
                                }
                                return@Button
                            }

                            val pending = PendingMaintenanceSave(
                                settingId = selected!!.settingId,
                                serviceDate = dateText,
                                serviceMileage = mileageRaw!!,
                                place = placeText.takeIf { it.isNotBlank() },
                                cost = costText.toIntOrNull(),
                                memo = memoText.takeIf { it.isNotBlank() }
                            )

                            scope.launch {
                                val decision = viewModel.checkMileageUpdateSuggestion(carId, pending.serviceMileage)

                                // 물어볼 필요 없으면 바로 저장
                                if (decision == null || !decision.shouldAsk) {
                                    viewModel.saveWithOptionalMileageUpdate(carId, pending, updateCarMileage = false, onDone = onBack)
                                    return@launch
                                }

                                // 자동 업데이트 설정이면: 다이얼로그 없이 차량 주행거리도 같이 올림
                                if (autoMileageUpdate) {
                                    viewModel.saveWithOptionalMileageUpdate(carId, pending, updateCarMileage = true, onDone = onBack)
                                    // (선택) 안내 스낵바
                                    snackbarHostState.showSnackbar("차량 주행거리도 ${pending.serviceMileage}km로 업데이트했어요.")
                                    return@launch
                                }

                                // 아니면 다이얼로그 표시
                                pendingSave = pending
                                mileageDecision = decision
                                showMileageDialog = true
                            }
                        },
                        enabled = options.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 상단 안내 카드(가독성)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("입력 규칙", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "정비 날짜와 주행거리는 이전 정비 기록보다 커야 해요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 필수 입력 섹션
            item {
                SectionHeader(
                    title = "필수 입력",
                    subtitle = "정비 항목 · 날짜 · 주행거리",
                    icon = Icons.Default.Build
                )
            }

            item {
                FormCard {
                    // 정비 항목 선택(드롭다운)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selected?.typeName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("정비 항목 *") },
                            leadingIcon = {
                                Icon(Icons.Default.Build, contentDescription = null)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            isError = triedSave && !itemValid,
                            supportingText = {
                                Column {
                                    // 현재 차량 주행거리
                                    val curText = currentMileage?.let { "${it.formatKm()}km" } ?: "불러오는 중…"
                                    Text(
                                        text = "현재 주행거리: $curText",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    // 이전 정비 주행거리
                                    val prevText = lastMileage?.let { "${it.formatKm()}km" } ?: "없음"
                                    Text(
                                        text = "이전 주행거리: $prevText",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    // 에러 메시지(상황별)
                                    if (triedSave && !mileageValid) {
                                        val err = when {
                                            mileageRaw == null -> "주행거리를 숫자로 입력해주세요."
                                            mileageRaw!! <= 0 -> "주행거리는 0보다 큰 값이어야 해요."
                                            lastMileage != null && mileageRaw!! <= lastMileage ->
                                                "이전 정비 주행거리(${lastMileage.formatKm()}km)보다 큰 값을 입력해주세요."
                                            else -> "주행거리를 확인해주세요."
                                        }

                                        Text(
                                            text = err,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(opt.typeName, fontWeight = FontWeight.Bold)
                                            val d = opt.lastServiceDate ?: "없음"
                                            val m = opt.lastServiceMileage?.let { "${it}km" } ?: "없음"
                                            Text(
                                                "이전 정비: $d · $m",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selected = opt
                                        expanded = false
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("정비 항목 추가")
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onGoToItemPicker()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 날짜
                    DatePickerField(
                        dateText = dateText,
                        onDateSelected = { dateText = it },
                        triedSave = triedSave,
                        lastDate = lastDate,
                        snackbarHostState = snackbarHostState
                    )

                    Spacer(Modifier.height(12.dp))

                    // 주행거리

                    OutlinedTextField(
                        value = mileageTextFieldValue,
                        onValueChange = { input ->
                            mileageTouched = true
                            val digits = input.text.filter { it.isDigit() }
                            val raw = digits.toIntOrNull()

                            mileageRaw = raw

                            val formatted = raw?.formatKm().orEmpty()

                            mileageTextFieldValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        },
                        label = { Text("정비 시 주행거리(km) *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        isError = triedSave && !mileageValid,
                        supportingText = {
                            Column {
                                val cur = currentMileage?.let { "${it.formatKm()}km" } ?: "불러오는 중…"

                                Text(
                                    text = "현재 주행거리: $cur",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                val prev = lastMileage?.let { "${it.formatKm()}km" } ?: "없음"

                                Text(
                                    text = "이전 주행거리: $prev",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                if (triedSave && !mileageValid) {
                                    val err = when {
                                        mileageRaw == null -> "주행거리를 숫자로 입력해주세요."
                                        mileageRaw!! <= 0 -> "주행거리는 0보다 큰 값이어야 해요."
                                        lastMileage != null && mileageRaw!! <= lastMileage ->
                                            "이전 정비 주행거리(${lastMileage.formatKm()}km)보다 큰 값을 입력해주세요."

                                        else -> "주행거리를 확인해주세요."
                                    }

                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (mileageSuspiciousHigh) {
                                    Text(
                                        text = "입력한 주행거리가 현재 값보다 많이 큽니다. 자릿수를 확인해주세요.",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 선택 입력 섹션
            item {
                SectionHeader(
                    title = "선택 입력",
                    subtitle = "장소 · 비용 · 메모",
                    icon = Icons.AutoMirrored.Filled.Notes
                )
            }

            item {
                FormCard {
                    OutlinedTextField(
                        value = placeText,
                        onValueChange = { placeText = it },
                        label = { Text("정비 장소") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("비용") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        trailingIcon = { Text("원", style = MaterialTheme.typography.labelMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it },
                        label = { Text("메모") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("차량 주행거리 자동 업데이트", fontWeight = FontWeight.SemiBold)
                            Text(
                                "조건에 해당하면 저장 시 차량 주행거리도 자동으로 갱신해요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = autoMileageUpdate,
                            onCheckedChange = { enabled ->
                                viewModel.setAutoMileageUpdate(carId, enabled) // 여기서도 해제 가능
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMileageDialog && pendingSave != null && mileageDecision != null) {
        val d = mileageDecision!!
        val p = pendingSave!!

        MileageUpdateDialog(
            currentMileage = d.currentCarMileage,
            maxHistoryMileage = d.maxHistoryMileage,
            newMileage = p.serviceMileage,
            autoUpdateEnabled = autoMileageUpdate,
            onAutoUpdateChanged = { enabled ->
                viewModel.setAutoMileageUpdate(carId, enabled) // 설정 저장
            },
            onConfirmUpdate = {
                showMileageDialog = false
                viewModel.saveWithOptionalMileageUpdate(carId, p, updateCarMileage = true, onDone = onBack)
            },
            onDismissSaveOnly = {
                showMileageDialog = false
                viewModel.saveWithOptionalMileageUpdate(carId, p, updateCarMileage = false, onDone = onBack)
            },
            onDismiss = { showMileageDialog = false }
        )
    }
}

@Composable
private fun FormCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

/**
 * 차량 상세화면의 섹션 헤더와 동일한 형태(아이콘 + 제목 + 보조설명 + 구분선).
 */
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    dateText: String,
    onDateSelected: (String) -> Unit,
    triedSave: Boolean,
    lastDate: LocalDate?,
    snackbarHostState: SnackbarHostState
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val minDate = remember(lastDate) { lastDate?.plusDays(1) }

    val minSelectableUtcMillis = remember(minDate) {
        minDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: Long.MIN_VALUE
    }
    val maxExclusiveUtcMillis = remember(today) {
        today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= minSelectableUtcMillis &&
                        utcTimeMillis < maxExclusiveUtcMillis
            }
        }
    )

    val pickedDate = dateText.toLocalDateOrNull()
    val dateValid = pickedDate != null &&
            (minDate == null || !pickedDate.isBefore(minDate)) &&
            !pickedDate.isAfter(today)

    OutlinedTextField(
        value = dateText,
        onValueChange = { /* readOnly */ },
        readOnly = true,
        label = { Text("정비 날짜 *") },
        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        isError = triedSave && !dateValid,
        supportingText = {
            Column {
                Text(
                    text = if (lastDate != null) "이전 정비일: $lastDate" else "이전 정비일: 없음",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "선택 가능: ${minDate?.toString() ?: "제한없음"} ~ ${today}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (triedSave && !dateValid) {
                    Text(
                        text = "이전 정비일 이후이며, 오늘까지의 날짜만 선택할 수 있어요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "날짜 선택")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        if ((minDate != null && selected.isBefore(minDate)) || selected.isAfter(today)) {
                            scope.launch {
                                snackbarHostState.showSnackbar("선택 가능한 날짜 범위를 벗어났어요.")
                            }
                            return@TextButton
                        }

                        onDateSelected(selected.format(DATE_FMT))
                        showDatePicker = false
                    }
                ) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MileageUpdateDialog(
    currentMileage: Int,
    maxHistoryMileage: Int?,
    newMileage: Int,
    autoUpdateEnabled: Boolean,
    onAutoUpdateChanged: (Boolean) -> Unit,
    onConfirmUpdate: () -> Unit,
    onDismissSaveOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    // “너무 큰 값” 휴리스틱(원하면 숫자 조정)
    val diff = (newMileage - currentMileage).coerceAtLeast(0)
    val ratio = if (currentMileage > 0) newMileage.toFloat() / currentMileage.toFloat() else 0f

    val suspiciousByRatio = ratio >= 3.0f          // 현재의 3배 이상
    val suspiciousByDiff = diff >= 200_000         // 20만km 이상 급증
    val suspiciousByHistory = maxHistoryMileage?.let { newMileage >= it * 2 } ?: false

    val isSuspicious = (suspiciousByRatio && suspiciousByDiff) || suspiciousByHistory

    val warnColor = MaterialTheme.colorScheme.error
    val warnContainer = MaterialTheme.colorScheme.errorContainer
    val warnOnContainer = MaterialTheme.colorScheme.onErrorContainer

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = (if (isSuspicious) warnContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isSuspicious) warnColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("주행거리 업데이트", fontWeight = FontWeight.Bold)
                    Text(
                        "차량 주행거리를 업데이트할까요?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    text = "현재 차량 주행거리(${currentMileage.formatKm()}km)에서 이번 정비 주행거리(${newMileage.formatKm()}km)로 업데이트 할까요?",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 경고 박스(너무 큰 값일 때만)
                if (isSuspicious) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = warnContainer),
                        border = BorderStroke(1.dp, warnColor),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = warnColor)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "입력값이 너무 크게 보입니다",
                                    color = warnOnContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val maxText = maxHistoryMileage?.let { "${it.formatKm()}km" } ?: "없음"
                            Text(
                                "현재 대비 +${diff.formatKm()}km (약 ${String.format("%.1f", ratio)}배), 기록 최대: $maxText",
                                color = warnOnContainer,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                "오타가 아니라면 계속 진행해도 괜찮아요.",
                                color = warnOnContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // 3칸 요약 카드
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MileageRow(
                            label = "현재 주행거리",
                            value = "${currentMileage.formatKm()} km",
                            icon = Icons.Default.DirectionsCar,
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        MileageRow(
                            label = "이번 정비",
                            value = "${newMileage.formatKm()} km",
                            icon = Icons.Default.Build,
                            valueColor = if (isSuspicious) warnColor else MaterialTheme.colorScheme.primary,
                            bold = true
                        )
                        val maxText = maxHistoryMileage?.let { "${it.formatKm()} km" } ?: "없음"
                        MileageRow(
                            label = "기록 최대",
                            value = maxText,
                            icon = Icons.Default.History,
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // “다음부터 자동 업데이트” 체크박스(설정 저장)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoUpdateEnabled,
                            onCheckedChange = onAutoUpdateChanged
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("다음부터 자동 업데이트", fontWeight = FontWeight.SemiBold)
                            Text(
                                "조건에 해당하면 차량 주행거리도 자동으로 함께 업데이트해요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmUpdate,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Upgrade, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("업데이트하고 저장", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissSaveOnly,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
            ) { Text("저장만") }
        }
    )
}

@Composable
private fun MileageRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
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
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                color = valueColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}


private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

private fun Int.formatKm(): String =
    NumberFormat.getIntegerInstance().format(this)