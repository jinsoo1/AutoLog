package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyFuelCost
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val fuelRecordRepository: FuelRecordRepository
) : ViewModel() {

    fun observeRecord(recordId: Long) = fuelRecordRepository.observeById(recordId)

    fun observeRecentStations(carId: Long, unit: FuelUnit) =
        fuelRecordRepository.observeRecentStations(carId, unit)

    suspend fun latestMileage(carId: Long): Int? =
        fuelRecordRepository.getLatestMileage(carId)

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
            onDone()
        }
    }

    fun delete(recordId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            fuelRecordRepository.delete(recordId)
            onDone()
        }
    }
}
