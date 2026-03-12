package com.jsworld.android.autolog.ui.view.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.EditSettingUiState
import com.jsworld.android.autolog.ui.data.room.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.ui.data.room.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.ui.data.room.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.ui.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@HiltViewModel
class EditMaintenanceSettingViewModel @Inject constructor(
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val historyRepository: MaintenanceHistoryRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUi(settingId: Long): Flow<EditSettingUiState> {
        return carMaintenanceRepository.observeSetting(settingId).flatMapLatest { setting ->
            if (setting == null) flowOf(EditSettingUiState(loading = true))
            else {
                combine(
                    maintenanceTypeRepository.observeType(setting.maintenanceTypeId),
                    historyRepository.observeLastHistory(settingId)
                ) { type, history ->
                    EditSettingUiState(
                        loading = false,
                        typeName = type?.name ?: "정비항목",
                        defaultKm = type?.defaultIntervalKm,
                        defaultMonths = type?.defaultIntervalMonths,
                        currentKm = setting.intervalKm,
                        currentMonths = setting.intervalMonths,

                        lastServiceDate = history?.serviceDate,
                        lastServiceMileage = history?.serviceMileage,
                        lastPlace = history?.place,
                        lastCost = history?.cost,
                        lastMemo = history?.memo
                    )
                }
            }
        }
    }

    fun save(settingId: Long, km: Int?, months: Int?, onDone: () -> Unit) {
        viewModelScope.launch {
            carMaintenanceRepository.updateSettingIntervals(settingId, km, months)
            widgetUpdater.requestUpdate()
            onDone()
        }
    }

    fun resetToDefault(settingId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            // ✅ null이면 기본값 사용
            carMaintenanceRepository.updateSettingIntervals(settingId, null, null)
            widgetUpdater.requestUpdate()
            onDone()
        }
    }
}