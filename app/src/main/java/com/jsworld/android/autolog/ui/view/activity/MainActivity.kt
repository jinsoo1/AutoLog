package com.jsworld.android.autolog.ui.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.glance.appwidget.updateAll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.jsworld.android.autolog.ui.theme.AutoLogTheme
import com.jsworld.android.autolog.ui.view.Routes
import com.jsworld.android.autolog.ui.view.navhost.AutoLogNavHost
import com.jsworld.android.autolog.ui.view.util.Constant.ACTION_OPEN_CAR_DETAIL
import com.jsworld.android.autolog.ui.view.util.Constant.EXTRA_CAR_ID
import com.jsworld.android.autolog.ui.view.viewModel.MainViewModel
import com.jsworld.android.autolog.ui.widget.CarStatusWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val widgetNavRequests = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialWidgetCarId = intent?.takeIf { it.action == ACTION_OPEN_CAR_DETAIL }
            ?.getLongExtra(EXTRA_CAR_ID, -1L)
            ?.takeIf { it > 0L }


        setContent {
            AutoLogTheme(dynamicColor = false) {
                val vm: MainViewModel = hiltViewModel()
                LaunchedEffect(Unit) { vm.ensureDefaults() }

                val navController = rememberNavController()

                // ✅ 앱이 이미 살아있는 상태에서 위젯 클릭 시 이동
                LaunchedEffect(Unit) {
                    widgetNavRequests.collect { carId ->
                        navController.navigate(Routes.carDetail(carId)) {
                            launchSingleTop = true
                            popUpTo(Routes.SPLASH) { inclusive = true } // 스플래시 떠있으면 제거
                        }
                    }
                }

                AutoLogNavHost(
                    navController = navController,
                    initialWidgetCarId = initialWidgetCarId,
                    onConsumeInitialWidget = { /* 필요하면 여기서 처리 */ }
                )
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.action == ACTION_OPEN_CAR_DETAIL) {
            val carId = intent.getLongExtra(EXTRA_CAR_ID, -1L)
            if (carId > 0L) widgetNavRequests.tryEmit(carId)
        }
    }
}