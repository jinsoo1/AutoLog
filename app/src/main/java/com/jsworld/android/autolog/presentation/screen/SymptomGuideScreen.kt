package com.jsworld.android.autolog.presentation.screen

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.asString
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.domain.model.Symptom
import com.jsworld.android.autolog.domain.model.SymptomCategory
import com.jsworld.android.autolog.domain.model.SymptomRiskLevel
import com.jsworld.android.autolog.domain.model.powertrainLabel
import com.jsworld.android.autolog.presentation.model.riskColor
import com.jsworld.android.autolog.presentation.viewModel.SymptomGuideViewModel

/**
 * 증상별 점검 가이드 — 카테고리·검색으로 증상을 찾는 목록 화면.
 *
 * "진단"이 아니라 "점검 가이드"다. 이 화면은 데이터를 보여주기만 하고,
 * 콘텐츠는 전부 assets/symptom_guide.json 에 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomGuideScreen(
    onBack: () -> Unit,
    onOpenSymptom: (String) -> Unit,
    viewModel: SymptomGuideViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val noticeSeen by viewModel.noticeSeen.collectAsStateWithLifecycle()

    // 목록이 그려진 뒤에 띄운다 — 로딩 중에 뜨면 빈 화면 위에 뜬 것처럼 보인다.
    if (!noticeSeen && !ui.loading) {
        SymptomGuideNoticeDialog(onConfirm = viewModel::markNoticeSeen)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.symptom_guide_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.symptom_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.symptom_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                item {
                    FilterChip(
                        selected = ui.category == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.common_all)) }
                    )
                }
                items(count = SymptomCategory.entries.size) { index ->
                    val category = SymptomCategory.entries[index]
                    FilterChip(
                        selected = ui.category == category,
                        onClick = {
                            viewModel.setCategory(if (ui.category == category) null else category)
                        },
                        label = { Text(category.label.asString(), maxLines = 1) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            when {
                ui.loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                ui.symptoms.isEmpty() -> EmptySearchMessage()

                else -> SymptomList(
                    symptoms = ui.symptoms,
                    onOpenSymptom = onOpenSymptom
                )
            }
        }
    }
}

@Composable
private fun SymptomList(
    symptoms: List<Symptom>,
    onOpenSymptom: (String) -> Unit
) {
    // 카테고리 순서대로 묶는다. 검색·필터 결과도 같은 틀을 유지해
    // "어느 계통의 증상인지"가 항상 함께 보인다.
    val grouped = remember(symptoms) {
        symptoms.groupBy { it.category }.toSortedMap(compareBy { it.ordinal })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grouped.forEach { (category, items) ->
            item(key = "header-${category.name}") {
                Text(
                    category.label.asString(),
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "items-${category.name}") {
                ListCard {
                    items.forEachIndexed { index, symptom ->
                        SymptomRow(
                            symptom = symptom,
                            showDivider = index != items.lastIndex,
                            onClick = { onOpenSymptom(symptom.id) }
                        )
                    }
                }
            }
        }

        item(key = "disclaimer") {
            Text(
                text = stringResource(R.string.symptom_disclaimer),
                modifier = Modifier.padding(top = 18.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SymptomRow(
    symptom: Symptom,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    symptom.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // 전 차종 공통이면 아무것도 붙이지 않는다 — 52건에 같은 배지를 달면 소음이다.
                symptom.powertrainLabel()?.let { label ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(R.string.symptom_applies_to, label.asString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            RiskBadge(symptom.riskLevel)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) RowDivider()
    }
}

/** 목록용 위험도 배지 — 단계 라벨만 짧게 */
@Composable
internal fun RiskBadge(level: SymptomRiskLevel) {
    val color = level.riskColor()
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = level.label.asString().substringBefore(" ·"),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EmptySearchMessage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.symptom_not_found_title), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.symptom_not_found_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 첫 진입 안내 — "이 가이드는 진단하지 않는다".
 *
 * 목록·상세 하단에도 같은 취지의 고지가 있지만, 그건 다 읽고 난 뒤에 보이는 글이다.
 * 이 기능의 가장 중요한 제약이라 **들어오는 순간 한 번은 정면으로** 말한다.
 * 본 사람에게는 다시 띄우지 않는다(전역 설정) — 매번 막아서면 잔소리가 된다.
 */
@Composable
private fun SymptomGuideNoticeDialog(onConfirm: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onConfirm,
        icon = {
            Surface(color = accent.copy(alpha = 0.14f), shape = CircleShape) {
                Icon(
                    Icons.Outlined.Troubleshoot,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(11.dp)
                        .size(23.dp)
                )
            }
        },
        title = {
            Text(
                stringResource(R.string.symptom_notice_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                NoticePoint(
                    icon = Icons.Outlined.Search,
                    title = stringResource(R.string.symptom_notice_1_title),
                    body = stringResource(R.string.symptom_notice_1_body)
                )
                NoticePoint(
                    icon = Icons.Outlined.DirectionsCar,
                    title = stringResource(R.string.symptom_notice_2_title),
                    body = stringResource(R.string.symptom_notice_2_body)
                )
                NoticePoint(
                    icon = Icons.Outlined.WarningAmber,
                    title = stringResource(R.string.symptom_notice_3_title),
                    body = stringResource(R.string.symptom_notice_3_body),
                    emphasized = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_confirm), fontWeight = FontWeight.SemiBold)
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun NoticePoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    emphasized: Boolean = false
) {
    // 안전 항목만 색을 준다 — 셋 다 강조하면 아무것도 강조되지 않는다.
    val accent =
        if (emphasized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (emphasized) MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (emphasized) 1.5.dp else 1.dp,
            color = if (emphasized) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(18.dp),
                tint = if (emphasized) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
