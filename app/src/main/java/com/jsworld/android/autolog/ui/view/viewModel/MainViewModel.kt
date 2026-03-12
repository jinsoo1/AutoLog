package com.jsworld.android.autolog.ui.view.viewModel

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import com.jsworld.android.autolog.ui.data.room.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.ui.view.Routes
import com.jsworld.android.autolog.ui.widget.CarStatusWidget
import com.jsworld.android.autolog.ui.widget.WidgetUpdater
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

    suspend fun ensureDefaults() {
        maintenanceTypeRepository.ensureDefaultTypes()
    }
}