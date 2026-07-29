package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.Car
import java.text.NumberFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    onSave: (Car) -> Unit,
    isFirst: Boolean = false,
    onRestore: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    // 주행거리는 숫자만 보관하고, 표시용 TextFieldValue 로 콤마 포맷을 유지한다
    var mileage by rememberSaveable { mutableStateOf("") }
    var mileageField by remember {
        mutableStateOf(TextFieldValue(mileage.toIntOrNull()?.let { it.formatCommaLocal() } ?: ""))
    }
    var fuelType by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val isValid = name.isNotBlank() && plate.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFirst) "첫 차량 등록" else "차량 등록") },
                navigationIcon = {
                    // 온보딩(첫 실행)에서는 돌아갈 화면이 없으므로 숨긴다
                    if (!isFirst) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 6.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // 네비바 안전
                        .imePadding()            // 키보드 안전
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(
                                Car(
                                    id = 0L,
                                    name = name.trim(),
                                    plate = plate.trim(),
                                    year = year.trim().ifBlank { null },
                                    mileage = mileage.toIntOrNull() ?: 0,
                                    fuelType = fuelType.ifBlank { null },
                                    notes = notes.trim().ifBlank { null }
                                )
                            )
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("차량 등록하기", fontWeight = FontWeight.SemiBold)
                    }

                    if (!isValid) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "차량 이름과 번호판은 필수예요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isFirst) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = onRestore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("이전에 쓰던 기록이 있나요? 백업에서 복원")
                        }
                    }
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                // 안내 카드 (온보딩/일반 추가 구분)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                if (isFirst) "첫 차량을 등록해볼까요?" else "차량 정보를 입력해주세요",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isFirst)
                                    "등록하면 정비 주기를 계산해 알려드려요. 이름과 번호판만 있으면 시작할 수 있어요."
                                else
                                    "등록 후 정비 주기와 알림을 설정할 수 있어요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                // 기본 정보 섹션
                SectionCard(title = "기본 정보") {
                    CarTextField2(
                        label = "차량 이름 *",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "예: 그랜저",
                        leadingIcon = Icons.Default.Badge
                    )

                    CarTextField2(
                        label = "번호판 *",
                        value = plate,
                        onValueChange = { plate = it },
                        placeholder = "예: 12가 3456",
                        leadingIcon = Icons.Default.ConfirmationNumber
                    )

                    CarTextField2(
                        label = "연식",
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        keyboardType = KeyboardType.Number,
                        placeholder = "예: 2021",
                        leadingIcon = Icons.Default.CalendarMonth
                    )
                }
            }

            item {
                // 주행/연료 섹션
                SectionCard(title = "주행 · 연료") {
                    OutlinedTextField(
                        value = mileageField,
                        onValueChange = { input ->
                            val digits = input.text.filter { it.isDigit() }
                            mileage = digits
                            val formatted = digits.toIntOrNull()?.formatCommaLocal() ?: ""
                            mileageField = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        },
                        label = { Text("현재 주행거리") },
                        placeholder = { Text("예: 37,900") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                        trailingIcon = { Text("km", style = MaterialTheme.typography.labelMedium) },
                        supportingText = {
                            Text(
                                "정비 시기 계산에 사용돼요. 나중에 언제든 수정할 수 있어요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FuelTypeChips(
                        selected = fuelType,
                        onSelected = { fuelType = it }
                    )
                }
            }

            item {
                // 메모 섹션
                SectionCard(title = "메모") {
                    CarTextField2(
                        label = "메모",
                        value = notes,
                        onValueChange = { notes = it },
                        singleLine = false,
                        minLines = 3,
                        leadingIcon = Icons.AutoMirrored.Filled.Notes,
                        placeholder = "예: 최근에 타이어 교체함"
                    )
                }
            }

            item { Spacer(Modifier.height(90.dp)) } // bottomBar 높이만큼 여유(스크롤 시 가려짐 방지)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun CarTextField2(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null)
            }
        },
        trailingIcon = {
            if (trailingText != null) {
                Text(trailingText, style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * 연료 타입 선택 칩.
 * 드롭다운 대신 모든 선택지를 한눈에 보여주고 한 번의 탭으로 고른다.
 * 선택된 칩을 다시 누르면 해제된다(선택 항목이 아니므로).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FuelTypeChips(
    selected: String,
    onSelected: (String) -> Unit
) {
    val fuelTypes = listOf("가솔린", "디젤", "LPG", "하이브리드", "전기", "수소", "기타")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                "연료 타입",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fuelTypes.forEach { type ->
                FilterChip(
                    selected = selected == type,
                    onClick = { onSelected(if (selected == type) "" else type) },
                    label = { Text(type) }
                )
            }
        }
    }
}

private fun Int.formatCommaLocal(): String =
    NumberFormat.getIntegerInstance().format(this)
