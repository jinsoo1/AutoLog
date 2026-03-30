package com.jsworld.android.autolog.ui.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.ui.data.item.MaintenanceHistory
import com.jsworld.android.autolog.ui.data.item.MaintenanceHistoryListUiState
import com.jsworld.android.autolog.ui.view.viewModel.MaintenanceHistoryListViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryListScreen(
    settingId: Long,
    viewModel: MaintenanceHistoryListViewModel,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit
) {
    val ui by viewModel.observeUi(settingId)
        .collectAsState(initial = MaintenanceHistoryListUiState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.typeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->

        if (ui.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (ui.histories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = "저장된 정비 내역이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = ui.histories,
                key = { it.id }
            ) { h ->
                HistoryCard(h) { historyId ->
                    onEdit(historyId)
                }
            }
        }
    }
}

private fun Int.formatComma(): String = NumberFormat.getIntegerInstance().format(this)

@Composable
private fun HistoryCard(
    h: MaintenanceHistory,
    onEdit: (Long) -> Unit
) {
    val dateText = h.serviceDate ?: "-"
    val mileageText = h.serviceMileage?.let { "${it.formatComma()} km" } ?: "-"
    val placeText = h.place?.takeIf { it.isNotBlank() } ?: "-"
    val costText = h.cost?.let { "${it.formatComma()}원" } ?: "-"
    val memoText = h.memo?.takeIf { it.isNotBlank() } ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 왼쪽 포인트 바(조금 둥글게)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.70f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 상단: 날짜/주행거리 + 수정 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = dateText,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = mileageText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // 수정 버튼(아이콘 버튼 + 톤다운)
                    FilledTonalIconButton(
                        onClick = { onEdit(h.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "수정"
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 장소 / 비용 / 메모 (아이콘 배지로 정돈)
                SmallLine(
                    icon = Icons.Default.Place,
                    label = "장소",
                    value = placeText
                )
                SmallLine(
                    icon = Icons.Default.Payments,
                    label = "비용",
                    value = costText
                )
                SmallLine(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "메모",
                    value = memoText,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun SmallLine(
    icon: ImageVector,
    label: String,
    value: String,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 아이콘을 작은 배지로 감싸서 통일감
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(6.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .width(44.dp)
                .padding(top = 2.dp)
        )

        Text(
            text = value,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        )
    }
}