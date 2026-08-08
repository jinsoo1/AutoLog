package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.MaintenanceStarterPack
import com.jsworld.android.autolog.presentation.viewModel.MaintenanceStarterViewModel

/**
 * 온보딩 — 첫 차량을 등록한 직후, 관리 스타일을 골라 정비 항목을 한 번에 켠다.
 *
 * 이 단계가 없으면 새 차량은 켜진 항목이 0개라서 사용자가 빈 정비 탭을 만나고,
 * 항목을 직접 켜야 한다는 사실을 스스로 알아내야 한다.
 */
@Composable
fun MaintenanceStarterScreen(
    carId: Long,
    viewModel: MaintenanceStarterViewModel,
    onDone: () -> Unit
) {
    val carFlow = remember(carId) { viewModel.observeCar(carId) }
    val car by carFlow.collectAsState(initial = null)
    val fuelType = car?.fuelType

    var selected by rememberSaveable { mutableStateOf(MaintenanceStarterPack.STANDARD) }
    var applying by remember { mutableStateOf(false) }

    // 연료 타입이 정해지면 팩별 실제 항목(전기차는 엔진 항목 제외)이 미리 계산된다.
    val itemsByPack = remember(fuelType) {
        MaintenanceStarterPack.entries.associateWith { viewModel.applicableItems(it, fuelType) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "차량은 어떻게\n관리하시겠어요?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "고른 스타일에 맞춰 정비 항목과 교체 주기를 채워드려요.\n나중에 언제든 항목을 켜고 끌 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }

            MaintenanceStarterPack.entries.forEach { pack ->
                item(key = pack.name) {
                    StarterPackCard(
                        pack = pack,
                        items = itemsByPack[pack].orEmpty(),
                        selected = selected == pack,
                        recommended = pack == MaintenanceStarterPack.STANDARD,
                        onClick = { selected = pack }
                    )
                }
            }
        }

        Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (applying) return@Button
                        applying = true
                        viewModel.apply(carId, selected, fuelType) { onDone() }
                    },
                    enabled = car != null && !applying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val count = itemsByPack[selected]?.size ?: 0
                    Text(
                        if (applying) "설정하는 중…"
                        else "${selected.title} 시작하기 · 항목 ${count}개",
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = onDone,
                    enabled = !applying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("나중에 직접 고를게요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StarterPackCard(
    pack: MaintenanceStarterPack,
    items: List<String>,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val border =
        if (selected) BorderStroke(2.dp, accent)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    // 반투명 색을 그대로 쓰면 그림자·톤과 섞여 딤처럼 보인다(대표차량 카드에서 났던 버그).
    // compositeOver 로 불투명하게 만들고, elevation 도 선택과 무관하게 고정한다.
    val container =
        if (selected) accent.copy(alpha = 0.06f).compositeOver(MaterialTheme.colorScheme.surface)
        else MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,   // Modifier.clickable 과 달리 리플이 둥근 모서리 안에 갇힌다
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Icon(
                        pack.icon(),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(17.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pack.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (recommended) {
                            Spacer(Modifier.width(7.dp))
                            Surface(color = accent.copy(alpha = 0.13f), shape = CircleShape) {
                                Text(
                                    "추천",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                    }
                    Text(
                        "${pack.description} · ${items.size}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "선택됨" else null,
                    tint = if (selected) accent
                    else MaterialTheme.colorScheme.outlineVariant
                )
            }

            // 기본은 앞 몇 개만, 원하면 펼쳐서 전체 확인.
            // 카드 탭은 "선택"이므로, 펼치기는 별도 탭 영역으로 분리한다.
            var expanded by rememberSaveable(pack.name) { mutableStateOf(false) }

            if (items.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))

                if (expanded) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items.forEach { name ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    .compositeOver(MaterialTheme.colorScheme.surface),
                                shape = CircleShape
                            ) {
                                Text(
                                    name,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    val preview = items.take(4).joinToString(" · ")
                    Text(
                        if (items.size > 4) "$preview 외 ${items.size - 4}개" else preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }

                if (items.size > 4) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            if (expanded) "접기" else "항목 모두 보기",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun MaintenanceStarterPack.icon(): ImageVector = when (this) {
    MaintenanceStarterPack.LIGHT -> Icons.Default.Bolt
    MaintenanceStarterPack.STANDARD -> Icons.Outlined.Build
    MaintenanceStarterPack.FULL -> Icons.Default.Verified
}
