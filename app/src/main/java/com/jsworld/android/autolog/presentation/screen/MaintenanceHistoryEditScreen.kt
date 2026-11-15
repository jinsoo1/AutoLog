package com.jsworld.android.autolog.presentation.screen

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import androidx.compose.foundation.layout.size
import com.jsworld.android.autolog.presentation.state.EditHistoryUiState
import com.jsworld.android.autolog.domain.model.isCareItemName
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.text.style.TextOverflow
import com.jsworld.android.autolog.core.ui.localizedItemName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.presentation.viewModel.MaintenanceHistoryEditViewModel
import java.text.NumberFormat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryEditScreen(
    historyId: Long,
    viewModel: MaintenanceHistoryEditViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // 정비 날짜 달력 선택
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler(enabled = ui.showUpdateCarDialog) {
        viewModel.dismissUpdateCarDialog()
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            initialDate = ui.date.toLocalDateOrNull(),
            // 이전/다음 내역 사이(경계 제외)만 선택 가능. 다음 내역이 없으면 오늘까지.
            minDate = ui.prevDate?.plusDays(1),
            maxDate = ui.nextDate?.minusDays(1) ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onSelected = { picked ->
                viewModel.onDateChange(picked.toString())
                showDatePicker = false
            }
        )
    }

    LaunchedEffect(historyId) {
        viewModel.load(historyId)
    }

    if (ui.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (ui.showUpdateCarDialog) {
        MileageUpdateDialog(
            currentMileage = ui.currentCarMileage,
            maxHistoryMileage = ui.maxHistoryMileage,
            newMileage = ui.pendingCarMileage ?: ui.mileage.toIntOrNull() ?: ui.currentCarMileage,
            autoUpdateEnabled = ui.autoUpdateCarMileage,
            onAutoUpdateChanged = { enabled -> viewModel.setAutoMileageUpdate(enabled) },

            // 버튼 동작은 기존대로
            onConfirmUpdate = { viewModel.confirmUpdateCarMileage(onDone = onBack) },
            onDismissSaveOnly = { viewModel.declineUpdateCarMileage(onDone = onBack) },

            // 바깥/백버튼/닫기: 다이얼로그만 닫기
            onDismiss = { viewModel.dismissUpdateCarDialog() }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.he_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (ui.showUpdateCarDialog) viewModel.dismissUpdateCarDialog()
                        else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back)) }
                },
                actions = {
                    IconButton(onClick = { viewModel.delete(onDone = onBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()  // 네비게이션바 가림 방지
                        .imePadding()             // 키보드 올라올 때 버튼 가림 방지
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.save(onDone = onBack) }
                    ) {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
        }
    ) { padding ->

        val dateRangeText = "${ui.prevDate?.toString() ?: "-"}  ~  ${ui.nextDate?.toString() ?: "-"}"
        val mileageRangeText = "${ui.prevMileage?.let { "${it}km" } ?: "-"}  ~  ${ui.nextMileage?.let { "${it}km" } ?: "-"}"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            // 어떤 항목의 기록인지 — 뒤로 나가지 않아도 보이게(첫 리뷰 지적)
            ui.typeName?.let { name -> EditHistoryItemHeader(name, ui) }

            // 범위 안내 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.he_range_note),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    RangeLine(
                        icon = Icons.Default.DateRange,
                        label = stringResource(R.string.common_date),
                        value = dateRangeText
                    )
                    RangeLine(
                        icon = Icons.Default.Route,
                        label = stringResource(R.string.common_mileage),
                        value = mileageRangeText
                    )

                    if (ui.isLast) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.he_last_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 에러 메시지
            if (ui.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            ui.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 입력 폼 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.he_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = ui.date,
                        onValueChange = viewModel::onDateChange,
                        label = { Text(stringResource(R.string.he_date)) },
                        placeholder = { Text("yyyy-MM-dd") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        // 직접 입력도 가능하지만, 달력으로 고르는 편이 빠르고 실수도 없다
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = stringResource(R.string.am_pick_date)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ui.mileage,
                        onValueChange = { viewModel.onMileageChange(it.filter(Char::isDigit)) },
                        label = { Text(stringResource(R.string.he_mileage)) },
                        placeholder = { Text(stringResource(R.string.he_mileage_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                        trailingIcon = { Text("km", style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation
                    )

                    OutlinedTextField(
                        value = ui.place,
                        onValueChange = viewModel::onPlaceChange,
                        label = { Text(stringResource(R.string.common_place)) },
                        placeholder = { Text(stringResource(R.string.he_place_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ui.cost,
                        onValueChange = { viewModel.onCostChange(it.filter(Char::isDigit)) },
                        label = { Text(stringResource(R.string.common_cost)) },
                        placeholder = { Text(stringResource(R.string.he_cost_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        trailingIcon = { Text(stringResource(R.string.money_unit), style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorTransformation
                    )

                    OutlinedTextField(
                        value = ui.memo,
                        onValueChange = viewModel::onMemoChange,
                        label = { Text(stringResource(R.string.common_memo)) },
                        placeholder = { Text(stringResource(R.string.he_memo_placeholder)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RangeLine(
    icon: ImageVector,
    label: String,
    value: String
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
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                    Text(stringResource(R.string.mu_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.mu_question),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    text = stringResource(R.string.mu_body, currentMileage.formatKm(), newMileage.formatKm()),
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
                                    stringResource(R.string.mu_suspicious_title),
                                    color = warnOnContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val maxText = maxHistoryMileage?.let { "${it.formatKm()}km" } ?: stringResource(R.string.common_none)
                            Text(
                                stringResource(R.string.mu_suspicious_body, diff.formatKm(), String.format("%.1f", ratio), maxText),
                                color = warnOnContainer,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                stringResource(R.string.mu_suspicious_hint),
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
                            label = stringResource(R.string.mu_current),
                            value = "${currentMileage.formatKm()} km",
                            icon = Icons.Default.DirectionsCar,
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        MileageRow(
                            label = stringResource(R.string.mu_this),
                            value = "${newMileage.formatKm()} km",
                            icon = Icons.Default.Build,
                            valueColor = if (isSuspicious) warnColor else MaterialTheme.colorScheme.primary,
                            bold = true
                        )
                        val maxText = maxHistoryMileage?.let { "${it.formatKm()} km" } ?: stringResource(R.string.common_none)
                        MileageRow(
                            label = stringResource(R.string.mu_max),
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
                            Text(stringResource(R.string.mu_auto_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.mu_auto_body),
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
                Text(stringResource(R.string.mu_update_and_save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissSaveOnly,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
            ) { Text(stringResource(R.string.mu_save_only)) }
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

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

/**
 * 정비 날짜 선택 달력.
 * 이전/다음 정비 내역 사이의 날짜만 선택할 수 있도록 제한한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDatePickerDialog(
    initialDate: LocalDate?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit
) {
    val minMillis = remember(minDate) {
        minDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: Long.MIN_VALUE
    }
    // 상한은 해당 날짜를 포함하도록 다음날 0시 직전까지 허용
    val maxExclusiveMillis = remember(maxDate) {
        maxDate?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            ?: Long.MAX_VALUE
    }

    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= minMillis && utcTimeMillis < maxExclusiveMillis
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val picked = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onSelected(picked)
                }
            ) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    ) {
        DatePicker(state = state)
    }
}

/** 수정 화면 머리의 항목 카드 — 아이콘 + 항목 이름 + 교체 주기 */
@Composable
private fun EditHistoryItemHeader(typeName: String, ui: EditHistoryUiState) {
    val isCare = isCareItemName(typeName)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Icon(
                if (isCare) Icons.Default.WaterDrop else Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier
                    .padding(9.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                typeName.localizedItemName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                ui.intervalLabel(isCare),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "10,000km · 6개월마다" — 정비 항목 상세 화면과 같은 표기 */
@Composable
@ReadOnlyComposable
private fun EditHistoryUiState.intervalLabel(isCare: Boolean): String {
    val parts = buildList {
        intervalKm?.let { add("${it.formatThousands()}km") }
        intervalMonths?.let { add(stringResource(R.string.duration_months, it)) }
    }
    if (parts.isEmpty()) {
        return stringResource(if (isCare) R.string.id_care_no_cycle else R.string.id_repair_no_cycle)
    }
    val suffix = stringResource(if (usingDefaultIntervals) R.string.id_cycle_suffix_default else R.string.id_cycle_suffix)
    return parts.joinToString(" · ") + suffix
}
