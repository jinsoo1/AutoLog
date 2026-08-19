package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.SettingLastCost
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CareRepository
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val expenseReportRepository: ExpenseReportRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val maintenanceHistoryRepository: MaintenanceHistoryRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val careRepository: CareRepository
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

    /** 지출 내역·연간 통계용 세차 기록 (별도 테이블) */
    fun careRecordsState(carId: Long): StateFlow<List<CareRecord>> =
        careMap.getOrPut(carId) {
            careRepository.observeRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    private val careMap = mutableMapOf<Long, StateFlow<List<CareRecord>>>()

    /**
     * 정비 시기 예측용 — 정상 항목까지 포함한 전체 상태.
     * (urgentState 는 임박·초과만 와서 "아직 여유 있는 항목이 언제쯤 올지"를 못 만든다)
     */
    fun overviewState(carId: Long): StateFlow<List<MaintenanceUiModel>> =
        overviewMap.getOrPut(carId) {
            carMaintenanceRepository.observeMaintenanceOverview(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    private val overviewMap = mutableMapOf<Long, StateFlow<List<MaintenanceUiModel>>>()

    /** 다가오는 지출 카드용 — 임박·초과 항목 */
    fun urgentState(carId: Long): StateFlow<List<MaintenanceUiModel>> =
        urgentMap.getOrPut(carId) {
            carMaintenanceRepository.observeMaintenanceStatusList(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /** 항목별 마지막 교체 비용 — "다음도 이 정도" 예상용 */
    fun lastCostsState(carId: Long): StateFlow<Map<Long, Int?>> =
        lastCostMap.getOrPut(carId) {
            maintenanceHistoryRepository.observeLastCostBySetting(carId)
                .map { list -> list.associate { it.settingId to it.cost } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        }

    private val urgentMap = mutableMapOf<Long, StateFlow<List<MaintenanceUiModel>>>()
    private val lastCostMap = mutableMapOf<Long, StateFlow<Map<Long, Int?>>>()
}
