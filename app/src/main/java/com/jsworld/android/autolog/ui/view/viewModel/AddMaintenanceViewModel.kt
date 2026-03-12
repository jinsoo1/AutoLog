package com.jsworld.android.autolog.ui.view.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.SettingOption
import com.jsworld.android.autolog.ui.data.room.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import com.jsworld.android.autolog.ui.data.room.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.ui.data.room.repository.UserPrefsRepository
import com.jsworld.android.autolog.ui.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@HiltViewModel
class AddMaintenanceViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val maintenanceHistoryRepository: MaintenanceHistoryRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    fun getCar(carId: Long): Flow<Car?> =
        carRepository.getCarById(carId)

    fun observeSettingOptions(carId: Long): Flow<List<SettingOption>> =
        carMaintenanceRepository.observeSettingOptions(carId)

    fun save(
        settingId: Long,
        serviceDate: String,
        serviceMileage: Int,
        place: String?,
        cost: Int?,
        memo: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            carMaintenanceRepository.insertHistory(
                settingId = settingId,
                serviceDate = serviceDate,
                serviceMileage = serviceMileage,
                place = place,
                cost = cost,
                memo = memo
            )
            onDone()
        }
    }

    /** ✅ "현재차량 주행거리 업데이트"를 물어볼지 판단 */
    suspend fun checkMileageUpdateSuggestion(
        carId: Long,
        newMileage: Int
    ): UpdateMileageDecision? {
        val car = carRepository.getCarById(carId).firstOrNull() ?: return null
        val current = car.mileage

        // 현재보다 크지 않으면 물어볼 필요 없음
        if (newMileage <= current) return UpdateMileageDecision(
            shouldAsk = false,
            currentCarMileage = current,
            maxHistoryMileage = null,
            newMileage = newMileage
        )

        val maxHistory = maintenanceHistoryRepository.getMaxMileageForCar(carId)
        // "정비기록 중 가장 높다면" → 기존 최대값 이상이면 (공동 1등 포함)
        val isMaxOrTied = (maxHistory == null) || (newMileage >= maxHistory)

        return UpdateMileageDecision(
            shouldAsk = isMaxOrTied,
            currentCarMileage = current,
            maxHistoryMileage = maxHistory,
            newMileage = newMileage
        )
    }

    /** ✅ 저장 + (선택) 차량 주행거리 업데이트 */
    fun saveWithOptionalMileageUpdate(
        carId: Long,
        pending: PendingMaintenanceSave,
        updateCarMileage: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            // 1) 정비 기록 저장(기존 save 로직 그대로)
            maintenanceHistoryRepository.insert(
                settingId = pending.settingId,
                serviceDate = pending.serviceDate,
                serviceMileage = pending.serviceMileage,
                place = pending.place,
                cost = pending.cost,
                memo = pending.memo
            )

            // 2) (선택) 차량 주행거리 갱신
            if (updateCarMileage) {
                carRepository.updateMileage(carId, pending.serviceMileage)
            }
            widgetUpdater.requestUpdate()
            onDone()
        }
    }

    fun observeAutoMileageUpdate(carId: Long) =
        userPrefsRepository.observeAutoMileageUpdate(carId)

    fun setAutoMileageUpdate(carId: Long, enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setAutoMileageUpdate(carId, enabled)
        }
    }
}

data class UpdateMileageDecision(
    val shouldAsk: Boolean,
    val currentCarMileage: Int,
    val maxHistoryMileage: Int?,
    val newMileage: Int
)

data class PendingMaintenanceSave(
    val settingId: Long,
    val serviceDate: String,
    val serviceMileage: Int,
    val place: String?,
    val cost: Int?,
    val memo: String?
)