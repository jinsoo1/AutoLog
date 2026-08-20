package com.jsworld.android.autolog.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
import com.jsworld.android.autolog.domain.model.REPEAT_INSPECTION
import com.jsworld.android.autolog.domain.model.REPEAT_INSURANCE
import com.jsworld.android.autolog.domain.model.REPEAT_TAX
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.model.formatScheduleDate
import com.jsworld.android.autolog.domain.model.suggestInspectionDate
import com.jsworld.android.autolog.domain.model.suggestTaxDate
import com.jsworld.android.autolog.presentation.scheduler.MaintenanceAlertScheduler
import com.jsworld.android.autolog.presentation.viewModel.CarScheduleViewModel
import com.jsworld.android.autolog.presentation.viewModel.ScheduleDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 차량 등록 직후 — 날짜 일정 제안.
 *
 * 이 앱에서 **기록이 0건이어도 즉시 값을 주는 유일한 기능**이 날짜 일정이다.
 * 정기검사는 연식만 알면, 자동차세는 아무것도 몰라도 계산된다. 그래서 정비 항목
 * 추천 바로 뒤, 아직 기록이 하나도 없는 이 자리에서 권한다.
 *
 * 숫자를 지어내지 않는다는 원칙은 그대로다 — 보험 만기는 계산할 방법이 없어서
 * 기본으로 끄고, 날짜를 고른 사람만 등록된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleStarterScreen(
    carId: Long,
    onDone: () -> Unit,
    viewModel: CarScheduleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val carYear by viewModel.carYear(carId).collectAsState(initial = null)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* 거부해도 일정은 저장된다 — 화면에서 D-day 로 볼 수 있다 */ }
    )

    // 연식이 없으면 정기검사를 계산할 수 없다. 여기서 물어 차량에도 남긴다.
    var yearInput by rememberSaveable { mutableStateOf("") }
    val effectiveYear = carYear?.takeIf { it.isNotBlank() } ?: yearInput
    val inspectionDate = remember(effectiveYear, today) {
        suggestInspectionDate(effectiveYear, today)
    }
    val taxDate = remember(today) { suggestTaxDate(today) }

    var inspectionOn by rememberSaveable { mutableStateOf(true) }
    var taxOn by rememberSaveable { mutableStateOf(true) }
    var insuranceDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showInsurancePicker by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val drafts = buildList {
        if (inspectionOn && inspectionDate != null) {
            add(
                ScheduleDraft(
                    ScheduleType.INSPECTION, "정기검사",
                    inspectionDate.toString(), REPEAT_INSPECTION
                )
            )
        }
        if (taxOn) {
            add(ScheduleDraft(ScheduleType.TAX, "자동차세", taxDate.toString(), REPEAT_TAX))
        }
        insuranceDate?.let {
            add(ScheduleDraft(ScheduleType.INSURANCE, "보험 만기", it, REPEAT_INSURANCE))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "이런 날짜도\n챙겨드릴까요?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "정비 기록이 없어도 챙길 수 있는 것들이에요.\n" +
                        "2주 전부터 알려드리고, 나중에 언제든 고칠 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                StarterScheduleCard(
                    title = "정기검사",
                    checked = inspectionOn && inspectionDate != null,
                    enabled = inspectionDate != null,
                    dateLabel = inspectionDate?.let { formatScheduleDate(it.toString(), today) },
                    repeatLabel = "2년마다",
                    hint = when {
                        inspectionDate != null ->
                            "연식으로 계산한 제안이에요 — 등록증의 검사 유효기간으로 맞춰주세요."
                        yearInput.isNotEmpty() -> "연식을 다시 확인해주세요"
                        else -> "연식을 알려주시면 계산해드려요"
                    },
                    onToggle = { inspectionOn = it }
                ) {
                    // 연식이 이미 있으면 물을 이유가 없다.
                    if (carYear.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it.filter(Char::isDigit).take(4) },
                            label = { Text("연식") },
                            placeholder = { Text("2021") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                StarterScheduleCard(
                    title = "자동차세",
                    checked = taxOn,
                    enabled = true,
                    dateLabel = formatScheduleDate(taxDate.toString(), today),
                    repeatLabel = "6개월마다",
                    hint = "6월·12월 납기 기준이에요. 연납이면 1월로 바꾸면 돼요.",
                    onToggle = { taxOn = it }
                )
            }

            item {
                StarterScheduleCard(
                    title = "보험 만기",
                    checked = insuranceDate != null,
                    enabled = true,
                    dateLabel = insuranceDate?.let { formatScheduleDate(it, today) },
                    repeatLabel = "1년마다",
                    // 가입일은 앱이 알 방법이 없다 — 지어내지 않고 고르게 한다.
                    hint = "가입일은 앱이 알 수 없어요. 만기일을 고르면 등록돼요.",
                    onToggle = { on -> if (!on) insuranceDate = null else showInsurancePicker = true }
                ) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showInsurancePicker = true }) {
                        Text(if (insuranceDate == null) "만기일 고르기" else "날짜 변경")
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (saving) return@Button
                        saving = true
                        // 여기서 받은 연식은 차량에도 남긴다 — 다음부터 묻지 않아도 된다.
                        if (carYear.isNullOrBlank() && inspectionDate != null && yearInput.isNotBlank()) {
                            viewModel.saveCarYear(carId, yearInput)
                        }
                        viewModel.addAll(
                            carId = carId,
                            drafts = drafts,
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
                            onDone = onDone
                        )
                    },
                    enabled = drafts.isNotEmpty() && !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (saving) "등록하는 중…" else "등록하기 · ${drafts.size}개",
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = onDone,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("나중에 할게요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showInsurancePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = insuranceDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showInsurancePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        insuranceDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showInsurancePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showInsurancePicker = false }) { Text("취소") }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun StarterScheduleCard(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    dateLabel: String?,
    repeatLabel: String,
    hint: String,
    onToggle: (Boolean) -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (checked) BorderStroke(2.dp, accent)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onToggle,
                    enabled = enabled
                )
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        // 날짜가 없으면 "언제"를 지어내지 않는다.
                        dateLabel?.let { "$it · $repeatLabel" } ?: "날짜 미정",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dateLabel != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            extra?.invoke()
        }
    }
}
