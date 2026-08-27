package com.jsworld.android.autolog.presentation.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.domain.model.Symptom
import com.jsworld.android.autolog.domain.model.SymptomRiskLevel
import com.jsworld.android.autolog.domain.model.powertrainLabel
import com.jsworld.android.autolog.domain.model.powertrainNote
import com.jsworld.android.autolog.domain.model.powertrainScopeLabel
import com.jsworld.android.autolog.presentation.model.riskColor
import com.jsworld.android.autolog.presentation.viewModel.SymptomDetailViewModel

/**
 * 증상 상세 — 정보 우선순위가 화면 순서다:
 * 증상명 → 주행위험도 → (안전 증상은 즉시 조치 먼저) → 점검 항목 →
 * 구분 포인트 → 확인 포인트 → 위험도 상승 조건 → 고지 문구.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomDetailScreen(
    onBack: () -> Unit,
    viewModel: SymptomDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "점검 가이드",
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
        when {
            ui.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.symptom == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("증상 정보를 찾을 수 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> SymptomDetailContent(
                symptom = ui.symptom!!,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun SymptomDetailContent(symptom: Symptom, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    symptom.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 상세에서는 공통이어도 밝힌다("모든 차량") — 배지가 없으면
                // 공통인지 태깅을 안 한 건지 사용자가 알 수 없다.
                Spacer(Modifier.width(8.dp))
                PowertrainBadge(
                    label = symptom.powertrainScopeLabel(),
                    specific = symptom.powertrainLabel() != null
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                symptom.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (symptom.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    symptom.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        item { RiskLevelCard(symptom.riskLevel) }

        // 안전 관련 증상은 점검 정보보다 행동 안내가 먼저다.
        if (symptom.immediateAction != null) {
            item {
                Spacer(Modifier.height(8.dp))
                ImmediateActionCard(symptom.immediateAction)
            }
        }

        if (symptom.checks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel("먼저 확인해보세요")
            }
            item {
                ListCard {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        symptom.checks.forEachIndexed { index, check ->
                            CheckRow(order = index + 1, text = check)
                        }
                    }
                }
            }
        }

        if (symptom.distinguishPoints.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel("이렇게 구분해볼 수 있어요")
            }
            item {
                ListCard {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        symptom.distinguishPoints.forEachIndexed { index, point ->
                            DistinguishRow(point)
                            if (index != symptom.distinguishPoints.lastIndex) {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        if (symptom.observationPoints.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel("추가로 확인해보세요")
            }
            item {
                ListCard {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        symptom.observationPoints.forEach { point ->
                            BulletRow(point)
                        }
                    }
                }
            }
        }

        if (symptom.escalations.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel("이런 증상이 함께 있다면 더 주의하세요")
            }
            item {
                ListCard {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        symptom.escalations.forEach { escalation ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    escalation.condition,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.width(8.dp))
                                RiskBadge(escalation.level)
                            }
                        }
                    }
                }
            }
        }

        // 배지가 무슨 뜻인지 풀어준다. 상단 배지는 짧아서 "디젤 해당"이
        // 어느 범위인지 애매한데, 여기서 한 문장으로 마감한다.
        item {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        symptom.powertrainNote(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "이 가이드는 일반적인 점검 참고 정보로, 실제 고장 원인을 확정하지 않아요. " +
                    "차량 상태와 차종에 따라 원인이 다를 수 있고, 안전과 관련된 증상은 전문 정비 점검을 권장해요.",
                modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 이 증상이 어느 동력 계통에 해당하는지 알리는 배지.
 *
 * 목록을 걸러내지 않고 알려주기만 한다 — 내 차에 해당하는지 판단은 사용자가 한다.
 * (연료 타입은 미설정일 수 있고 사용자가 주유 편의로 고르기도 해서, 이 값으로
 * 증상을 숨기면 라벨이 부정확한 사람에게서 안전 증상이 사라진다)
 */
@Composable
private fun PowertrainBadge(label: String, specific: Boolean) {
    // 특정 계통만 해당하면 눈에 띄게, 모든 차량이면 차분하게 — 좁혀진 항목이
    // 더 많은 정보를 담고 있어서다.
    val container =
        if (specific) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val content =
        if (specific) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(color = container, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (specific) Icons.Outlined.LocalGasStation else Icons.Outlined.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = content
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$label 해당",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}

@Composable
private fun RiskLevelCard(level: SymptomRiskLevel) {
    val color = level.riskColor()
    Surface(
        color = color.copy(alpha = 0.10f).compositeOver(MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "주행위험도",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${level.level}단계 · ${level.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                level.guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ImmediateActionCard(action: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "지금은 이렇게 하세요",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    action,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun CheckRow(order: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "$order",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/** "조건 → 해석" 문자열을 조건(굵게)과 해석(회색)으로 나눠 보여준다 */
@Composable
private fun DistinguishRow(point: String) {
    val parts = point.split(" → ", limit = 2)
    if (parts.size == 2) {
        Column {
            Text(
                parts[0],
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "→ ${parts[1]}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        BulletRow(point)
    }
}

@Composable
private fun BulletRow(text: String) {
    Row {
        Text(
            "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
