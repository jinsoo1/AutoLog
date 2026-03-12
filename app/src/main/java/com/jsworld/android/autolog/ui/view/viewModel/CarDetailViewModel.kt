package com.jsworld.android.autolog.ui.view.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.CarMaintenanceSetting
import com.jsworld.android.autolog.ui.data.item.MaintenanceSort
import com.jsworld.android.autolog.ui.data.item.MaintenanceType
import com.jsworld.android.autolog.ui.data.item.MaintenanceUiModel
import com.jsworld.android.autolog.ui.data.item.SettingOption
import com.jsworld.android.autolog.ui.data.room.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import com.jsworld.android.autolog.ui.data.room.repository.CarSortPreferenceRepository
import com.jsworld.android.autolog.ui.data.room.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.ui.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@HiltViewModel
class CarDetailViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val carSortPrefRepository: CarSortPreferenceRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    // ✅ carId별 캐시
    private val carFlowMap = mutableMapOf<Long, StateFlow<Car?>>()
    private val statusFlowMap = mutableMapOf<Long, StateFlow<List<MaintenanceUiModel>>>()
    private val sortFlowMap = mutableMapOf<Long, StateFlow<MaintenanceSort>>()
    private val settingsFlowMap = mutableMapOf<Long, StateFlow<List<CarMaintenanceSetting>>>()
    private val optionsFlowMap = mutableMapOf<Long, StateFlow<List<SettingOption>>>() // 타입은 실제 리턴 타입으로
    private val typesFlow: StateFlow<List<MaintenanceType>> by lazy {
        maintenanceTypeRepository.observeAllTypes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun carState(carId: Long): StateFlow<Car?> =
        carFlowMap.getOrPut(carId) {
            carRepository.getCarById(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    fun maintenanceStatusState(carId: Long): StateFlow<List<MaintenanceUiModel>> =
        statusFlowMap.getOrPut(carId) {
            carMaintenanceRepository.observeMaintenanceStatusList(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun sortState(carId: Long): StateFlow<MaintenanceSort> =
        sortFlowMap.getOrPut(carId) {
            carSortPrefRepository.observeSort(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaintenanceSort.DEFAULT)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun sortedSettingsState(carId: Long): StateFlow<List<CarMaintenanceSetting>> =
        settingsFlowMap.getOrPut(carId) {
            sortState(carId)
                .flatMapLatest { sort ->
                    carMaintenanceRepository.getMaintenanceSettingsSorted(carId, sort)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun maintenanceTypesState(): StateFlow<List<MaintenanceType>> = typesFlow

    fun settingOptionsState(carId: Long): StateFlow<List<SettingOption>> =
        optionsFlowMap.getOrPut(carId) {
            carMaintenanceRepository.observeSettingOptions(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun setSort(carId: Long, sort: MaintenanceSort) {
        viewModelScope.launch { carSortPrefRepository.setSort(carId, sort) }
    }

    fun updateCarMileage(carId: Long, newMileage: Int) {
        viewModelScope.launch {
            if (newMileage < 0) return@launch
            carMaintenanceRepository.updateCarMileage(carId, newMileage)
            widgetUpdater.requestUpdate()
        }
    }
}
