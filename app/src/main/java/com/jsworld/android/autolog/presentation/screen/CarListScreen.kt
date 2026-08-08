package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarCardUi
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.presentation.theme.StatusNormal
import com.jsworld.android.autolog.presentation.theme.StatusOverdue
import com.jsworld.android.autolog.presentation.theme.StatusSoon
import com.jsworld.android.autolog.presentation.viewModel.CarListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListScreen(
    onAddCarClick: () -> Unit,
    onCarClick: (Car) -> Unit,
    onSettingsClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CarListViewModel = hiltViewModel()
) {
    val uiCars by viewModel.uiCars.collectAsStateWithLifecycle()
    val showBackupBanner by viewModel.showBackupBanner.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CarListTopBar(
                carCount = uiCars.size,
                onSettingsClick = onSettingsClick,
                onBack = onBack
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
                onAdd = onAddCarClick,
                onRestore = onSettingsClick
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (showBackupBanner) {
                    BackupReminderBanner(
                        onBackup = onSettingsClick,
                        onDismiss = { viewModel.dismissBackupBanner() }
                    )
                }
                // 대수 표기는 상단바 서브타이틀("N대 관리 중")로 이동
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

    // 반투명 색을 그대로 카드 배경에 쓰면 카드 그림자(elevation)가 배경 밑으로 비쳐
    // 딤이 낀 것처럼 보인다. surface 위에 미리 합성해 불투명한 색으로 만든다.
    val container = if (car.isPrimary)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            .compositeOver(MaterialTheme.colorScheme.surface)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 상단: 차량명/대표 + 별
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = car.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                // 정보 pill 3개 (연료는 길어져도 한 줄 + … 처리)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(
                        icon = Icons.Default.Route,
                        text = "${car.mileage.formatComma()} km"
                    )
                    InfoPill(
                        icon = Icons.Default.CalendarMonth,
                        text = car.year ?: "-"
                    )
                    // 연료는 남는 공간을 먹고, 한 줄로만 보이도록
                    InfoPill(
                        icon = Icons.Default.LocalGasStation,
                        text = car.fuelType ?: "-",
                        modifier = Modifier.weight(1f) // ⭐ 길면 여기서 줄어듦(ellipsis)
                    )
                }

                // 하단: 정비 요약(1개) — 상태색 틴트 컨테이너
                Surface(
                    color = statusColor.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = statusColor,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = statusLabel,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                summary.title,
                                style = MaterialTheme.typography.titleSmall,
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

                        // 위험 개수가 있을 때만 배지 노출
                        if (ui.dangerCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = statusColor,
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    "위험 ${ui.dangerCount}",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,                     // 한 줄 고정
                overflow = TextOverflow.Ellipsis  // 길면 … 처리
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListTopBar(
    carCount: Int,
    onSettingsClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로"
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = "내 차량",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (carCount > 0) {
                    Text(
                        text = "${carCount}대 관리 중",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
private fun BackupReminderBanner(
    onBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "백업을 권장해요",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "기기를 바꾸거나 앱을 지우면 기록이 사라져요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onBackup) { Text("백업") }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "닫기")
            }
        }
    }
}

@Composable
fun EmptyCarView(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onRestore: () -> Unit
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
            TextButton(onClick = onRestore) {
                Text("이전에 쓰던 기록이 있나요? 백업에서 복원")
            }
        }
    }
}