package com.jsworld.android.autolog.ui.view.screen

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
import com.jsworld.android.autolog.ui.view.viewModel.MaintenanceHistoryEditViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryEditScreen(
    historyId: Long,
    viewModel: MaintenanceHistoryEditViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    BackHandler(enabled = ui.showUpdateCarDialog) {
        viewModel.dismissUpdateCarDialog()
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

            // ✅ 버튼 동작은 기존대로
            onConfirmUpdate = { viewModel.confirmUpdateCarMileage(onDone = onBack) },
            onDismissSaveOnly = { viewModel.declineUpdateCarMileage(onDone = onBack) },

            // ✅ 바깥/백버튼/닫기: 다이얼로그만 닫기
            onDismiss = { viewModel.dismissUpdateCarDialog() }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정비 내역 수정") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (ui.showUpdateCarDialog) viewModel.dismissUpdateCarDialog()
                        else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") }
                },
                actions = {
                    IconButton(onClick = { viewModel.delete(onDone = onBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제")
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
                        .navigationBarsPadding()  // ✅ 네비게이션바 가림 방지
                        .imePadding()             // ✅ 키보드 올라올 때 버튼 가림 방지
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    ) {
                        Text("취소")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.save(onDone = onBack) }
                    ) {
                        Text("저장")
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
                .verticalScroll(rememberScrollState()), // ✅ 폼이 길어지면 스크롤
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            // ✅ 범위 안내 카드
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
                            "이전/다음 내역 범위 안에서만 수정할 수 있어요",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    RangeLine(
                        icon = Icons.Default.DateRange,
                        label = "날짜",
                        value = dateRangeText
                    )
                    RangeLine(
                        icon = Icons.Default.Route,
                        label = "주행거리",
                        value = mileageRangeText
                    )

                    if (ui.isLast) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "※ 마지막 내역입니다. 주행거리 변경 시 차량 최종 주행거리 업데이트 여부를 확인합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ✅ 에러 메시지
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

            // ✅ 입력 폼 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "정비 정보",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = ui.date,
                        onValueChange = viewModel::onDateChange,
                        label = { Text("정비 날짜") },
                        placeholder = { Text("yyyy-MM-dd") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ui.mileage,
                        onValueChange = viewModel::onMileageChange,
                        label = { Text("정비 주행거리") },
                        placeholder = { Text("예: 37900") },
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                        trailingIcon = { Text("km", style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = ui.place,
                        onValueChange = viewModel::onPlaceChange,
                        label = { Text("장소") },
                        placeholder = { Text("예: 현대 블루핸즈") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ui.cost,
                        onValueChange = viewModel::onCostChange,
                        label = { Text("비용") },
                        placeholder = { Text("예: 120000") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        trailingIcon = { Text("원", style = MaterialTheme.typography.labelMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = ui.memo,
                        onValueChange = viewModel::onMemoChange,
                        label = { Text("메모") },
                        placeholder = { Text("작업 내용, 부품명 등") },
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
    // ✅ “너무 큰 값” 휴리스틱(원하면 숫자 조정)
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

                // ✅ 경고 박스(너무 큰 값일 때만)
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

                // ✅ 3칸 요약 카드
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

                // ✅ “다음부터 자동 업데이트” 체크박스(설정 저장)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
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

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)