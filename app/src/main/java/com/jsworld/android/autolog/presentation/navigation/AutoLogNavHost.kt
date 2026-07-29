package com.jsworld.android.autolog.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExitTransition
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
import com.jsworld.android.autolog.presentation.navigation.Routes
import com.jsworld.android.autolog.presentation.navigation.Routes.ADD_CAR_FIRST
import com.jsworld.android.autolog.presentation.navigation.Routes.ADD_MAINTENANCE_TYPE
import com.jsworld.android.autolog.presentation.navigation.Routes.EDIT_MAINTENANCE_HISTORY
import com.jsworld.android.autolog.presentation.navigation.Routes.EDIT_MAINTENANCE_SETTING
import com.jsworld.android.autolog.presentation.navigation.Routes.EXCEL_EXPORT
import com.jsworld.android.autolog.presentation.navigation.Routes.ROUTE_HISTORY_LIST
import com.jsworld.android.autolog.presentation.navigation.Routes.historyListRoute
import com.jsworld.android.autolog.presentation.screen.AddCarScreen
import com.jsworld.android.autolog.presentation.screen.AddMaintenanceScreen
import com.jsworld.android.autolog.presentation.screen.AddMaintenanceTypeScreen
import com.jsworld.android.autolog.presentation.screen.CarDetailScreen
import com.jsworld.android.autolog.presentation.screen.CarListScreen
import com.jsworld.android.autolog.presentation.screen.CarMaintenanceItemPickerScreen
import com.jsworld.android.autolog.presentation.screen.EditCarScreen
import com.jsworld.android.autolog.presentation.screen.EditMaintenanceSettingScreen
import com.jsworld.android.autolog.presentation.screen.ExcelExportScreen
import com.jsworld.android.autolog.presentation.screen.MaintenanceHistoryEditScreen
import com.jsworld.android.autolog.presentation.screen.MaintenanceHistoryListScreen
import com.jsworld.android.autolog.presentation.screen.NoticeScreen
import com.jsworld.android.autolog.presentation.screen.SettingsScreen
import com.jsworld.android.autolog.presentation.viewModel.AddMaintenanceTypeViewModel
import com.jsworld.android.autolog.presentation.viewModel.AddMaintenanceViewModel
import com.jsworld.android.autolog.presentation.viewModel.CarMaintenanceItemPickerViewModel
import com.jsworld.android.autolog.presentation.viewModel.EditCarViewModel
import com.jsworld.android.autolog.presentation.viewModel.EditMaintenanceSettingViewModel
import com.jsworld.android.autolog.presentation.viewModel.MainViewModel



private const val ENTER = 260
private const val EXIT = 220

@Composable
fun AutoLogNavHost(
    navController: NavHostController,
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
                        navController.navigate(Routes.CAR_LIST) {
                            popUpTo("add_car?first=true") { inclusive = true }
                            launchSingleTop = true
                        }
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
                    viewModel.addCar(car)

                    if (isFirst) {
                        // 최초 진입 → AddCar 스택 제거 후 리스트로
                        navController.navigate(Routes.CAR_LIST) {
                            popUpTo("add_car?first=true") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        // 일반 추가 → 뒤로
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Routes.CAR_LIST) {
            CarListScreen(
                onAddCarClick = { navController.navigate(Routes.addCarFirst(false)) },
                onCarClick = { car ->
                    navController.navigate(Routes.carDetail(car.id)) {
                        launchSingleTop = true
                        popUpTo(Routes.CAR_LIST) { inclusive = false } // list 아래는 건드리지 않음
                    }
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
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
                viewModel = hiltViewModel()
            )
        }

        composable(EXCEL_EXPORT) {
            ExcelExportScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.CAR_DETAIL,
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { backStackEntry ->
            val carId = backStackEntry.arguments!!.getLong("carId")

            CarDetailScreen(
                carId = carId,
                viewModel = hiltViewModel(),
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.CAR_LIST) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },

                onGoToList = {
                    // 스택에 이미 있는 리스트로 되돌아간다(새 인스턴스를 쌓지 않음).
                    // 리스트가 스택에 없는 진입 경로(스플래시→대표차량 상세 등)만 리셋 이동.
                    if (!navController.popBackStack(Routes.CAR_LIST, inclusive = false)) {
                        navController.navigateToCarListRoot()
                    }
                },

                onEditCar = { id ->
                    navController.navigate("${Routes.EDIT_CAR}/$id") { launchSingleTop = true }
                },
                onAddMaintenance = { id ->
                    navController.navigate("${Routes.ADD_MAINTENANCE}/$id") { launchSingleTop = true }
                },
                onAddMaintenanceItem = { id ->
                    navController.navigate("${Routes.CAR_MAINTENANCE_ITEM_PICKER}/$id") { launchSingleTop = true }
                },
                onEditMaintenanceSetting = { settingId ->
                    navController.navigate("$EDIT_MAINTENANCE_SETTING/$settingId") { launchSingleTop = true }
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
                onDeletedGoToList = {
                    navController.navigate(Routes.CAR_LIST) {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }

        composable(
            route = "${Routes.ADD_MAINTENANCE}/{carId}",
            arguments = listOf(navArgument("carId") { type = NavType.LongType })
        ) { entry ->
            val carId = entry.arguments!!.getLong("carId")
            val vm: AddMaintenanceViewModel = hiltViewModel()

            AddMaintenanceScreen(
                carId = carId,
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

        composable(
            route = "$EDIT_MAINTENANCE_SETTING/{settingId}",
            arguments = listOf(navArgument("settingId") { type = NavType.LongType })
        ) { entry ->
            val settingId = entry.arguments!!.getLong("settingId")
            val vm: EditMaintenanceSettingViewModel = hiltViewModel()

            EditMaintenanceSettingScreen(
                settingId = settingId,
                viewModel = vm,

                // 히스토리 목록은 "목록 화면"처럼 취급 → 상태 저장/복원 설정 추천
                onViewAllHistory = { id ->
                    navController.navigate(historyListRoute(id)) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "$ROUTE_HISTORY_LIST/{settingId}",
            arguments = listOf(navArgument("settingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("settingId") ?: return@composable

            MaintenanceHistoryListScreen(
                settingId = id,
                viewModel = hiltViewModel(),
                onEdit = { historyId ->
                    navController.navigate("${Routes.EDIT_MAINTENANCE_HISTORY}/$historyId") {
                        launchSingleTop = true
                    }
                },
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

        if (initialWidgetCarId != null && initialWidgetCarId > 0L) {
            host.openCarDetailAsListChild(initialWidgetCarId)
            onConsumeInitialWidget()
            return@LaunchedEffect
        }

        when {
            dest == Routes.ADD_CAR -> {
                host.navigate("add_car?first=true") {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            dest.startsWith("car_detail/") -> {
                val carId = dest.substringAfter("car_detail/").toLongOrNull()
                if (carId != null) host.openCarDetailAsListChild(carId) else host.navigateToCarListRoot()
            }
            else -> {
                host.navigate(dest) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}


fun NavHostController.navigateToCarListRoot() {
    navigate(Routes.CAR_LIST) {
        // 시작 지점(SPLASH)은 이미 스택에서 제거된 상태라 startDestination 기준 popUpTo는
        // 아무것도 지우지 못한다(스택 누적 버그). 그래프 전체를 비우고 리스트를 루트로 만든다.
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
        restoreState = false
    }
}

/**
 * 어떤 진입점(대표차량/위젯/푸시)에서도 "CAR_LIST -> CAR_DETAIL" 구조를 강제
 */
fun NavHostController.openCarDetailAsListChild(carId: Long) {
    val currentRoute = currentBackStackEntry?.destination?.route
    val targetRoute = Routes.carDetail(carId)

    if (currentRoute == targetRoute) return  // 중복 방지

    navigateToCarListRoot()
    navigate(targetRoute) { launchSingleTop = true }
}