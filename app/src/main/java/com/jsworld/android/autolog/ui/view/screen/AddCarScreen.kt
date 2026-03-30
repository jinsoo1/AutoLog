package com.jsworld.android.autolog.ui.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.ui.data.item.Car


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    onSave: (Car) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var mileage by rememberSaveable { mutableStateOf("") }
    var fuelType by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val isValid = name.isNotBlank() && plate.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("차량 등록") }
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
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
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
                // 안내 카드
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            Text("차량 정보를 입력해주세요", fontWeight = FontWeight.Bold)
                            Text(
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
                        leadingIcon = Icons.Default.Badge
                    )

                    CarTextField2(
                        label = "번호판 *",
                        value = plate,
                        onValueChange = { plate = it },
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
                SectionCard(title = "주행/연료") {
                    CarTextField2(
                        label = "현재 주행거리",
                        value = mileage,
                        onValueChange = { mileage = it.filter(Char::isDigit) },
                        keyboardType = KeyboardType.Number,
                        placeholder = "예: 37900",
                        leadingIcon = Icons.Default.Route,
                        trailingText = "km"
                    )

                    FuelTypeDropdown2(
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
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
        shape = RoundedCornerShape(16.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelTypeDropdown2(
    selected: String,
    onSelected: (String) -> Unit
) {
    val fuelTypes = listOf("가솔린", "디젤", "LPG", "하이브리드", "전기", "수소", "기타")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("연료 타입") },
            placeholder = { Text("선택") },
            leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(16.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            fuelTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

