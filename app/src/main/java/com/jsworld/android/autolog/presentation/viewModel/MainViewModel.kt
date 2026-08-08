package com.jsworld.android.autolog.presentation.viewModel

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.data.repository.BackupRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.presentation.navigation.Routes
import com.jsworld.android.autolog.presentation.widget.CarStatusWidget
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val carRepository: CarRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val backupRepository: BackupRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    init {
        viewModelScope.launch {
            widgetUpdater.requestUpdate()
        }
    }

    /**
     * 차량이 하나도 없으면 온보딩, 있으면 탭 셸.
     * 어느 차량을 볼지는 [CarContextViewModel] 이 결정하므로 여기서 다루지 않는다.
     */
    val startDestination: StateFlow<String?> =
        carRepository.getAllCars()
            .map { cars -> if (cars.isEmpty()) Routes.ADD_CAR else Routes.MAIN }
            .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    /** 🚗 차량 추가. 저장이 끝나면 새 carId 를 돌려준다(온보딩 추천 화면 진입용). */
    fun addCar(car: Car, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val carId = carRepository.addCar(car)
            widgetUpdater.requestUpdate()
            onSaved(carId)
        }
    }

    /** 🚗 차량 목록 */
    val cars: StateFlow<List<Car>> =
        carRepository.getAllCars()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /** 온보딩(첫 차량) 화면에서 SAF로 고른 백업 파일로 복원 */
    fun restoreBackup(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            backupRepository.restoreBackup(uri)
                .onSuccess {
                    widgetUpdater.requestUpdate()
                    onResult(true, null)
                }
                .onFailure { onResult(false, it.message) }
        }
    }

    suspend fun ensureDefaults() {
        maintenanceTypeRepository.ensureDefaultTypes()
    }
}