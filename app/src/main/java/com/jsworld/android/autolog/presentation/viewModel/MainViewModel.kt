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

    val startDestination: StateFlow<String?> =
        combine(
            carRepository.getAllCars(),
            carRepository.getPrimaryCar()
        ) { cars, primary ->

            when {
                cars.isEmpty() -> Routes.ADD_CAR
                primary == null -> Routes.CAR_LIST
                else -> "car_detail/${primary.id}"
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    /** 🚗 차량 추가 */
    fun addCar(car: Car) {
        viewModelScope.launch {
            carRepository.addCar(car)
            widgetUpdater.requestUpdate()
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