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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.jsworld.android.autolog.presentation.component.MaintenanceItemPickSheet
import com.jsworld.android.autolog.presentation.component.previousServiceLabel
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
    onBack: () -> Unit,
    preselectedSettingId: Long? = null
) {
    // Flow 를 만드는 함수들이라 remember 로 고정한다(리컴포지션마다 재구독되지 않도록).
    val optionsFlow = remember(carId) { viewModel.observeSettingOptions(carId) }
    val carFlow = remember(carId) { viewModel.getCar(carId) }
    val autoMileageFlow = remember(carId) { viewModel.observeAutoMileageUpdate(carId) }

    val options by optionsFlow.collectAsState(initial = emptyList())
    val car by carFlow.collectAsState(initial = null)
    val currentMileage = car?.mileage

    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SettingOption?>(null) }

    // 일회성 수리 모드 — 항목을 고르는 대신 수리 이름을 직접 적는다.
    // 주기 없는 항목으로 저장되므로 임박 알림·다음 정비에 나타나지 않는다.
    var repairMode by rememberSaveable { mutableStateOf(false) }
    var repairName by rememberSaveable { mutableStateOf("") }

    // 임박 카드/항목 상세에서 들어온 경우 그 항목을 미리 골라둔다.
    LaunchedEffect(preselectedSettingId, options) {
        if (preselectedSettingId == null || selected != null) return@LaunchedEffect
        selected = options.firstOrNull { it.settingId == preselectedSettingId }
    }

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


    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 선택된 항목의 “이전 정비” 정보
    val lastDate = selected?.lastServiceDate?.toLocalDateOrNull()
    val lastMileage = selected?.lastServiceMileage

    var showMileageDialog by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<PendingMaintenanceSave?>(null) }
    var mileageDecision by remember { mutableStateOf<UpdateMileageDecision?>(null) }

    val autoMileageUpdate by autoMileageFlow.collectAsState(initial = false)

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

    // 검증(기존 그대로) — 수리 모드에서는 이름이 항목을 대신한다.
    val itemValid = if (repairMode) repairName.isNotBlank() else selected != null

    val pickedDate = dateText.toLocalDateOrNull()
    val dateValid = pickedDate != null && (lastDate == null || pickedDate.isAfter(lastDate))

    val mileageValue = mileageRaw
    val mileageValid = mileageRaw != null &&
            mileageValue!! > 0 &&
                (lastMileage == null || mileageValue > lastMileage)

    // 입력된 값이 규칙(이전 기록보다 커야 함)을 실제로 어긴 상태
    val mileageRuleBroken = mileageValue != null && lastMileage != null && mileageValue <= lastMileage

    // 자릿수 오타(예: 38,950 → 389,500) 감지용 소프트 경고 (저장은 막지 않음)
    val mileageSuspiciousHigh = mileageValue != null && mileageValid && (
            (currentMileage != null && currentMileage > 0 && mileageValue >= currentMileage * 5) ||
                    mileageValue > 1_000_000
            )

    val canSave = itemValid && dateValid && mileageValid

    // 저장 버튼이 무엇이 모자란지 직접 말한다(눌러서 스낵바로 혼나지 않도록).
    val saveLabel = when {
        // 수리는 항목 목록이 비어 있어도 기록할 수 있어야 한다.
        !repairMode && options.isEmpty() -> "먼저 정비 항목을 추가해주세요"
        repairMode && !itemValid -> "수리 이름을 입력해주세요"
        !itemValid -> "항목을 선택해주세요"
        !dateValid -> "정비 날짜를 확인해주세요"
        mileageRuleBroken -> "주행거리를 확인해주세요"
        !mileageValid -> "주행거리를 입력해주세요"
        else -> "저장"
    }

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
                            // 저장 버튼은 canSave 일 때만 활성화되므로 여기서 다시 막을 필요가 없다.
                            // 수리 모드에서는 settingId 를 저장 시점에 만들므로 자리만 채운다.
                            val pending = PendingMaintenanceSave(
                                settingId = if (repairMode) -1L else selected!!.settingId,
                                serviceDate = dateText,
                                serviceMileage = mileageRaw!!,
                                place = placeText.takeIf { it.isNotBlank() },
                                cost = costText.toIntOrNull(),
                                memo = memoText.takeIf { it.isNotBlank() }
                            )

                            // 수리/일반을 한 곳에서 분기해 두 저장 경로가 갈라지지 않게 한다.
                            fun doSave(updateCarMileage: Boolean) {
                                if (repairMode) {
                                    viewModel.saveRepairWithOptionalMileageUpdate(
                                        carId, repairName, pending, updateCarMileage, onDone = onBack
                                    )
                                } else {
                                    viewModel.saveWithOptionalMileageUpdate(
                                        carId, pending, updateCarMileage, onDone = onBack
                                    )
                                }
                            }

                            scope.launch {
                                val decision = viewModel.checkMileageUpdateSuggestion(carId, pending.serviceMileage)

                                // 물어볼 필요 없으면 바로 저장
                                if (decision == null || !decision.shouldAsk) {
                                    doSave(updateCarMileage = false)
                                    return@launch
                                }

                                // 자동 업데이트 설정이면: 다이얼로그 없이 차량 주행거리도 같이 올림
                                if (autoMileageUpdate) {
                                    doSave(updateCarMileage = true)
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
                        enabled = canSave,
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
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 입력 규칙은 화면 상단에 상주시키지 않는다. 각 필드 아래에서, 어겼을 때만 알려준다.
            item {
                SectionHeader(
                    title = "무엇을 정비했나요",
                    subtitle = "항목 · 날짜 · 주행거리",
                    icon = Icons.Default.Build
                )
            }

            item {
                FormCard {
                    if (repairMode) {
                        // 일회성 수리 — 항목 대신 수리 이름을 직접 적는다.
                        OutlinedTextField(
                            value = repairName,
                            onValueChange = { repairName = it },
                            label = { Text("수리 이름") },
                            placeholder = { Text("예: 써모스탯 교체") },
                            leadingIcon = { Icon(Icons.Default.Handyman, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ExpandMore, contentDescription = "항목 목록에서 선택")
                                }
                            },
                            supportingText = { Text("탭하면 항목 목록으로 돌아갈 수 있어요") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                                Text(
                                    "주기 없이 저장돼요",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "임박 알림이나 다음 정비 목록에 나타나지 않아요. " +
                                        "필요하면 나중에 항목 상세에서 주기를 설정할 수 있어요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    } else {
                        // 정비 항목 선택 — 좁은 드롭다운 대신 바텀시트를 띄운다.
                        // 항목이 늘어나도 이전 정비·상태를 한눈에 비교할 수 있어야 하기 때문이다.
                        OutlinedTextField(
                            value = selected?.typeName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("정비 항목") },
                            placeholder = { Text("항목을 선택해주세요") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                            supportingText = {
                                selected?.previousServiceLabel()?.let { Text(it) }
                            },
                            colors = disabledFieldReadsAsEnabled(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // 날짜
                    DatePickerField(
                        dateText = dateText,
                        onDateSelected = { dateText = it },
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
                        // 값이 실제로 규칙을 어긴 순간부터 빨갛게 알린다(비어 있을 때는 저장을 눌렀을 때만).
                        isError = mileageRuleBroken,
                        supportingText = {
                            when {
                                mileageRuleBroken -> Text(
                                    text = "이전 정비 ${lastMileage!!.formatKm()}km보다 커야 해요",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )

                                mileageSuspiciousHigh -> Text(
                                    text = "현재 값보다 많이 큽니다. 자릿수를 확인해주세요.",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // 자동으로 채워둔 값이면 그렇다고 밝혀준다.
                                !mileageTouched && currentMileage != null && mileageRaw == currentMileage ->
                                    Text("차량 주행거리를 그대로 넣었어요")

                                lastMileage != null -> Text("이전 정비 ${lastMileage.formatKm()}km 이후 값")

                                else -> Unit
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                SectionHeader(
                    title = "더 남길 내용",
                    subtitle = "정비소 · 비용 · 메모 (선택)",
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

    if (expanded) {
        val overviewFlow = remember(carId) { viewModel.observeMaintenanceOverview(carId) }
        val statuses by overviewFlow.collectAsState(initial = emptyList())

        MaintenanceItemPickSheet(
            options = options,
            statuses = statuses,
            onSelect = { option ->
                selected = option
                repairMode = false
                expanded = false
            },
            onAddItem = {
                expanded = false
                onGoToItemPicker()
            },
            onAddRepair = {
                repairMode = true
                selected = null
                expanded = false
            },
            onDismiss = { expanded = false }
        )
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
                if (repairMode) {
                    viewModel.saveRepairWithOptionalMileageUpdate(carId, repairName, p, updateCarMileage = true, onDone = onBack)
                } else {
                    viewModel.saveWithOptionalMileageUpdate(carId, p, updateCarMileage = true, onDone = onBack)
                }
            },
            onDismissSaveOnly = {
                showMileageDialog = false
                if (repairMode) {
                    viewModel.saveRepairWithOptionalMileageUpdate(carId, repairName, p, updateCarMileage = false, onDone = onBack)
                } else {
                    viewModel.saveWithOptionalMileageUpdate(carId, p, updateCarMileage = false, onDone = onBack)
                }
            },
            onDismiss = { showMileageDialog = false }
        )
    }
}

/**
 * 탭해서 시트를 띄우는 읽기 전용 필드용 색. `enabled = false` 로 두면 텍스트필드가
 * 클릭을 삼키지 않지만 기본 색이 흐려지므로, 활성 상태와 같게 보이도록 맞춘다.
 */
@Composable
private fun disabledFieldReadsAsEnabled() = OutlinedTextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

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
        isError = dateText.isNotBlank() && !dateValid,
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
                if (dateText.isNotBlank() && !dateValid) {
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