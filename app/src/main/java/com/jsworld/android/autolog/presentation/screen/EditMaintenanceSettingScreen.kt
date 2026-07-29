package com.jsworld.android.autolog.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.presentation.state.EditSettingUiState
import com.jsworld.android.autolog.presentation.viewModel.EditMaintenanceSettingViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMaintenanceSettingScreen(
    settingId: Long,
    viewModel: EditMaintenanceSettingViewModel,
    onViewAllHistory: (Long) -> Unit,
    onBack: () -> Unit
) {
    val ui by viewModel.observeUi(settingId).collectAsState(initial = EditSettingUiState())

    var kmText by rememberSaveable(ui.currentKm) { mutableStateOf(ui.currentKm?.toString() ?: "") }
    var monthsText by rememberSaveable(ui.currentMonths) { mutableStateOf(ui.currentMonths?.toString() ?: "") }

    val km = kmText.trim().toIntOrNull()
    val months = monthsText.trim().toIntOrNull()

    val canSave = !ui.loading &&
            (kmText.isBlank() || km != null) &&
            (monthsText.isBlank() || months != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주기 수정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        },

        // 버튼은 bottomBar로 내려서 항상 보이게
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.resetToDefault(settingId, onBack) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("기본값")
                        }

                        Button(
                            onClick = {
                                viewModel.save(
                                    settingId = settingId,
                                    km = kmText.trim().toIntOrNull(),
                                    months = monthsText.trim().toIntOrNull(),
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
        }
    ) { padding ->

        if (ui.loading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val defKmText = ui.defaultKm?.let { "${it}km" } ?: "-"
        val defMonthsText = ui.defaultMonths?.let { "${it}개월" } ?: "-"

        val effectiveKm = ui.currentKm ?: ui.defaultKm
        val effectiveMonths = ui.currentMonths ?: ui.defaultMonths
        val effectiveText =
            "${effectiveKm?.let { "${it}km" } ?: "-"} / ${effectiveMonths?.let { "${it}개월" } ?: "-"}"

        val isUsingDefault = ui.currentKm == null && ui.currentMonths == null

        // 본문은 LazyColumn으로 스크롤
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) 헤더
            item {
                var lastExpanded by rememberSaveable(settingId) { mutableStateOf(false) }

                // 마지막 정비 데이터가 하나라도 있나?
                val hasLast = ui.lastServiceDate != null ||
                        ui.lastServiceMileage != null ||
                        !ui.lastPlace.isNullOrBlank() ||
                        ui.lastCost != null ||
                        !ui.lastMemo.isNullOrBlank()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        // 상단 헤더(기존 그대로)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = ui.typeName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "현재 적용: $effectiveText",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Surface(
                                color = if (isUsingDefault)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    if (isUsingDefault) "기본" else "사용자 설정",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isUsingDefault)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 마지막 정비 내역(접힘/펼침)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = hasLast) { lastExpanded = !lastExpanded }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "마지막 정비",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(72.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 접힘 상태 요약(항상 보여주기)
                            val summary = if (!hasLast) {
                                "없음"
                            } else {
                                val d = ui.lastServiceDate ?: "-"
                                val m = ui.lastServiceMileage?.let { "${it.formatKm()}km" } ?: "-"
                                "$d · $m"
                            }

                            Text(
                                text = summary,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (hasLast) {
                                Icon(
                                    imageVector = if (lastExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = hasLast && !lastExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                // 상세 정보(기존)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HeaderMiniRow(
                                        icon = Icons.Default.Place,
                                        label = "장소",
                                        value = ui.lastPlace?.takeIf { it.isNotBlank() } ?: "-"
                                    )

                                    HeaderMiniRow(
                                        icon = Icons.Default.Payments,
                                        label = "비용",
                                        value = ui.lastCost?.let { "${it.formatKm()}원" } ?: "-"
                                    )

                                    HeaderMiniRow(
                                        icon = Icons.AutoMirrored.Filled.Notes,
                                        label = "메모",
                                        value = ui.lastMemo?.takeIf { it.isNotBlank() } ?: "-",
                                        valueMaxLines = 3
                                    )
                                }

                                // 하단 액션 영역: 전체 내역 보기 + 접기 버튼
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 전체 내역 보기(메인 액션)
                                    FilledTonalButton(
                                        onClick = { onViewAllHistory(settingId) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("전체 내역 보기")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2) 기본 주기
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("기본 주기", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))

                        InfoLineCompact(
                            icon = Icons.Default.Route,
                            title = "거리",
                            value = defKmText
                        )
                        Spacer(Modifier.height(6.dp))
                        InfoLineCompact(
                            icon = Icons.Default.DateRange,
                            title = "기간",
                            value = defMonthsText
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "입력을 비워두면 기본값을 사용합니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 3) 입력 영역
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        OutlinedTextField(
                            value = kmText,
                            onValueChange = { kmText = it.filter(Char::isDigit) },
                            label = { Text("주기 (km)") },
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                            placeholder = { Text(ui.defaultKm?.toString() ?: "") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorTransformation,
                            supportingText = {
                                Text(
                                    text = if (kmText.isBlank()) "비워두면 기본값($defKmText) 사용"
                                    else "입력값: ${km ?: "-"}km",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = monthsText,
                            onValueChange = { monthsText = it.filter(Char::isDigit) },
                            label = { Text("주기 (개월)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            placeholder = { Text(ui.defaultMonths?.toString() ?: "") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                Text(
                                    text = if (monthsText.isBlank()) "비워두면 기본값($defMonthsText) 사용"
                                    else "입력값: ${months ?: "-"}개월",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 마지막 여백(하단바와 겹치지 않게 느낌 주기)
            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}


@Composable
private fun InfoLineCompact(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
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
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LastMaintenanceSection(
    lastServiceDate: String?,
    lastServiceMileage: Int?,
    lastPlace: String?,
    lastCost: Int?,
    lastMemo: String?
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val hasAny = lastServiceDate != null || lastServiceMileage != null ||
            !lastPlace.isNullOrBlank() || lastCost != null || !lastMemo.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasAny) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("마지막 정비 내역", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                if (hasAny) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (!hasAny) {
                Text(
                    text = "저장된 정비 내역이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }

            // 기본(항상 표시): 날짜 + 주행거리만 간단히
            InfoLineCompact(
                icon = Icons.Default.DateRange,
                title = "날짜",
                value = lastServiceDate ?: "-"
            )
            Spacer(Modifier.height(6.dp))
            InfoLineCompact(
                icon = Icons.Default.Route,
                title = "주행",
                value = lastServiceMileage?.let { "${it.formatKm()}km" } ?: "-"
            )

            // 펼침(선택): 장소/비용/메모
            AnimatedVisibility(visible = expanded) {
                Column {
                    if (!lastPlace.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        InfoLineCompact(Icons.Default.Place, "장소", lastPlace)
                    }
                    if (lastCost != null) {
                        Spacer(Modifier.height(8.dp))
                        InfoLineCompact(Icons.Default.Payments, "비용", "${lastCost.formatKm()}원")
                    }
                    if (!lastMemo.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = lastMemo,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderMiniRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueMaxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)