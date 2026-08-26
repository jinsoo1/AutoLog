package com.jsworld.android.autolog.presentation.screen

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "증상별 점검 가이드",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
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
                        "예: 핸들 떨림, 웅웅, 찌그덕, 탄 냄새",
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
                                contentDescription = "지우기",
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
                        label = { Text("전체") }
                    )
                }
                items(count = SymptomCategory.entries.size) { index ->
                    val category = SymptomCategory.entries[index]
                    FilterChip(
                        selected = ui.category == category,
                        onClick = {
                            viewModel.setCategory(if (ui.category == category) null else category)
                        },
                        label = { Text(category.label, maxLines = 1) }
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
                    category.label,
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
                text = "이 가이드는 일반적인 점검 참고 정보로, 실제 고장 원인을 확정하지 않아요. " +
                    "차량 상태와 차종에 따라 원인이 다를 수 있고, 안전과 관련된 증상은 전문 정비 점검을 권장해요.",
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
                        "$label 해당",
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
            text = level.label.substringBefore(" ·"),
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
            Text("찾는 증상이 없어요", fontWeight = FontWeight.Bold)
            Text(
                "다른 표현으로 검색해보세요. 예: 웅웅, 덜그럭, 탄 냄새",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
