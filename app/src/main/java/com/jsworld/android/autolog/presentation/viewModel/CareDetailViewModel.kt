package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.isCareItemName
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CareDetailViewModel @Inject constructor(
    private val historyRepository: MaintenanceHistoryRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val recordsMap = mutableMapOf<Long, StateFlow<List<CarMaintenanceRecord>>>()
    private val namesMap = mutableMapOf<Long, StateFlow<List<String>>>()

    /** 이 차량의 세차·관리 기록 전부(최신순) */
    fun careRecordsState(carId: Long): StateFlow<List<CarMaintenanceRecord>> =
        recordsMap.getOrPut(carId) {
            historyRepository.observeCarRecords(carId)
                .map { list -> list.filter { isCareItemName(it.typeName) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /**
     * 기록 시트의 "무엇을" 칩 — 켜져 있는 세차 계열 항목 + 기록에 있던 이름 + 기본 '세차'.
     * 없는 항목을 고르면 저장 시 이름으로 찾거나 만들어 쓴다(수리와 같은 방식).
     */
    fun careNamesState(carId: Long): StateFlow<List<String>> =
        namesMap.getOrPut(carId) {
            carMaintenanceRepository.observeSettingOptions(carId)
                .map { options ->
                    val active = options.map { it.typeName }.filter { isCareItemName(it) }
                    (listOf("세차") + active).distinct()
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("세차"))
        }

    fun save(
        carId: Long,
        itemName: String,
        serviceDate: String,
        cost: Int?,
        place: String?,
        memo: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            // 이름으로 항목을 찾거나(켜져 있으면 재사용) 주기 없는 항목으로 만든다.
            val settingId = carMaintenanceRepository.getOrCreateRepairSetting(carId, itemName)
            carMaintenanceRepository.insertHistory(
                settingId = settingId,
                serviceDate = serviceDate,
                serviceMileage = null,
                place = place?.trim()?.takeIf { it.isNotBlank() },
                cost = cost,
                memo = memo?.trim()?.takeIf { it.isNotBlank() }
            )
            widgetUpdater.requestUpdate()
            onDone()
        }
    }
}
