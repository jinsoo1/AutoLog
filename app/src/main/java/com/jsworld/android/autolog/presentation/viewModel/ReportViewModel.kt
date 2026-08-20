package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.SettingLastCost
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarScheduleRepository
import com.jsworld.android.autolog.domain.repository.CareRepository
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val expenseReportRepository: ExpenseReportRepository,
    private val fuelRecordRepository: FuelRecordRepository,
    private val maintenanceHistoryRepository: MaintenanceHistoryRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val careRepository: CareRepository,
    private val scheduleRepository: CarScheduleRepository,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    /**
     * 월간 리포트 알림 권한을 물어야 하는가 — 리포트를 실제로 볼 만한 사용자에게만.
     * (기본 켜짐인데 권한이 없으면 알림이 조용히 실패한다)
     */
    suspend fun shouldAskReportNotificationPermission(): Boolean {
        val enabled = userPrefsRepository.observeMonthlyReportNotificationEnabled().first()
        val asked = userPrefsRepository.observeMonthlyReportPermissionAsked().first()
        return enabled && !asked
    }

    fun markReportNotificationPermissionAsked() {
        viewModelScope.launch { userPrefsRepository.setMonthlyReportPermissionAsked() }
    }

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

    /** 정기검사·보험·자동차세 — '곧 있을 일' 카드에 정비 항목과 함께 오른다 */
    fun schedulesState(carId: Long): StateFlow<List<CarSchedule>> =
        schedulesMap.getOrPut(carId) {
            scheduleRepository.observeByCar(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    private val schedulesMap = mutableMapOf<Long, StateFlow<List<CarSchedule>>>()

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
