package com.jsworld.android.autolog.ui.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.CarCardUi
import com.jsworld.android.autolog.ui.data.item.MaintenanceStatus
import com.jsworld.android.autolog.ui.theme.StatusNormal
import com.jsworld.android.autolog.ui.theme.StatusOverdue
import com.jsworld.android.autolog.ui.theme.StatusSoon
import com.jsworld.android.autolog.ui.view.viewModel.CarListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Car) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: CarListViewModel = hiltViewModel()
) {
    val uiCars by viewModel.uiCars.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CarListTopBar(
                onSettingsClick = onSettingsClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCarClick,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("차량 추가") }
            )
        }
    ) { padding ->
        if (uiCars.isEmpty()) {
            EmptyCarView(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                onAdd = onAddCarClick
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "${uiCars.size}대",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiCars, key = { it.car.id }) { ui ->
                        CarSummaryCard(
                            ui = ui,
                            onClick = { onCarClick(ui.car) },
                            onPrimaryClick = { viewModel.selectPrimaryCar(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarSummaryCard(
    ui: CarCardUi,
    onClick: () -> Unit,
    onPrimaryClick: (Car) -> Unit
) {
    val car = ui.car
    val summary = ui.summary

    val (statusColor, statusLabel, statusIcon) = when (summary.status) {
        MaintenanceStatus.OVERDUE -> Triple(StatusOverdue, "필요", Icons.Default.Error)
        MaintenanceStatus.SOON -> Triple(StatusSoon, "임박", Icons.Default.Warning)
        MaintenanceStatus.NORMAL -> Triple(StatusNormal, "정상", Icons.Default.CheckCircle)
    }

    val showStrip = summary.status != MaintenanceStatus.NORMAL
    val container = if (car.isPrimary)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else
        MaterialTheme.colorScheme.surface

    val badgeText = if (ui.dangerCount > 0) "위험 ${ui.dangerCount}" else "정상"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // ✅ 왼쪽 상태 스트립(임박/초과일 때만)
            if (showStrip) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(statusColor)
                )
            } else {
                Spacer(Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 상단: 차량명/대표 + 별
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = car.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (car.isPrimary) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(
                                        "대표",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                car.plate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { onPrimaryClick(ui.car) }) {
                        Icon(
                            imageVector = if (car.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "대표 차량 설정",
                            tint = if (car.isPrimary) Color(0xFFFFB300)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ✅ 정보 pill 3개 (연료는 길어져도 한 줄 + … 처리)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(
                        icon = Icons.Default.Route,
                        text = "${car.mileage.formatComma()} km"
                    )
                    InfoPill(
                        icon = Icons.Default.CalendarMonth,
                        text = car.year ?: "-"
                    )
                    // ✅ 연료는 남는 공간을 먹고, 한 줄로만 보이도록
                    InfoPill(
                        icon = Icons.Default.LocalGasStation,
                        text = car.fuelType ?: "-",
                        modifier = Modifier.weight(1f) // ⭐ 길면 여기서 줄어듦(ellipsis)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 하단: 정비 요약(1개)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            summary.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            summary.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // ✅ 칩 대신 더 미니멀한 상태 배지
                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            badgeText, // ✅ "위험 3" / "정상"
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,                    // ✅ 한 줄 고정
                overflow = TextOverflow.Ellipsis  // ✅ 길면 … 처리
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListTopBar(
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "내 차량",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정"
                )
            }
        }
    )
}



private fun Int.formatComma(): String =
    java.text.NumberFormat.getIntegerInstance().format(this)

@Composable
fun EmptyCarView(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(56.dp))
            Text("등록된 차량이 없어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("오른쪽 아래 버튼으로 차량을 추가해보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("차량 추가")
            }
        }
    }
}