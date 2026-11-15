package com.jsworld.android.autolog.presentation.screen

import com.jsworld.android.autolog.core.ui.localizedItemName
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.asString
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.presentation.component.CarSwitcherSheet
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
import com.jsworld.android.autolog.domain.model.REPEAT_INSPECTION
import com.jsworld.android.autolog.domain.model.REPEAT_INSURANCE
import com.jsworld.android.autolog.domain.model.REPEAT_TAX
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.model.dDayLabel
import com.jsworld.android.autolog.domain.model.formatScheduleDate
import com.jsworld.android.autolog.domain.model.sortSchedules
import com.jsworld.android.autolog.domain.model.suggestInspectionDate
import com.jsworld.android.autolog.domain.model.suggestTaxDate
import com.jsworld.android.autolog.presentation.scheduler.MaintenanceAlertScheduler
import com.jsworld.android.autolog.presentation.viewModel.CarScheduleViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 날짜 일정 — 정기검사·보험 만기·자동차세.
 *
 * 주행거리로 안 잡히는 것들이라 정비(주기)와 화면을 나눈다.
 * **기록이 하나도 없어도 동작하는 유일한 기능**이라, 신규 사용자에게
 * 처음으로 "앱이 일을 한다"를 보여주는 자리이기도 하다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarScheduleScreen(
    carId: Long,
    onBack: () -> Unit,
    viewModel: CarScheduleViewModel = hiltViewModel()
) {
    // 이 화면은 설정·알림에서도 열려서 차량 컨텍스트가 없는 채로 들어온다.
    // 그래서 어느 차인지 화면이 직접 밝히고, 여러 대면 여기서 바꾸게 한다.
    var currentCarId by rememberSaveable(carId) { mutableStateOf(carId) }

    val cars by viewModel.cars.collectAsState()
    val car = remember(cars, currentCarId) { cars.firstOrNull { it.id == currentCarId } }
    var showCarSwitcher by rememberSaveable { mutableStateOf(false) }

    val schedules by viewModel.schedulesState(currentCarId).collectAsState()
    val carYear by viewModel.carYear(currentCarId).collectAsState(initial = null)

    val today = remember { LocalDate.now() }
    val sorted = remember(schedules, today) { sortSchedules(schedules, today) }
    val nearest = sorted.firstOrNull { (it.remainingDays(today) ?: Long.MIN_VALUE) >= 0 }
        ?: sorted.firstOrNull()

    val context = LocalContext.current

    // 첫 일정을 등록할 때만 물어본다 — 화면에 들어오자마자 권한부터 묻지 않는다.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* 거부해도 일정 자체는 저장된다 — 화면에서 D-day 로 볼 수 있다 */ }
    )

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CarSchedule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.schedule_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // 정기검사·자동차세는 차마다 이름이 같고 날짜만 다르다 —
                        // 차 이름이 없으면 어느 차 일정인지 구분할 단서가 없다.
                        val switchable = cars.size > 1
                        Row(
                            modifier = if (switchable) {
                                Modifier.clickable { showCarSwitcher = true }
                            } else Modifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                car?.name ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (switchable) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (switchable) FontWeight.Bold else FontWeight.Normal
                            )
                            if (switchable) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.car_switcher_switch),
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScheduleHeroCard(nearest, today) }

            if (sorted.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.schedule_registered)) }
                item {
                    ListCard {
                        Column {
                            sorted.forEachIndexed { index, schedule ->
                                ScheduleRow(
                                    schedule = schedule,
                                    today = today,
                                    onClick = { editTarget = schedule },
                                    showDivider = index != sorted.lastIndex
                                )
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.schedule_inspection_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ListCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showAddSheet = true }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScheduleLeadingIcon(Icons.Default.Add, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.schedule_add),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.schedule_add_sub),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ScheduleEditSheet(
            existing = null,
            carYear = carYear,
            today = today,
            onDismiss = { showAddSheet = false },
            onDelete = null,
            onSaveCarYear = { year -> viewModel.saveCarYear(currentCarId, year) },
            onSave = { type, title, dueDate, repeatMonths, memo ->
                viewModel.add(
                    carId = currentCarId,
                    type = type,
                    title = title,
                    dueDate = dueDate,
                    repeatMonths = repeatMonths,
                    memo = memo,
                    // 첫 일정 — 알림 채널을 만들고 하루 1회 체인을 세운다.
                    // (정비 알림이 꺼져 있으면 이 체인이 아예 없던 상태다)
                    onNeedsAlertSetup = {
                        AutoLogNotificationHelper.createChannels(context)
                        MaintenanceAlertScheduler.scheduleNext(
                            context,
                            MaintenanceAlertPrefs.DEFAULT_HOUR
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    },
                    onDone = { showAddSheet = false }
                )
            }
        )
    }

    if (showCarSwitcher) {
        // 차량 추가·관리로 갈 경로가 없는 화면이라 그 줄은 띄우지 않는다.
        CarSwitcherSheet(
            cars = cars,
            selectedCarId = currentCarId,
            onSelect = { selected ->
                currentCarId = selected.id
                viewModel.selectCar(selected.id)
                showCarSwitcher = false
            },
            onDismiss = { showCarSwitcher = false }
        )
    }

    editTarget?.let { target ->
        ScheduleEditSheet(
            existing = target,
            carYear = carYear,
            today = today,
            onDismiss = { editTarget = null },
            onDelete = { viewModel.delete(target.id) { editTarget = null } },
            onDone = {
                viewModel.markDone(target.id) { editTarget = null }
            },
            onSave = { type, title, dueDate, repeatMonths, memo ->
                viewModel.update(
                    target.copy(
                        type = type,
                        title = title,
                        dueDate = dueDate,
                        repeatMonths = repeatMonths,
                        memo = memo?.takeIf { it.isNotBlank() }
                    )
                ) { editTarget = null }
            }
        )
    }
}

/* ───────────────────────── 구성 요소 ───────────────────────── */

/** 가장 가까운 일정 — 세차 허브 히어로와 같은 문법(라벨·큰 숫자·보조 설명) */
@Composable
private fun ScheduleHeroCard(nearest: CarSchedule?, today: LocalDate) {
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
                    Brush.linearGradient(listOf(scheme.tertiaryContainer, scheme.primaryContainer))
                )
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = content.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(132.dp)
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val remaining = nearest?.remainingDays(today)
                if (nearest == null || remaining == null) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.schedule_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(R.string.schedule_empty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        stringResource(R.string.schedule_nearest, nearest.title.localizedItemName()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = content.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (remaining >= 0) "$remaining" else "${-remaining}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = content
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(if (remaining >= 0) R.string.schedule_days_left_unit else R.string.schedule_days_over_unit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = content.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        listOfNotNull(
                            formatScheduleDate(nearest.dueDate, today).asString(),
                            nearest.memo?.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.8f)
                    )
                    if (nearest.type == ScheduleType.INSURANCE && remaining in 0..60) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = content.copy(alpha = 0.12f),
                            contentColor = content,
                            shape = CircleShape
                        ) {
                            Text(
                                stringResource(R.string.schedule_insurance_tip),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    schedule: CarSchedule,
    today: LocalDate,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val remaining = schedule.remainingDays(today)
    // 지난 일정만 강조색 — 남은 일정은 차분하게(경고가 아니라 정보다)
    val accent =
        if (remaining != null && remaining < 0) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.tertiary

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScheduleLeadingIcon(schedule.type.icon(), MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                schedule.title.localizedItemName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                listOfNotNull(
                    formatScheduleDate(schedule.dueDate, today).asString(),
                    schedule.repeatMonths?.let { repeatLabel(it) },
                    schedule.memo?.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (remaining != null) {
            Surface(color = accent.copy(alpha = 0.12f), shape = CircleShape) {
                Text(
                    dDayLabel(remaining),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 44.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun ScheduleLeadingIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .size(32.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = tint)
    }
}

private fun ScheduleType.icon(): ImageVector = when (this) {
    ScheduleType.INSPECTION -> Icons.Outlined.CheckCircle
    ScheduleType.INSURANCE -> Icons.Outlined.Shield
    ScheduleType.TAX -> Icons.Outlined.Receipt
    ScheduleType.CUSTOM -> Icons.Outlined.CalendarMonth
}

@Composable
@ReadOnlyComposable
private fun repeatLabel(months: Int): String = when {
    months % 12 == 0 -> stringResource(R.string.schedule_repeat_years, months / 12)
    else -> stringResource(R.string.schedule_repeat_months, months)
}

/* ───────────────────────── 추가·수정 시트 ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditSheet(
    existing: CarSchedule?,
    carYear: String?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onDone: (() -> Unit)? = null,
    /** 여기서 받은 연식을 차량에도 저장한다. 연식이 이미 있으면 호출되지 않는다 */
    onSaveCarYear: (String) -> Unit = {},
    onSave: (
        type: ScheduleType, title: String, dueDate: String,
        repeatMonths: Int?, memo: String?
    ) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(existing?.type ?: ScheduleType.INSPECTION) }
    // 한국어 프리셋 원문으로 저장된 일정도 수정 칸에는 표시 언어로 채운다(제목은 식별자가 아니다).
    val existingTitle = existing?.title?.localizedItemName()
    var title by rememberSaveable { mutableStateOf(existingTitle ?: "") }
    var dueDate by rememberSaveable { mutableStateOf(existing?.dueDate ?: "") }
    var repeatMonths by rememberSaveable { mutableStateOf(existing?.repeatMonths) }
    var memo by rememberSaveable { mutableStateOf(existing?.memo ?: "") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    // 연식이 없는 차는 정기검사 날짜를 계산할 수 없다. 예전엔 날짜 칸이 빈 채로
    // 저장도 막혀 이유를 알 수 없었는데, 이제 그 자리에서 연식을 묻는다.
    var yearInput by rememberSaveable { mutableStateOf("") }
    val needsYear = existing == null && carYear.isNullOrBlank()
    // 프리셋을 고르면 제목·날짜·주기를 채워준다. 이미 손댄 값은 덮지 않는다.
    var touched by rememberSaveable { mutableStateOf(existing != null) }

    // 프리셋 제목은 표시 언어로 채운다 — 제목은 식별자가 아니라 사용자가 고칠 수 있는 이름이다.
    val presetLabels = ScheduleType.entries.associateWith { it.presetLabel() }
    fun applyPreset(next: ScheduleType) {
        type = next
        if (touched) return
        title = presetLabels.getValue(next)
        repeatMonths = next.presetRepeat()
        dueDate = when (next) {
            ScheduleType.INSPECTION -> suggestInspectionDate(carYear, today)?.toString().orEmpty()
            ScheduleType.TAX -> suggestTaxDate(today).toString()
            else -> ""
        }
    }

    // 처음 열릴 때 기본 프리셋 채우기
    remember(existing) {
        if (existing == null) applyPreset(ScheduleType.INSPECTION)
        true
    }

    val canSave = title.isNotBlank() && runCatching { LocalDate.parse(dueDate) }.isSuccess

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (existing == null) R.string.schedule_add else R.string.schedule_edit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (existing == null) {
                Text(
                    stringResource(R.string.schedule_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ScheduleType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { applyPreset(option) },
                            label = { Text(presetLabels.getValue(option), maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; touched = true },
                label = { Text(stringResource(R.string.schedule_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = if (dueDate.isBlank()) "" else formatScheduleDate(dueDate, today).asString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.common_date)) },
                placeholder = { Text(stringResource(R.string.schedule_pick_date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text(stringResource(R.string.common_change)) }
                }
            )
            if (type == ScheduleType.INSPECTION && existing == null) {
                if (needsYear) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { input ->
                            yearInput = input.filter(Char::isDigit).take(4)
                            // 네 자리가 채워지는 순간 날짜를 계산해 넣는다.
                            // 오타(1980 미만·내년 초과)면 null 이라 날짜를 건드리지 않는다.
                            suggestInspectionDate(yearInput, today)?.let { dueDate = it.toString() }
                        },
                        label = { Text(stringResource(R.string.schedule_year_label)) },
                        placeholder = { Text("2021") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        when {
                            yearInput.length < 4 -> stringResource(R.string.schedule_year_hint_enter)
                            suggestInspectionDate(yearInput, today) == null -> stringResource(R.string.schedule_year_hint_check)
                            else -> stringResource(R.string.schedule_year_hint_suggested)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.schedule_year_hint_suggested),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = repeatMonths?.toString().orEmpty(),
                onValueChange = { input ->
                    repeatMonths = input.filter { it.isDigit() }.take(3).toIntOrNull()
                    touched = true
                },
                label = { Text(stringResource(R.string.schedule_repeat_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text(stringResource(R.string.common_memo_optional)) },
                placeholder = { Text(stringResource(R.string.schedule_memo_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (onDone != null) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(if (existing?.repeatMonths != null) R.string.schedule_done_next else R.string.schedule_done_remove),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Button(
                onClick = {
                    // 여기서 받은 연식은 차량에도 남긴다 — 다음부터는 묻지 않아도 된다.
                    if (needsYear && suggestInspectionDate(yearInput, today) != null) {
                        onSaveCarYear(yearInput)
                    }
                    onSave(type, title.trim(), dueDate, repeatMonths, memo)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        // 일정은 미래가 기본이지만 지난 날짜도 허용한다(놓친 일정을 등록할 수 있어야 한다).
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(dueDate).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            }.getOrNull() ?: today.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate().toString()
                        touched = true
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** 저장용 제목 — 한국어 원문으로 고정한다(LocalizedNames 가 표시 언어로 바꾼다). */
internal fun ScheduleType.presetTitle(): String = when (this) {
    ScheduleType.INSPECTION -> "정기검사"
    ScheduleType.INSURANCE -> "보험 만기"
    ScheduleType.TAX -> "자동차세"
    ScheduleType.CUSTOM -> "직접 입력"
}

/** 표시용 라벨 */
@Composable
@ReadOnlyComposable
internal fun ScheduleType.presetLabel(): String = when (this) {
    ScheduleType.CUSTOM -> stringResource(R.string.schedule_custom)
    else -> presetTitle().localizedItemName()
}

internal fun ScheduleType.presetRepeat(): Int? = when (this) {
    ScheduleType.INSPECTION -> REPEAT_INSPECTION
    ScheduleType.INSURANCE -> REPEAT_INSURANCE
    ScheduleType.TAX -> REPEAT_TAX
    ScheduleType.CUSTOM -> null
}
