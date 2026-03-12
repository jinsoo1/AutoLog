package com.jsworld.android.autolog.ui.view.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import com.jsworld.android.autolog.ui.data.room.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.ui.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class EditCarViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val historyRepository: MaintenanceHistoryRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    fun observeCar(carId: Long): Flow<Car?> = carRepository.getCarById(carId)

    fun observeMaxHistoryMileage(carId: Long): Flow<Int?> =
        historyRepository.observeMaxServiceMileageForCar(carId)

    fun save(car: Car, onDone: () -> Unit) {
        viewModelScope.launch {
            carRepository.updateCar(car)
            widgetUpdater.requestUpdate()
            onDone()
        }
    }

    fun deleteCar(car: Car, onDone: () -> Unit) {
        viewModelScope.launch {
            carRepository.deleteCar(car)
            widgetUpdater.requestUpdate() // ✅ 위젯 갱신(차량 삭제 반영)
            onDone()
        }
    }
}