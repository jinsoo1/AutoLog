package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val expenseReportRepository: ExpenseReportRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val maintenanceHistoryRepository: MaintenanceHistoryRepository
) : ViewModel() {

    // null = 로딩 중, emptyList = 기록 없음 — 빈 상태 화면이 로딩 중에 깜빡이지 않게 구분한다.
    private val expensesMap = mutableMapOf<Long, StateFlow<List<MonthlyExpense>?>>()
    private val fuelMap = mutableMapOf<Long, StateFlow<List<FuelRecord>>>()
    private val maintMap = mutableMapOf<Long, StateFlow<List<CarMaintenanceRecord>>>()

    fun expensesState(carId: Long): StateFlow<List<MonthlyExpense>?> =
        expensesMap.getOrPut(carId) {
            expenseReportRepository.observeMonthlyExpenses(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    /** 지출 내역 리스트용 — 선택한 달의 실제 기록을 보여주기 위해 원본을 그대로 든다 */
    fun fuelRecordsState(carId: Long): StateFlow<List<FuelRecord>> =
        fuelMap.getOrPut(carId) {
            fuelRecordRepository.observeByCar(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun maintenanceRecordsState(carId: Long): StateFlow<List<CarMaintenanceRecord>> =
        maintMap.getOrPut(carId) {
            maintenanceHistoryRepository.observeCarRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
}
