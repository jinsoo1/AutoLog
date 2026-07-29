package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.MaintenanceType
import com.jsworld.android.autolog.domain.model.MaintenanceTypePickUi
import com.jsworld.android.autolog.presentation.model.PickerItemUi
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.collections.filter

@HiltViewModel
class CarMaintenanceItemPickerViewModel @Inject constructor(
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val carMaintenanceSettingRepository: CarMaintenanceRepository,
    private val carRepository: CarRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    /** 차량 연료 타입(연료별 항목 필터링용) */
    fun observeCarFuelType(carId: Long): Flow<String?> =
        carRepository.getCarById(carId).map { it?.fuelType }

    private val _events = MutableSharedFlow<PickerUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PickerUiEvent> = _events.asSharedFlow()

    private fun observeAllItems(carId: Long): Flow<List<PickerItemUi>> =
        combine(
            maintenanceTypeRepository.observeAllTypes(),
            carMaintenanceSettingRepository.observeAllByCarId(carId)
        ) { types, settings ->
            val settingMap = settings.associateBy { it.maintenanceTypeId }
            types.map { type ->
                val s = settingMap[type.id]
                PickerItemUi(
                    typeId = type.id,
                    typeName = type.name,
                    defaultKm = type.defaultIntervalKm,
                    defaultMonths = type.defaultIntervalMonths,
                    settingId = s?.id,
                    isActive = s?.isActive ?: false,
                    intervalKm = s?.intervalKm,
                    intervalMonths = s?.intervalMonths
                )
            }
        }

    fun observeManagingItems(carId: Long): Flow<List<PickerItemUi>> =
        observeAllItems(carId)
            .map { list -> list.filter { it.settingId != null && it.isActive } }
            .distinctUntilChanged()

    fun observeRestoreItems(carId: Long): Flow<List<PickerItemUi>> =
        observeAllItems(carId)
            .map { list -> list.filter { it.settingId != null && !it.isActive } }
            .distinctUntilChanged()

    fun observeAddableItems(carId: Long): Flow<List<PickerItemUi>> =
        observeAllItems(carId)
            .map { list -> list.filter { it.settingId == null } }
            .distinctUntilChanged()

    /**
     * 체크 변경:
     * - 체크 해제 => disable + 스낵바("정비 내역은 유지됩니다")
     * - 체크 true  => (있으면 enable, 없으면 insert)
     */
    fun setChecked(carId: Long, typeId: Long, checked: Boolean) {
        viewModelScope.launch {
            val existing = carMaintenanceSettingRepository.getByCarIdAndTypeIdOnce(carId, typeId)

            if (checked) {
                when {
                    existing == null -> {
                        carMaintenanceSettingRepository.insertDefaultActive(carId, typeId)
                        _events.tryEmit(PickerUiEvent.Snackbar("관리 항목에 추가했어요."))
                    }
                    existing.isActive.not() -> {
                        carMaintenanceSettingRepository.setActive(existing.id, true)
                        _events.tryEmit(PickerUiEvent.Snackbar("항목을 복구했어요."))
                    }
                }
            } else {
                if (existing != null && existing.isActive) {
                    carMaintenanceSettingRepository.setActive(existing.id, false)
                    _events.tryEmit(PickerUiEvent.Snackbar("정비 내역은 유지됩니다."))
                }
            }
            widgetUpdater.requestUpdate()
        }
    }


}

sealed interface PickerUiEvent {
    data class Snackbar(val message: String) : PickerUiEvent
}