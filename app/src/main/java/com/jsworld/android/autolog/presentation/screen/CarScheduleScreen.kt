package com.jsworld.android.autolog.presentation.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.REPEAT_INSPECTION
import com.jsworld.android.autolog.domain.model.REPEAT_INSURANCE
import com.jsworld.android.autolog.domain.model.REPEAT_TAX
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.model.dDayLabel
import com.jsworld.android.autolog.domain.model.formatScheduleDate
import com.jsworld.android.autolog.domain.model.sortSchedules
import com.jsworld.android.autolog.domain.model.suggestInspectionDate
import com.jsworld.android.autolog.domain.model.suggestTaxDate
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
    val schedules by viewModel.schedulesState(carId).collectAsState()
    val carYear by viewModel.carYear(carId).collectAsState(initial = null)

    val today = remember { LocalDate.now() }
    val sorted = remember(schedules, today) { sortSchedules(schedules, today) }
    val nearest = sorted.firstOrNull { (it.remainingDays(today) ?: Long.MIN_VALUE) >= 0 }
        ?: sorted.firstOrNull()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CarSchedule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("날짜 일정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
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
                item { SectionLabel("등록된 일정") }
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
                            "정기검사는 최초 등록일 기준으로 계산해요 (비사업용 승용: 신차 4년, " +
                                "이후 2년). 차종에 따라 다를 수 있으니 등록증의 유효기간으로 " +
                                "맞춰주세요.",
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
                                "일정 추가",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "정기검사 · 보험 만기 · 자동차세 · 직접 입력",
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
            onSave = { type, title, dueDate, repeatMonths, memo ->
                viewModel.add(carId, type, title, dueDate, repeatMonths, memo) {
                    showAddSheet = false
                }
            }
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
                        "등록된 일정이 없어요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "정기검사·보험 만기는 기록이 없어도 챙길 수 있어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        "가장 가까운 일정 · ${nearest.title}",
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
                            if (remaining >= 0) "일 남음" else "일 지남",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = content.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        listOfNotNull(
                            formatScheduleDate(nearest.dueDate, today),
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
                                "갱신 전에 견적 비교해보세요",
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
                schedule.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                listOfNotNull(
                    formatScheduleDate(schedule.dueDate, today),
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

private fun repeatLabel(months: Int): String = when {
    months % 12 == 0 -> "${months / 12}년마다"
    else -> "${months}개월마다"
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
    onSave: (
        type: ScheduleType, title: String, dueDate: String,
        repeatMonths: Int?, memo: String?
    ) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(existing?.type ?: ScheduleType.INSPECTION) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var dueDate by rememberSaveable { mutableStateOf(existing?.dueDate ?: "") }
    var repeatMonths by rememberSaveable { mutableStateOf(existing?.repeatMonths) }
    var memo by rememberSaveable { mutableStateOf(existing?.memo ?: "") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    // 프리셋을 고르면 제목·날짜·주기를 채워준다. 이미 손댄 값은 덮지 않는다.
    var touched by rememberSaveable { mutableStateOf(existing != null) }

    fun applyPreset(next: ScheduleType) {
        type = next
        if (touched) return
        title = next.presetTitle()
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
                    if (existing == null) "일정 추가" else "일정 수정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (existing == null) {
                Text(
                    "종류",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ScheduleType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { applyPreset(option) },
                            label = { Text(option.presetTitle(), maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; touched = true },
                label = { Text("이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = if (dueDate.isBlank()) "" else formatScheduleDate(dueDate, today),
                onValueChange = {},
                readOnly = true,
                label = { Text("날짜") },
                placeholder = { Text("날짜를 선택해주세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("변경") }
                }
            )
            if (type == ScheduleType.INSPECTION && existing == null) {
                Text(
                    "연식으로 계산한 제안이에요 — 등록증의 검사 유효기간으로 맞춰주세요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = repeatMonths?.toString().orEmpty(),
                onValueChange = { input ->
                    repeatMonths = input.filter { it.isDigit() }.take(3).toIntOrNull()
                    touched = true
                },
                label = { Text("반복 (개월, 비우면 1회성)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (선택)") },
                placeholder = { Text("예: OO화재") },
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
                        if (existing?.repeatMonths != null) "완료 — 다음 회차로 넘기기"
                        else "완료 — 일정 지우기",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Button(
                onClick = {
                    onSave(type, title.trim(), dueDate, repeatMonths, memo)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장", fontWeight = FontWeight.Bold)
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

private fun ScheduleType.presetTitle(): String = when (this) {
    ScheduleType.INSPECTION -> "정기검사"
    ScheduleType.INSURANCE -> "보험 만기"
    ScheduleType.TAX -> "자동차세"
    ScheduleType.CUSTOM -> "직접 입력"
}

private fun ScheduleType.presetRepeat(): Int? = when (this) {
    ScheduleType.INSPECTION -> REPEAT_INSPECTION
    ScheduleType.INSURANCE -> REPEAT_INSURANCE
    ScheduleType.TAX -> REPEAT_TAX
    ScheduleType.CUSTOM -> null
}
