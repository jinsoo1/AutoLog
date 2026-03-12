package com.jsworld.android.autolog.ui.view.navhost

import android.net.http.SslCertificate.saveState
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jsworld.android.autolog.ui.view.Routes
import com.jsworld.android.autolog.ui.view.Routes.ADD_CAR_FIRST
import com.jsworld.android.autolog.ui.view.Routes.ADD_MAINTENANCE_TYPE
import com.jsworld.android.autolog.ui.view.Routes.EDIT_MAINTENANCE_HISTORY
import com.jsworld.android.autolog.ui.view.Routes.EDIT_MAINTENANCE_SETTING
import com.jsworld.android.autolog.ui.view.Routes.ROUTE_HISTORY_LIST
import com.jsworld.android.autolog.ui.view.Routes.historyListRoute
import com.jsworld.android.autolog.ui.view.screen.AddCarScreen
import com.jsworld.android.autolog.ui.view.screen.AddMaintenanceScreen
import com.jsworld.android.autolog.ui.view.screen.AddMaintenanceTypeScreen
import com.jsworld.android.autolog.ui.view.screen.CarDetailScreen
import com.jsworld.android.autolog.ui.view.screen.CarListScreen
import com.jsworld.android.autolog.ui.view.screen.CarMaintenanceItemPickerScreen
import com.jsworld.android.autolog.ui.view.screen.EditCarScreen
import com.jsworld.android.autolog.ui.view.screen.EditMaintenanceSettingScreen
import com.jsworld.android.autolog.ui.view.screen.MaintenanceHistoryEditScreen
import com.jsworld.android.autolog.ui.view.screen.MaintenanceHistoryListScreen
import com.jsworld.android.autolog.ui.view.screen.NoticeScreen
import com.jsworld.android.autolog.ui.view.viewModel.AddMaintenanceTypeViewModel
import com.jsworld.android.autolog.ui.view.viewModel.AddMaintenanceViewModel
import com.jsworld.android.autolog.ui.view.viewModel.CarMaintenanceItemPickerViewModel
import com.jsworld.android.autolog.ui.view.viewModel.EditCarViewModel
import com.jsworld.android.autolog.ui.view.viewModel.EditMaintenanceSettingViewModel
import com.jsworld.android.autolog.ui.view.viewModel.MainViewModel



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

            AddCarScreen(
                onSave = { car ->
                    viewModel.addCar(car)

                    if (isFirst) {
                        // ✅ 최초 진입 → AddCar 스택 제거 후 리스트로
                        navController.navigate(Routes.CAR_LIST) {
                            popUpTo("add_car?first=true") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        // ✅ 일반 추가 → 뒤로
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
                        popUpTo(Routes.CAR_LIST) { inclusive = false } // ✅ list 아래는 건드리지 않음
                    }
                },
                onNoticeClick = {
                    navController.navigate(Routes.NOTICE)
                }
            )
        }

        composable(Routes.NOTICE) {
            NoticeScreen(
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
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
                    navController.navigateToCarListRoot()
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

                // ✅ 히스토리 목록은 "목록 화면"처럼 취급 → 상태 저장/복원 설정 추천
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

        handled = true  // ✅ 여기서 잠금

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
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
        restoreState = false
    }
}

/**
 * ✅ 어떤 진입점(대표차량/위젯/푸시)에서도 "CAR_LIST -> CAR_DETAIL" 구조를 강제
 */
fun NavHostController.openCarDetailAsListChild(carId: Long) {
    val currentRoute = currentBackStackEntry?.destination?.route
    val targetRoute = Routes.carDetail(carId)

    if (currentRoute == targetRoute) return  // ✅ 중복 방지

    navigateToCarListRoot()
    navigate(targetRoute) { launchSingleTop = true }
}