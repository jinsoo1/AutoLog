package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.MaintenanceStarterPack
import com.jsworld.android.autolog.domain.model.isItemApplicableToFuel
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class MaintenanceStarterViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    fun observeCar(carId: Long): Flow<Car?> = carRepository.getCarById(carId)

    /**
     * 이 차량 연료 타입 기준으로 팩에서 실제로 켜질 항목 이름들.
     * 화면 미리보기용 — 전기차라면 엔진오일이 개수에서 빠져 보인다.
     */
    fun applicableItems(pack: MaintenanceStarterPack, fuelType: String?): List<String> {
        val names = when (pack) {
            MaintenanceStarterPack.LIGHT -> DefaultMaintenanceItems.lightPack
            MaintenanceStarterPack.STANDARD -> DefaultMaintenanceItems.standardPack
            MaintenanceStarterPack.FULL -> DefaultMaintenanceItems.fullPack
        }
        return names.filter { isItemApplicableToFuel(it, fuelType) }
    }

    fun apply(carId: Long, pack: MaintenanceStarterPack, fuelType: String?, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            // 기본 항목 시딩이 아직 안 끝났을 수 있다(앱 최초 실행 직후).
            // 이름으로 타입을 찾으므로 적용 전에 반드시 보장한다.
            maintenanceTypeRepository.ensureDefaultTypes()
            val count = carMaintenanceRepository.applyStarterPack(carId, pack, fuelType)
            widgetUpdater.requestUpdate()
            onDone(count)
        }
    }
}
