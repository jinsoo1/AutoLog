package com.jsworld.android.autolog.presentation.screen

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.localizedItemName
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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text(stringResource(R.string.es_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                            Text(stringResource(R.string.es_default))
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
                            Text(stringResource(R.string.common_save))
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
        val defMonthsText = ui.defaultMonths?.let { stringResource(R.string.duration_months, it) } ?: "-"

        val effectiveKm = ui.currentKm ?: ui.defaultKm
        val effectiveMonths = ui.currentMonths ?: ui.defaultMonths
        val effectiveText =
            "${effectiveKm?.let { "${it}km" } ?: "-"} / ${effectiveMonths?.let { stringResource(R.string.duration_months, it) } ?: "-"}"

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
                                    text = ui.typeName.localizedItemName(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.es_current_applied, effectiveText),
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
                                    stringResource(if (isUsingDefault) R.string.es_badge_default else R.string.es_badge_custom),
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
                                text = stringResource(R.string.cd_last_service),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(72.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 접힘 상태 요약(항상 보여주기)
                            val summary = if (!hasLast) {
                                stringResource(R.string.common_none)
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
                                        label = stringResource(R.string.common_place),
                                        value = ui.lastPlace?.takeIf { it.isNotBlank() } ?: "-"
                                    )

                                    HeaderMiniRow(
                                        icon = Icons.Default.Payments,
                                        label = stringResource(R.string.common_cost),
                                        value = ui.lastCost?.let { stringResource(R.string.money_amount, it) } ?: "-"
                                    )

                                    HeaderMiniRow(
                                        icon = Icons.AutoMirrored.Filled.Notes,
                                        label = stringResource(R.string.common_memo),
                                        value = ui.lastMemo?.takeIf { it.isNotBlank() } ?: "-",
                                        valueMaxLines = 3
                                    )
                                }

                                // 전체 내역은 이 화면을 띄운 "정비 항목 상세"에서 이미 보여준다.
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
                        Text(stringResource(R.string.es_default_cycle), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))

                        InfoLineCompact(
                            icon = Icons.Default.Route,
                            title = stringResource(R.string.cd_distance),
                            value = defKmText
                        )
                        Spacer(Modifier.height(6.dp))
                        InfoLineCompact(
                            icon = Icons.Default.DateRange,
                            title = stringResource(R.string.cd_period),
                            value = defMonthsText
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.es_empty_uses_default),
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
                            label = { Text(stringResource(R.string.es_cycle_km)) },
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                            placeholder = { Text(ui.defaultKm?.toString() ?: "") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorTransformation,
                            supportingText = {
                                Text(
                                    text = if (kmText.isBlank()) stringResource(R.string.es_blank_default, defKmText)
                                    else stringResource(R.string.es_input_km, km?.toString() ?: "-"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = monthsText,
                            onValueChange = { monthsText = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.es_cycle_months)) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            placeholder = { Text(ui.defaultMonths?.toString() ?: "") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                Text(
                                    text = if (monthsText.isBlank()) stringResource(R.string.es_blank_default, defMonthsText)
                                    else stringResource(R.string.es_input_months, months?.toString() ?: "-"),
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
                Text(stringResource(R.string.es_last_history), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

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
                    text = stringResource(R.string.es_no_history),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }

            // 기본(항상 표시): 날짜 + 주행거리만 간단히
            InfoLineCompact(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.common_date),
                value = lastServiceDate ?: "-"
            )
            Spacer(Modifier.height(6.dp))
            InfoLineCompact(
                icon = Icons.Default.Route,
                title = stringResource(R.string.es_driving),
                value = lastServiceMileage?.let { "${it.formatKm()}km" } ?: "-"
            )

            // 펼침(선택): 장소/비용/메모
            AnimatedVisibility(visible = expanded) {
                Column {
                    if (!lastPlace.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        InfoLineCompact(Icons.Default.Place, stringResource(R.string.common_place), lastPlace)
                    }
                    if (lastCost != null) {
                        Spacer(Modifier.height(8.dp))
                        InfoLineCompact(Icons.Default.Payments, stringResource(R.string.common_cost), stringResource(R.string.money_amount, lastCost))
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