package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CareRepository
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val historyRepository: MaintenanceHistoryRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val careRepository: CareRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    /** '이번 달 지출' 카드용 세차 기록 — 세차는 별도 테이블이라 따로 가져온다 */
    fun careRecordsState(carId: Long): StateFlow<List<CareRecord>> =
        careRecordsMap.getOrPut(carId) {
            careRepository.observeRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    private val careRecordsMap = mutableMapOf<Long, StateFlow<List<CareRecord>>>()

    // 차량을 전환해도 이미 만든 Flow 를 재사용한다(CarDetailViewModel 과 같은 방식).
    private val overviewMap = mutableMapOf<Long, StateFlow<List<MaintenanceUiModel>>>()
    private val recordsMap = mutableMapOf<Long, StateFlow<List<CarMaintenanceRecord>>>()
    private val maxServiceMileageMap = mutableMapOf<Long, StateFlow<Int?>>()
    private val fuelMap = mutableMapOf<Long, StateFlow<List<FuelRecord>>>()

    fun fuelRecordsState(carId: Long): StateFlow<List<FuelRecord>> =
        fuelMap.getOrPut(carId) {
            fuelRecordRepository.observeByCar(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun overviewState(carId: Long): StateFlow<List<MaintenanceUiModel>> =
        overviewMap.getOrPut(carId) {
            carMaintenanceRepository.observeMaintenanceOverview(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun recordsState(carId: Long): StateFlow<List<CarMaintenanceRecord>> =
        recordsMap.getOrPut(carId) {
            historyRepository.observeCarRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /** 주행거리를 이 값보다 낮게 내리면 기존 정비 기록과 모순이 생긴다. */
    fun maxServiceMileageState(carId: Long): StateFlow<Int?> =
        maxServiceMileageMap.getOrPut(carId) {
            historyRepository.observeMaxServiceMileageForCar(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    fun updateCarMileage(carId: Long, newMileage: Int) {
        viewModelScope.launch {
            if (newMileage < 0) return@launch
            carMaintenanceRepository.updateCarMileage(carId, newMileage)
            widgetUpdater.requestUpdate()
        }
    }
}
