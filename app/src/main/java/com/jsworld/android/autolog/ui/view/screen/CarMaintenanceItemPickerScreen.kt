package com.jsworld.android.autolog.ui.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.ui.data.item.CategoryGroup
import com.jsworld.android.autolog.ui.data.item.MaintenanceCategory
import com.jsworld.android.autolog.ui.data.item.MaintenanceTypePickUi
import com.jsworld.android.autolog.ui.data.item.PickerItemUi
import com.jsworld.android.autolog.ui.data.item.groupByCategory
import com.jsworld.android.autolog.ui.view.viewModel.CarMaintenanceItemPickerViewModel
import com.jsworld.android.autolog.ui.view.viewModel.PickerUiEvent
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CarMaintenanceItemPickerScreen(
    carId: Long,
    viewModel: CarMaintenanceItemPickerViewModel,
    onBack: () -> Unit,
    onAddCustomItem: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val managing by viewModel.observeManagingItems(carId).collectAsState(initial = emptyList())
    val restore by viewModel.observeRestoreItems(carId).collectAsState(initial = emptyList())
    val addable by viewModel.observeAddableItems(carId).collectAsState(initial = emptyList())

    val managingGroups = remember(managing) { groupByCategory(managing) }
    val restoreGroups = remember(restore) { groupByCategory(restore) }
    val addableGroups = remember(addable) { groupByCategory(addable) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is PickerUiEvent.Snackbar -> snackbarHostState.showSnackbar(e.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정비 항목 선택") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomItem,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("항목 추가") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // 관리중
            item {
                PickerSectionHeader(
                    title = "관리중",
                    subtitle = "체크 해제하면 비활성화됩니다.",
                    icon = Icons.Default.CheckCircle,
                    count = managing.size
                )
            }
            sectionWithCategoryGroups(
                sectionKey = "managing",
                groups = managingGroups,
                checkedProvider = { true }, // 관리중은 모두 체크 상태
                onCheckedChange = { item, checked ->
                    // checked=false → disable
                    viewModel.setChecked(carId, item.typeId, checked)
                }
            )

            // 복원(비활성)
            item {
                PickerSectionHeader(
                    title = "복원",
                    subtitle = "이전에 관리했던 항목이에요. 다시 켤 수 있어요.",
                    icon = Icons.Default.Restore,
                    count = restore.size
                )
            }
            sectionWithCategoryGroups(
                sectionKey = "restore",
                groups = restoreGroups,
                checkedProvider = { false },
                onCheckedChange = { item, checked ->
                    // checked=true → enable
                    viewModel.setChecked(carId, item.typeId, checked)
                }
            )

            // 추가 가능
            item {
                PickerSectionHeader(
                    title = "추가 가능한 항목",
                    subtitle = "체크하면 관리 목록에 추가됩니다.",
                    icon = Icons.Default.Add,
                    count = addable.size
                )
            }
            sectionWithCategoryGroups(
                sectionKey = "addable",
                groups = addableGroups,
                checkedProvider = { false },
                onCheckedChange = { item, checked ->
                    // checked=true → insert (없으면 insert)
                    viewModel.setChecked(carId, item.typeId, checked)
                }
            )

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PickerSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    count: Int
) {
    val (accent, badgeContainer, badgeContent) = when (title) {
        "관리중" -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "복원" -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Surface(color = badgeContainer, shape = MaterialTheme.shapes.large) {
                Text(
                    text = "$count 개",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeContent
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}


@Composable
private fun CategoryGroupCard(
    category: MaintenanceCategory,
    items: List<PickerItemUi>,
    checkedProvider: (PickerItemUi) -> Boolean,
    onCheckedChange: (PickerItemUi, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {

            // 카드 헤더(카테고리)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                // (선택) 카테고리 안 항목 개수 표시
                Text(
                    text = "${items.size}개",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 카드 내부 항목들
            items.forEachIndexed { index, item ->
                PickerRowInCard(
                    item = item,
                    checked = checkedProvider(item),
                    onCheckedChange = { onCheckedChange(item, it) }
                )

                // 마지막 항목 아래엔 divider 생략
                if (index != items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun PickerRowInCard(
    item: PickerItemUi,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val hasCustomInterval = item.intervalKm != null || item.intervalMonths != null

    // 텍스트 생성 헬퍼
    fun cycleText(km: Int?, months: Int?): String {
        val kmText = km?.let { "${it.formatKm()}km" } ?: "-"
        val moText = months?.let { "${it}개월" } ?: "-"
        return "$kmText / $moText"
    }

    val defaultText = cycleText(item.defaultKm, item.defaultMonths)

    // 실제 사용 주기(커스텀은 우선, 없으면 기본)
    val effectiveKm = item.intervalKm ?: item.defaultKm
    val effectiveMonths = item.intervalMonths ?: item.defaultMonths
    val currentText = cycleText(effectiveKm, effectiveMonths)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = item.typeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(3.dp))

            // 기본 주기(항상 표시)
            Text(
                text = "기본 주기: $defaultText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 차량 설정 주기(커스텀이 있을 때만 표시)
            if (hasCustomInterval) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "현재 설정: $currentText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun LazyListScope.sectionWithCategoryGroups(
    sectionKey: String,
    groups: List<CategoryGroup>,
    checkedProvider: (PickerItemUi) -> Boolean,
    onCheckedChange: (PickerItemUi, Boolean) -> Unit
) {
    if (groups.isEmpty()) {
        item(key = "empty_$sectionKey") {
            Text(
                text = "항목이 없습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        return
    }

    // 그룹(카테고리) 단위로 카드 1개씩
    items(
        items = groups,
        key = { group -> "group_${sectionKey}_${group.category.name}" } // enum이면 name이 고유
    ) { group ->
        CategoryGroupCard(
            category = group.category,
            items = group.items,
            checkedProvider = checkedProvider,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)