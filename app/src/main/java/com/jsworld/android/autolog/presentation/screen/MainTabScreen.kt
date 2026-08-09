package com.jsworld.android.autolog.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.presentation.component.CarSwitcherSheet
import com.jsworld.android.autolog.presentation.viewModel.CarContextViewModel
import kotlinx.coroutines.launch

/**
 * 선택된 탭은 채워진 아이콘, 나머지는 외곽선 아이콘을 쓴다.
 * 색만으로 구분하는 것보다 현재 위치가 훨씬 분명해진다.
 */
private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("홈", Icons.Filled.Home, Icons.Outlined.Home),
    MAINTENANCE("정비", Icons.Filled.Build, Icons.Outlined.Build),
    FUEL("주유", Icons.Filled.LocalGasStation, Icons.Outlined.LocalGasStation),
    REPORT("리포트", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("설정", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * 앱의 기본 셸. 탭 전환은 네비게이션 백스택을 쌓지 않고 상태로만 처리한다.
 * (탭마다 화면이 하나뿐이라 중첩 NavHost 가 필요하지 않고, 백스택 누적 버그의 여지도 없다)
 */
@Composable
fun MainTabScreen(
    carContextViewModel: CarContextViewModel,
    onAddCar: () -> Unit,
    onManageCars: () -> Unit,
    onEditCar: (Long) -> Unit,
    onManageItems: (Long) -> Unit,
    onAddMaintenance: (carId: Long, settingId: Long?) -> Unit,
    onOpenItemDetail: (Long) -> Unit,
    onEditHistory: (Long) -> Unit,
    /** 어떤 에너지(주유/충전)를 기록할지 함께 넘긴다 — 플러그인 하이브리드 때문. */
    onAddFuel: (FuelUnit) -> Unit,
    onEditFuel: (Long) -> Unit,
    onNoticeClick: () -> Unit,
    onExcelExportClick: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showSwitcher by rememberSaveable { mutableStateOf(false) }

    val cars by carContextViewModel.cars.collectAsState()
    val selectedCar by carContextViewModel.selectedCar.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 홈이 아닌 탭에서 뒤로가기 → 앱 종료가 아니라 홈으로
    BackHandler(enabled = tab != MainTab.HOME) { tab = MainTab.HOME }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // 본문과 탭바를 가르는 얇은 선. 그림자보다 가볍고 색이 튀지 않는다.
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    MainTab.entries.forEach { item ->
                        val selected = tab == item
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = item },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(23.dp)
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.65f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.65f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    car = selectedCar,
                    onSwitchCar = { showSwitcher = true },
                    onNoticeClick = onNoticeClick,
                    onEditCar = onEditCar,
                    onAddMaintenance = onAddMaintenance,
                    onOpenItemDetail = onOpenItemDetail,
                    onSeeAllRecords = { tab = MainTab.MAINTENANCE },
                    onSeeAllFuel = { tab = MainTab.FUEL },
                    onOpenReport = { tab = MainTab.REPORT }
                )

                MainTab.MAINTENANCE -> MaintenanceTabScreen(
                    car = selectedCar,
                    onSwitchCar = { showSwitcher = true },
                    onManageItems = onManageItems,
                    onAddMaintenance = onAddMaintenance,
                    onEditHistory = onEditHistory
                )

                MainTab.FUEL -> FuelTabScreen(
                    car = selectedCar,
                    onSwitchCar = { showSwitcher = true },
                    onAddFuel = onAddFuel,
                    onEditFuel = onEditFuel
                )

                MainTab.REPORT -> ReportTabScreen(
                    car = selectedCar,
                    onSwitchCar = { showSwitcher = true }
                )

                MainTab.SETTINGS -> SettingsScreen(
                    onBackClick = {},
                    onNoticeClick = onNoticeClick,
                    onExcelExportClick = onExcelExportClick,
                    showBack = false
                )
            }
        }
    }

    if (showSwitcher) {
        CarSwitcherSheet(
            cars = cars,
            selectedCarId = selectedCar?.id,
            onSelect = { car ->
                carContextViewModel.selectCar(car.id)
                showSwitcher = false
            },
            onAddCar = {
                showSwitcher = false
                onAddCar()
            },
            onManageCars = {
                showSwitcher = false
                onManageCars()
            },
            onDismiss = { showSwitcher = false }
        )
    }
}
