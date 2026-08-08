package com.jsworld.android.autolog.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.SettingOption

/**
 * 정비 항목 선택 시트.
 *
 * 예전에는 좁은 드롭다운에서 골라야 해서 항목이 늘면 비교가 어려웠다.
 * 여기서는 임박한 항목을 위로 올리고 이전 정비·상태를 함께 보여준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceItemPickSheet(
    options: List<SettingOption>,
    statuses: List<MaintenanceUiModel>,
    onSelect: (SettingOption) -> Unit,
    onAddItem: () -> Unit,
    /** 일회성 수리 기록 — 항목 없이 이름만 적는 흐름으로 전환한다. */
    onAddRepair: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val statusBySetting = remember(statuses) { statuses.associateBy { it.settingId } }

    // 임박·초과 → 정상 → 기록 없음 순. 같은 등급 안에서는 원래 순서를 유지한다.
    val sorted = remember(options, statusBySetting) {
        options.sortedBy { option ->
            when (statusBySetting[option.settingId]?.status) {
                MaintenanceStatus.OVERDUE -> 0
                MaintenanceStatus.SOON -> 1
                MaintenanceStatus.NORMAL -> 2
                null -> 3
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
        ) {
            // 제목 반대편에 항목 관리 진입을 둔다.
            // 목록을 손보는 일은 기록 입력과 성격이 달라 목록 안에 섞지 않는다.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "정비 항목",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (sorted.isEmpty()) "켜둔 정비 항목이 없어요"
                        else "교체가 임박한 항목이 위에 있어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable(onClick = onAddItem)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "정비 항목 관리",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(items = sorted, key = { it.settingId }) { option ->
                    ItemRow(
                        option = option,
                        status = statusBySetting[option.settingId],
                        onClick = { onSelect(option) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // 수리는 주기 정비와 성격이 다른 기록이라 디바이더 아래에 따로 둔다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddRepair)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Handyman,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        "수리 기록 추가",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "차량 수리 내역을 입력해요",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    option: SettingOption,
    status: MaintenanceUiModel?,
    onClick: () -> Unit
) {
    val urgent = status?.status == MaintenanceStatus.SOON ||
            status?.status == MaintenanceStatus.OVERDUE

    val badgeColor = when (status?.status) {
        MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.error
        MaintenanceStatus.SOON -> MaterialTheme.colorScheme.tertiary
        MaintenanceStatus.NORMAL -> MaterialTheme.colorScheme.primary
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val badgeLabel = when {
        status == null -> "첫 기록"
        status.status == MaintenanceStatus.OVERDUE -> "초과"
        status.status == MaintenanceStatus.SOON -> "임박"
        else -> "정상"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (urgent) badgeColor.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
        ) {
            Icon(
                if (urgent) Icons.Default.PriorityHigh else Icons.Default.Build,
                contentDescription = null,
                tint = if (urgent) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(6.dp)
                    .size(14.dp)
            )
        }

        Spacer(Modifier.width(11.dp))

        Column(Modifier.weight(1f)) {
            Text(
                option.typeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                option.previousServiceLabel() ?: "기록 없음",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Surface(color = badgeColor.copy(alpha = 0.13f), shape = CircleShape) {
            Text(
                badgeLabel,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

/** "이전 정비 2025.11.03 · 24,300km". 기록이 없으면 null. */
fun SettingOption.previousServiceLabel(): String? {
    val parts = buildList {
        lastServiceDate?.takeIf { it.isNotBlank() }?.let { add(it) }
        lastServiceMileage?.let { add("${java.text.NumberFormat.getIntegerInstance().format(it)}km") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { "이전 정비 $it" }
}
