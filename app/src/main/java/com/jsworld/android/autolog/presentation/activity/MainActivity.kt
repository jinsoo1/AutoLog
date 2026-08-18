package com.jsworld.android.autolog.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.jsworld.android.autolog.presentation.widget.WidgetDailyUpdateScheduler
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.jsworld.android.autolog.presentation.theme.AutoLogTheme
import com.jsworld.android.autolog.presentation.navigation.AutoLogNavHost
import com.jsworld.android.autolog.presentation.navigation.navigateToMainRoot
import com.jsworld.android.autolog.core.util.Constant.ACTION_OPEN_CAR_DETAIL
import com.jsworld.android.autolog.core.util.Constant.ACTION_OPEN_REPORT
import com.jsworld.android.autolog.core.util.Constant.EXTRA_CAR_ID
import com.jsworld.android.autolog.presentation.viewModel.CarContextViewModel
import com.jsworld.android.autolog.presentation.viewModel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val widgetNavRequests = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /**
     * 월간 리포트 알림 탭 → 리포트 탭 열기 요청.
     * 소비될 때까지 값이 유지돼야 해서(콜드 스타트에 탭 셸이 늦게 뜬다) 상태로 둔다.
     */
    private val openReportRequests = MutableStateFlow(false)

    /** 웜 스타트에서 다른 화면에 있을 때 탭 셸로 복귀시키는 신호 (onNewIntent 전용) */
    private val navToRootRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialWidgetCarId = intent?.takeIf { it.action == ACTION_OPEN_CAR_DETAIL }
            ?.getLongExtra(EXTRA_CAR_ID, -1L)
            ?.takeIf { it > 0L }

        // savedInstanceState 가 있으면 회전 등 재생성 — 이미 처리한 인텐트를 다시 소비하지 않는다.
        if (savedInstanceState == null && intent?.action == ACTION_OPEN_REPORT) {
            openReportRequests.value = true
        }


        // 위젯 일일 갱신 체인이 끊겼을 수 있으니(강제 종료 등) 앱을 열 때 되살린다.
        lifecycleScope.launch {
            WidgetDailyUpdateScheduler.ensureScheduled(this@MainActivity)
        }

        setContent {
            AutoLogTheme(dynamicColor = false) {
                val vm: MainViewModel = hiltViewModel()
                LaunchedEffect(Unit) { vm.ensureDefaults() }

                // 탭 화면들이 공유하는 "현재 차량". 액티비티 스코프여야 탭을 옮겨도 유지된다.
                val carContextViewModel: CarContextViewModel = hiltViewModel()

                val navController = rememberNavController()

                // 앱이 이미 살아있는 상태에서 위젯 클릭 시: 그 차량으로 전환하고 탭 셸로
                LaunchedEffect(Unit) {
                    widgetNavRequests.collect { carId ->
                        carContextViewModel.selectCar(carId)
                        navController.navigateToMainRoot()
                    }
                }

                LaunchedEffect(Unit) {
                    navToRootRequests.collect { navController.navigateToMainRoot() }
                }

                val openReportRequested by openReportRequests.collectAsState()

                AutoLogNavHost(
                    navController = navController,
                    carContextViewModel = carContextViewModel,
                    initialWidgetCarId = initialWidgetCarId,
                    onConsumeInitialWidget = { /* 필요하면 여기서 처리 */ },
                    openReportRequested = openReportRequested,
                    onConsumeOpenReport = { openReportRequests.value = false }
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

        // 앱이 살아있는 상태에서 리포트 알림 탭 — 어느 화면에 있든 탭 셸로 돌아가
        // 리포트 탭을 연다. 셸 복귀는 위젯과 같은 경로(widgetNavRequests)를 못 쓰므로
        // (차량 전환이 함께 일어난다) 요청 플래그만 세우고 MainTabScreen 이 소비한다.
        if (intent.action == ACTION_OPEN_REPORT) {
            openReportRequests.value = true
            navToRootRequests.tryEmit(Unit)
        }
    }
}
