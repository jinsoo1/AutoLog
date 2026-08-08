package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.SettingOption
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
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

    /** 항목 선택 시트에서 임박한 항목을 위로 올리고 상태 배지를 보여주기 위해 쓴다. */
    fun observeMaintenanceOverview(carId: Long): Flow<List<MaintenanceUiModel>> =
        carMaintenanceRepository.observeMaintenanceOverview(carId)

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

    /** "현재차량 주행거리 업데이트"를 물어볼지 판단 */
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

    /**
     * 일회성 수리 저장. 수리 이름으로 주기 없는 항목을 찾거나 만들어 기록을 남긴다.
     * 주기가 없으므로 임박 알림·다음 정비에는 나타나지 않는다.
     */
    fun saveRepairWithOptionalMileageUpdate(
        carId: Long,
        repairName: String,
        pending: PendingMaintenanceSave,
        updateCarMileage: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val settingId = carMaintenanceRepository.getOrCreateRepairSetting(carId, repairName)

            maintenanceHistoryRepository.insert(
                settingId = settingId,
                serviceDate = pending.serviceDate,
                serviceMileage = pending.serviceMileage,
                place = pending.place,
                cost = pending.cost,
                memo = pending.memo
            )

            if (updateCarMileage) {
                carRepository.updateMileage(carId, pending.serviceMileage)
            }
            widgetUpdater.requestUpdate()
            onDone()
        }
    }

    /** 저장 + (선택) 차량 주행거리 업데이트 */
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