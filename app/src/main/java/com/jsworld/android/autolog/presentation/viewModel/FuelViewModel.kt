package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyFuelCost
import com.jsworld.android.autolog.domain.model.suggestBackdatedMileage
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FuelViewModel @Inject constructor(
    private val fuelRecordRepository: FuelRecordRepository
) : ViewModel() {

    private val recordsMap = mutableMapOf<Long, StateFlow<List<FuelRecord>>>()
    private val monthlyMap = mutableMapOf<Long, StateFlow<List<MonthlyFuelCost>>>()

    fun recordsState(carId: Long): StateFlow<List<FuelRecord>> =
        recordsMap.getOrPut(carId) {
            fuelRecordRepository.observeByCar(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun monthlyCostState(carId: Long): StateFlow<List<MonthlyFuelCost>> =
        monthlyMap.getOrPut(carId) {
            fuelRecordRepository.observeMonthlyCost(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun delete(recordId: Long) {
        viewModelScope.launch { fuelRecordRepository.delete(recordId) }
    }
}

/**
 * 주유 기록 입력/수정 화면용. 화면이 하나이므로 ViewModel 도 하나로 둔다.
 */
@HiltViewModel
class FuelRecordEditViewModel @Inject constructor(
    private val fuelRecordRepository: FuelRecordRepository,
    private val carRepository: CarRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    fun observeRecord(recordId: Long) = fuelRecordRepository.observeById(recordId)

    fun observeRecentStations(carId: Long, unit: FuelUnit) =
        fuelRecordRepository.observeRecentStations(carId, unit)

    suspend fun latestMileage(carId: Long): Int? =
        fuelRecordRepository.getLatestMileage(carId)

    /** 과거 날짜 기록의 주행거리 제안 — 그 날짜 앞뒤 기록 사이의 값 */
    suspend fun suggestMileageFor(carId: Long, date: String): Int? {
        val (prev, next) = fuelRecordRepository.getMileageAround(carId, date)
        return suggestBackdatedMileage(prev, next)
    }

    fun save(
        recordId: Long?,
        carId: Long,
        filledAt: String,
        mileage: Int?,
        amount: Int?,
        quantity: Double?,
        unitPrice: Int?,
        unit: FuelUnit,
        station: String?,
        memo: String?,
        photoPath: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            if (recordId == null) {
                fuelRecordRepository.insert(
                    carId = carId,
                    filledAt = filledAt,
                    mileage = mileage,
                    amount = amount,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    unit = unit,
                    station = station,
                    memo = memo,
                    photoPath = photoPath
                )
            } else {
                fuelRecordRepository.update(
                    FuelRecord(
                        id = recordId,
                        carId = carId,
                        filledAt = filledAt,
                        mileage = mileage,
                        amount = amount,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        unit = unit,
                        station = station?.trim()?.takeIf { it.isNotBlank() },
                        memo = memo?.trim()?.takeIf { it.isNotBlank() },
                        photoPath = photoPath
                    )
                )
            }
            maybeUpdateCarMileage(carId, mileage)
            onDone()
        }
    }

    /**
     * 주유 시 적은 주행거리가 차량 누적 주행거리보다 크면 차량 값도 따라 올린다.
     * 더 작을 때는 건드리지 않는다 — 과거 날짜의 기록을 뒤늦게 넣는 경우가 있어서다.
     */
    private suspend fun maybeUpdateCarMileage(carId: Long, mileage: Int?) {
        if (mileage == null) return
        val car = carRepository.getCarById(carId).first() ?: return
        if (mileage > car.mileage) {
            carRepository.updateMileage(carId, mileage)
            // 주행거리가 바뀌면 정비 임박 계산이 달라지므로 위젯도 갱신한다.
            widgetUpdater.requestUpdate()
        }
    }

    fun delete(recordId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            fuelRecordRepository.delete(recordId)
            onDone()
        }
    }
}
