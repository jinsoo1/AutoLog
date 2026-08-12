package com.jsworld.android.autolog.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jsworld.android.autolog.presentation.navigation.Routes.ADD_CAR_FIRST
import com.jsworld.android.autolog.presentation.navigation.Routes.ADD_MAINTENANCE_TYPE
import com.jsworld.android.autolog.presentation.navigation.Routes.EDIT_MAINTENANCE_HISTORY
import com.jsworld.android.autolog.presentation.navigation.Routes.EDIT_MAINTENANCE_SETTING
import com.jsworld.android.autolog.presentation.navigation.Routes.EXCEL_EXPORT
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.presentation.screen.AddCarScreen
import com.jsworld.android.autolog.presentation.screen.AddMaintenanceScreen
import com.jsworld.android.autolog.presentation.screen.AddMaintenanceTypeScreen
import com.jsworld.android.autolog.presentation.screen.CarDetailScreen
import com.jsworld.android.autolog.presentation.screen.CarListScreen
import com.jsworld.android.autolog.presentation.component.CarSwitcherSheet
import com.jsworld.android.autolog.presentation.screen.CareDetailScreen
import com.jsworld.android.autolog.presentation.screen.ReportScreen
import com.jsworld.android.autolog.presentation.screen.CarMaintenanceItemPickerScreen
import com.jsworld.android.autolog.presentation.screen.EditCarScreen
import com.jsworld.android.autolog.presentation.screen.EditMaintenanceSettingScreen
import com.jsworld.android.autolog.presentation.screen.ExcelExportScreen
import com.jsworld.android.autolog.presentation.screen.FuelRecordEditScreen
import com.jsworld.android.autolog.presentation.screen.MainTabScreen
import com.jsworld.android.autolog.presentation.screen.MaintenanceStarterScreen
import com.jsworld.android.autolog.presentation.screen.MaintenanceHistoryEditScreen
import com.jsworld.android.autolog.presentation.screen.MaintenanceItemDetailScreen
import com.jsworld.android.autolog.presentation.screen.NoticeScreen
import com.jsworld.android.autolog.presentation.screen.SettingsScreen
import com.jsworld.android.autolog.presentation.viewModel.AddMaintenanceTypeViewModel
import com.jsworld.android.autolog.presentation.viewModel.AddMaintenanceViewModel
import com.jsworld.android.autolog.presentation.viewModel.CarContextViewModel
import com.jsworld.android.autolog.presentation.viewModel.CarMaintenanceItemPickerViewModel
import com.jsworld.android.autolog.presentation.viewModel.EditCarViewModel
import com.jsworld.android.autolog.presentation.viewModel.EditMaintenanceSettingViewModel
import com.jsworld.android.autolog.presentation.viewModel.MainViewModel


private const val ENTER = 260
private const val EXIT = 220

@Composable
fun AutoLogNavHost(
    navController: NavHostController,
    carContextViewModel: CarContextViewModel,
    initialWidgetCarId: Long?,
    onConsumeInitialWidget: () -> Unit = {}
) {
    val viewModel: MainViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,

        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ENTER)) + fadeIn(tween(ENTER))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(EXIT))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ENTER)) + fadeIn(tween(ENTER))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(EXIT))
        }
    ) {
        composable(Routes.SPLASH) {
            SplashRoute(
                navController = navController,
                carContextViewModel = carContextViewModel,
                initialWidgetCarId = initialWidgetCarId,
                onConsumeInitialWidget = onConsumeInitialWidget
            )
        }

        composable(
            route = ADD_CAR_FIRST,
            arguments = listOf(
                navArgument("first") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isFirst = backStackEntry.arguments?.getBoolean("first") ?: false
            val context = LocalContext.current

            // 온보딩(첫 차량) 화면에서 백업 파일(SAF)로 복원
            val restoreLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri ?: return@rememberLauncherForActivityResult
                viewModel.restoreBackup(uri) { success, message ->
                    if (success) {
                        navController.navigateToMainRoot()
                    } else {
                        Toast.makeText(
                            context,
                            message ?: "복원에 실패했습니다.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            AddCarScreen(
                isFirst = isFirst,
                onBack = { navController.popBackStack() },
                onRestore = {
                    restoreLauncher.launch(
                        arrayOf(
                            "application/json",
                            "text/json",
                            "text/plain",
                            "application/octet-stream"
                        )
                    )
                },
                onSave = { car ->
                    // 저장이 끝나면 정비 항목 추천 화면으로 — 첫 차량이든 n번째든.
                    // (새 차량은 켜진 항목이 0개라 이 단계가 없으면 빈 정비 탭을 만난다)
                    viewModel.addCar(car) { carId ->
                        navController.navigate(
                            Routes.maintenanceStarter(carId, first = isFirst)
                        ) {
                            // 등록 화면은 스택에서 걷어낸다 — 추천에서 뒤로 가도
                            // 빈 등록 폼으로 돌아가지 않게.
                            popUpTo(ADD_CAR_FIRST) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainTabScreen(
                carContextViewModel = carContextViewModel,
                onAddCar = { navController.navigate(Routes.addCarFirst(false)) },
                onManageCars = { navController.navigate(Routes.CAR_LIST) { launchSingleTop = true } },
                onEditCar = { carId ->
                    navController.navigate("${Routes.EDIT_CAR}/$carId") { launchSingleTop = true }
                },
                onManageItems = { carId ->
                    navController.navigate(Routes.carDetail(carId)) { launchSingleTop = true }
                },
                onAddMaintenance = { carId, settingId ->
                    navController.navigate(Routes.addMaintenance(carId, settingId)) {
                        launchSingleTop = true
                    }
                },
                onOpenItemDetail = { settingId ->
                    navController.navigate(Routes.maintenanceItemDetail(settingId)) {
                        launchSingleTop = true
                    }
                },
                onEditHistory = { historyId ->
                    navController.navigate("$EDIT_MAINTENANCE_HISTORY/$historyId") {
                        launchSingleTop = true
                    }
                },
                onAddFuel = { unit ->
                    navController.navigate(Routes.fuelRecord(unit = unit.symbol)) {
                        launchSingleTop = true
                    }
                },
                onEditFuel = { recordId ->
                    // 종류는 화면에서 기록을 읽어 결정하므로 여기서는 기본값을 넘긴다.
                    navController.navigate(Routes.fuelRecord(recordId)) { launchSingleTop = true }
                },
                onNoticeClick = { navController.navigate(Routes.NOTICE) },
                onExcelExportClick = { navController.navigate(EXCEL_EXPORT) },
                onOpenCareDetail = { carId ->
                    navController.navigate(Routes.careDetail(carId)) { launchSingleTop = true }
                },
                onOpenReport = {
                    navController.navigate(Routes.REPORT) { launchSingleTop = true }
                }
            )
        }

        // 지출 리포트 — 홈 카드·설정에서 진입. 차량 전환은 이 화면에서도 가능하다.
        composable(Routes.REPORT) {
            val cars by carContextViewModel.cars.collectAsState()
            val selectedCar by carContextViewModel.selectedCar.collectAsState()
            var showSwitcher by rememberSaveable { mutableStateOf(false) }

            ReportScreen(
                car = selectedCar,
                onBack = { navController.popBackStack() },
                onSwitchCar = { showSwitcher = true }
            )

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
                        navController.navigate(Routes.addCarFirst(false))
                    },
                    onManageCars = {
                        showSwitcher = false
                        navController.navigate(Routes.CAR_LIST) { launchSingleTop = true }
                    },
                    onDismiss = { showSwitcher = false }
                )
            }
        }

        // 세차·관리 허브
        composable(
            route = Routes.CARE_DETAIL,
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            CareDetailScreen(
                carId = carId,
                onBack = { navController.popBackStack() },
                onEditHistory = { historyId ->
                    navController.navigate("$EDIT_MAINTENANCE_HISTORY/$historyId") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 차량 관리 — 차량을 고르면 그 차량을 현재 차량으로 삼고 탭으로 돌아간다.
        composable(Routes.CAR_LIST) {
            CarListScreen(
                onAddCarClick = { navController.navigate(Routes.addCarFirst(false)) },
                onCarClick = { car ->
                    carContextViewModel.selectCar(car.id)
                    navController.popBackStack()
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTICE) {
            NoticeScreen(
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNoticeClick = { navController.navigate(Routes.NOTICE) },
                onExcelExportClick = { navController.navigate(EXCEL_EXPORT) },
                onReportClick = { navController.navigate(Routes.REPORT) { launchSingleTop = true } },
                viewModel = hiltViewModel()
            )
        }

        composable(EXCEL_EXPORT) {
            ExcelExportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // 정비 항목 관리
        composable(
            route = Routes.CAR_DETAIL,
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { backStackEntry ->
            val carId = backStackEntry.arguments!!.getLong("carId")

            CarDetailScreen(
                carId = carId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onAddMaintenanceItem = { id ->
                    navController.navigate("${Routes.CAR_MAINTENANCE_ITEM_PICKER}/$id") {
                        launchSingleTop = true
                    }
                },
                onOpenItemDetail = { settingId ->
                    navController.navigate(Routes.maintenanceItemDetail(settingId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "${Routes.EDIT_CAR}/{carId}",
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            val vm: EditCarViewModel = hiltViewModel()

            EditCarScreen(
                carId = carId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onDeletedGoToList = { navController.navigateToMainRoot() }
            )
        }

        composable(
            route = Routes.ADD_MAINTENANCE_WITH_ARGS,
            arguments = listOf(
                navArgument("carId") { type = NavType.LongType },
                navArgument("settingId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            val preselectedSettingId = entry.arguments?.getLong("settingId")?.takeIf { it > 0L }
            val vm: AddMaintenanceViewModel = hiltViewModel()

            AddMaintenanceScreen(
                carId = carId,
                preselectedSettingId = preselectedSettingId,
                viewModel = vm,
                onGoToItemPicker = {
                    navController.navigate("${Routes.CAR_MAINTENANCE_ITEM_PICKER}/$carId") {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.CAR_MAINTENANCE_ITEM_PICKER}/{carId}",
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            val vm: CarMaintenanceItemPickerViewModel = hiltViewModel()

            CarMaintenanceItemPickerScreen(
                carId = carId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onAddCustomItem = { navController.navigate(Routes.addMaintenanceType(carId)) }
            )
        }

        composable(
            route = ADD_MAINTENANCE_TYPE,
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            val vm: AddMaintenanceTypeViewModel = hiltViewModel()

            AddMaintenanceTypeScreen(
                carId = carId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() } // 저장 후 Picker로 복귀
            )
        }

        // 정비 항목 상세 — 주기 + 교체 내역
        composable(
            route = Routes.MAINTENANCE_ITEM_DETAIL,
            arguments = listOf(navArgument("settingId") { type = NavType.LongType })
        ) { entry ->
            val settingId = entry.arguments!!.getLong("settingId")

            MaintenanceItemDetailScreen(
                settingId = settingId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onEditIntervals = {
                    navController.navigate("$EDIT_MAINTENANCE_SETTING/$settingId") {
                        launchSingleTop = true
                    }
                },
                onAddRecord = { carId ->
                    navController.navigate(Routes.addMaintenance(carId, settingId)) {
                        launchSingleTop = true
                    }
                },
                onEditHistory = { historyId ->
                    navController.navigate("$EDIT_MAINTENANCE_HISTORY/$historyId") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "$EDIT_MAINTENANCE_SETTING/{settingId}",
            arguments = listOf(navArgument("settingId") { type = NavType.LongType })
        ) { entry ->
            val settingId = entry.arguments!!.getLong("settingId")
            val vm: EditMaintenanceSettingViewModel = hiltViewModel()

            EditMaintenanceSettingScreen(
                settingId = settingId,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // 차량 등록 직후 정비 항목 추천 (첫 차량이든 n번째든)
        composable(
            route = Routes.MAINTENANCE_STARTER,
            arguments = listOf(
                navArgument("carId") { type = NavType.LongType },
                navArgument("first") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            val isFirstCar = entry.arguments?.getBoolean("first") ?: false
            MaintenanceStarterScreen(
                carId = carId,
                viewModel = hiltViewModel(),
                onDone = {
                    if (isFirstCar) {
                        // 첫 차량: 등록 화면이 스택에서 빠져 뒤가 없다 → 메인 루트로
                        navController.navigateToMainRoot()
                    } else {
                        // n번째: 차량 추가를 시작했던 화면(메인/차량 관리)으로 복귀
                        navController.popBackStack()
                    }
                }
            )
        }

        // 주유(충전) 기록 입력·수정
        composable(
            route = Routes.FUEL_RECORD,
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("unit") {
                    type = NavType.StringType
                    defaultValue = "L"
                }
            )
        ) { entry ->
            val recordId = entry.arguments?.getLong("recordId")?.takeIf { it > 0L }
            val unitSymbol = entry.arguments?.getString("unit") ?: "L"
            val selectedCar by carContextViewModel.selectedCar.collectAsState()

            FuelRecordEditScreen(
                car = selectedCar,
                recordId = recordId,
                requestedUnit = FuelUnit.fromSymbol(unitSymbol),
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "$EDIT_MAINTENANCE_HISTORY/{historyId}",
            arguments = listOf(navArgument("historyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val historyId = backStackEntry.arguments?.getLong("historyId") ?: return@composable

            MaintenanceHistoryEditScreen(
                historyId = historyId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}


@Composable
fun SplashRoute(
    navController: NavController,
    carContextViewModel: CarContextViewModel,
    initialWidgetCarId: Long?,
    onConsumeInitialWidget: () -> Unit
) {
    val viewModel: MainViewModel = hiltViewModel()
    val startDestination by viewModel.startDestination.collectAsState()

    var handled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(startDestination, initialWidgetCarId) {
        if (handled) return@LaunchedEffect

        val host = navController as? NavHostController ?: return@LaunchedEffect
        val dest = startDestination ?: return@LaunchedEffect

        handled = true  // 여기서 잠금

        // 위젯으로 들어왔으면 그 차량을 현재 차량으로 삼고 탭 셸을 띄운다.
        if (initialWidgetCarId != null && initialWidgetCarId > 0L) {
            carContextViewModel.selectCar(initialWidgetCarId)
            host.navigateToMainRoot()
            onConsumeInitialWidget()
            return@LaunchedEffect
        }

        if (dest == Routes.ADD_CAR) {
            host.navigate(Routes.addCarFirst(true)) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        } else {
            host.navigateToMainRoot()
        }
    }
}

/**
 * 탭 셸을 스택의 루트로 만든다.
 *
 * 시작 지점(SPLASH)이 이미 스택에서 제거된 경우 startDestination 기준 popUpTo 는
 * 아무것도 지우지 못해 화면이 계속 쌓였다(과거 백스택 누적 버그). 그래서 그래프 전체를 비운다.
 */
fun NavHostController.navigateToMainRoot() {
    navigate(Routes.MAIN) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
        restoreState = false
    }
}
