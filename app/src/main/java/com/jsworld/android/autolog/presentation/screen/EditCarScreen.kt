package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.presentation.viewModel.EditCarViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCarScreen(
    carId: Long,
    viewModel: EditCarViewModel,
    onBack: () -> Unit,
    onDeletedGoToList: () -> Unit
) {
    val car by viewModel.observeCar(carId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (car == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("차량 정보 수정") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        // 삭제 버튼(휴지통)
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "차량 삭제")
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    var name by rememberSaveable(car!!.id) { mutableStateOf(car!!.name) }
    var plate by rememberSaveable(car!!.id) { mutableStateOf(car!!.plate) }
    var year by rememberSaveable(car!!.id) { mutableStateOf(car!!.year ?: "") }
    var mileageText by rememberSaveable(car!!.id) { mutableStateOf(car!!.mileage.toString()) }
    var fuelType by rememberSaveable(car!!.id) { mutableStateOf(car!!.fuelType ?: "") }
    var notes by rememberSaveable(car!!.id) { mutableStateOf(car!!.notes ?: "") }
    var isPrimary by rememberSaveable(car!!.id) { mutableStateOf(car!!.isPrimary) }

    var triedSave by remember { mutableStateOf(false) }

    val maxHistoryMileage by viewModel.observeMaxHistoryMileage(carId)
        .collectAsState(initial = null)

    val minAllowedMileage = maxHistoryMileage ?: 0
    val mileage = mileageText.trim().toIntOrNull()
    val nameValid = name.isNotBlank()
    val plateValid = plate.isNotBlank()
    val mileageValid = mileage != null && mileage >= minAllowedMileage
    val canSave = nameValid && plateValid && mileageValid

    val showMileageError =
        // 저장 시도했으면 무조건 기존처럼 에러 표시
        (triedSave && !mileageValid) ||
                // 입력 중에도: 값이 있고(minMileage 계산 가능) & 최소값 미만이면 즉시 표시
                (mileageText.isNotBlank() && mileage != null && mileage < minAllowedMileage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("차량 정보 수정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "차량 삭제")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars) // 네비게이션바 인셋 적용
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) { Text("취소") }

                    Button(
                        onClick = {
                            triedSave = true
                            if (!canSave) return@Button

                            viewModel.save(
                                car!!.copy(
                                    name = name.trim(),
                                    plate = plate.trim(),
                                    year = year.trim().takeIf { it.isNotBlank() },
                                    mileage = mileage,
                                    fuelType = fuelType.trim().takeIf { it.isNotBlank() },
                                    notes = notes.trim().takeIf { it.isNotBlank() },
                                    isPrimary = isPrimary
                                ),
                                onDone = onBack
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 상단 요약 헤더 카드
            item {
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
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (name.isBlank()) "차량 이름" else name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (plate.isBlank()) "번호판" else plate,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isPrimary) {
                            AssistChip(
                                onClick = { },
                                enabled = false,
                                label = { Text("대표") }
                            )
                        }
                    }
                }
            }

            // 기본 정보 섹션
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(title = "기본 정보", icon = Icons.Default.Badge)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("차량 이름 *") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
                            isError = triedSave && !nameValid,
                            supportingText = {
                                if (triedSave && !nameValid) {
                                    Text(
                                        "차량 이름을 입력해주세요.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = plate,
                            onValueChange = { plate = it },
                            label = { Text("번호판 *") },
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null) },
                            isError = triedSave && !plateValid,
                            supportingText = {
                                if (triedSave && !plateValid) {
                                    Text(
                                        "번호판을 입력해주세요.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = year,
                                onValueChange = { input -> year = input.filter(Char::isDigit).take(4) },
                                label = { Text("연식") },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )

                            FuelTypeDropdown(
                                fuelType = fuelType,
                                onFuelTypeChange = { fuelType = it },
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("대표 차량", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "홈/목록에서 우선 표시됩니다.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
                        }
                    }
                }
            }

            // 주행/메모 섹션
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(title = "주행 / 메모", icon = Icons.Default.Route)

                        OutlinedTextField(
                            value = mileageText,
                            onValueChange = { mileageText = it.filter(Char::isDigit) },
                            label = { Text("총 주행거리(km) *") },
                            leadingIcon = { Icon(Icons.Default.Route, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = showMileageError,
                            supportingText = {
                                Column {
                                    Text(
                                        text = "정비 기록상 최대 주행거리: ${minAllowedMileage.formatKm()} km",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    if (showMileageError) {
                                        val err = when {
                                            mileage == null -> "주행거리를 숫자로 입력해주세요."
                                            mileage < minAllowedMileage ->
                                                "정비 기록(최대 ${minAllowedMileage.formatKm()} km)보다 작게 설정할 수 없어요."
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("메모") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("차량 삭제", fontWeight = FontWeight.Bold) },
                text = {
                    Text("이 차량을 삭제할까요?\n삭제하면 관련 정비 설정/내역도 함께 제거됩니다.")
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("취소")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            // 실제 삭제 + 리스트로(스택 정리)
                            viewModel.deleteCar(car!!, onDone = onDeletedGoToList)
                        }
                    ) {
                        Text("삭제")
                    }
                }
            )
        }


    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelTypeDropdown(
    fuelType: String,
    onFuelTypeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("가솔린", "디젤", "LPG", "하이브리드", "전기", "수소", "기타")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = fuelType,
            onValueChange = {}, // 직접 입력 불가
            readOnly = true,
            singleLine = true,
            label = { Text("연료") },
            leadingIcon = { Icon(Icons.Default.LocalGasStation, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onFuelTypeChange(opt)
                        expanded = false
                    }
                )
            }

            // 선택 해제(빈 값)
            DropdownMenuItem(
                text = { Text("선택 안함") },
                onClick = {
                    onFuelTypeChange("")
                    expanded = false
                }
            )
        }
    }
}

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)