package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CareRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MaintenanceTabViewModel @Inject constructor(
    private val historyRepository: MaintenanceHistoryRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val careRepository: CareRepository
) : ViewModel() {

    private val recordsMap = mutableMapOf<Long, StateFlow<List<CarMaintenanceRecord>>>()
    private val noHistoryItemsMap = mutableMapOf<Long, StateFlow<List<MaintenanceUiModel>>>()

    fun recordsState(carId: Long): StateFlow<List<CarMaintenanceRecord>> =
        recordsMap.getOrPut(carId) {
            historyRepository.observeCarRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /** 세차 항목이 하나라도 켜져 있는지 — 세차 카드 노출 조건 */
    fun careEnabledState(carId: Long): StateFlow<Boolean> =
        careEnabledMap.getOrPut(carId) {
            careRepository.observeItems(carId)
                .map { items -> items.any { it.enabled } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
        }

    /** 세차 카드 내용(경과일·횟수)용 세차 기록 */
    fun careRecordsState(carId: Long): StateFlow<List<CareRecord>> =
        careRecordsMap.getOrPut(carId) {
            careRepository.observeRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    private val careEnabledMap = mutableMapOf<Long, StateFlow<Boolean>>()
    private val careRecordsMap = mutableMapOf<Long, StateFlow<List<CareRecord>>>()

    /**
     * 주기는 있는데 기록이 하나도 없는 항목들 — 상단 배너와 바텀시트용.
     * 이런 항목은 0km/오늘 기준 계산이라 홈·리포트·알림의 임박/초과에서
     * 제외되므로, 여기서 첫 기록 입력을 안내한다.
     */
    fun noHistoryItemsState(carId: Long): StateFlow<List<MaintenanceUiModel>> =
        noHistoryItemsMap.getOrPut(carId) {
            carMaintenanceRepository.observeMaintenanceOverview(carId)
                .map { list -> list.filter { !it.hasHistory }.sortedBy { it.name } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
}
