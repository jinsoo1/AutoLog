package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.presentation.state.MaintenanceItemDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * 정비 항목 상세 — 주기 설정과 교체 내역을 한 화면에서 보여주기 위한 상태를 만든다.
 * (예전에는 "항목 수정" 화면과 "내역 목록" 화면이 따로 있어 한 단계를 더 들어가야 했다)
 */
@HiltViewModel
class MaintenanceItemDetailViewModel @Inject constructor(
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val maintenanceTypeRepository: MaintenanceTypeRepository,
    private val historyRepository: MaintenanceHistoryRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUi(settingId: Long): Flow<MaintenanceItemDetailUiState> {
        return carMaintenanceRepository.observeSetting(settingId)
            .flatMapLatest { setting ->
                if (setting == null) {
                    flowOf(MaintenanceItemDetailUiState(loading = false))
                } else {
                    combine(
                        maintenanceTypeRepository.observeType(setting.maintenanceTypeId),
                        historyRepository.observeHistories(settingId),
                        carMaintenanceRepository.observeMaintenanceOverview(setting.carId)
                    ) { type, histories, overview ->
                        val intervalKm = setting.intervalKm ?: type?.defaultIntervalKm
                        val intervalMonths = setting.intervalMonths ?: type?.defaultIntervalMonths

                        val lastMileage = histories.mapNotNull { it.serviceMileage }.maxOrNull()
                        val status = overview.firstOrNull { it.settingId == settingId }

                        MaintenanceItemDetailUiState(
                            loading = false,
                            carId = setting.carId,
                            typeName = type?.name ?: "정비 항목",
                            intervalKm = intervalKm,
                            intervalMonths = intervalMonths,
                            usingDefaultIntervals =
                                setting.intervalKm == null && setting.intervalMonths == null,
                            status = status?.status
                                ?: MaintenanceItemDetailUiState().status,
                            remainingText = status?.remainingText.orEmpty(),
                            progressRatio = status?.progressRatio,
                            lastServiceMileage = lastMileage,
                            nextDueMileage = if (lastMileage != null && intervalKm != null) {
                                lastMileage + intervalKm
                            } else null,
                            histories = histories,
                            averageIntervalKm = histories.averageIntervalKm(),
                            averageCost = histories.averageCost()
                        )
                    }
                }
            }
            .onStart { emit(MaintenanceItemDetailUiState(loading = true)) }
    }

    fun deleteHistory(historyId: Long) {
        viewModelScope.launch { carMaintenanceRepository.deleteHistory(historyId) }
    }
}

/** 실제 교체 간격의 평균. 주행거리가 적힌 기록이 2건 이상일 때만 의미가 있다. */
private fun List<MaintenanceHistory>.averageIntervalKm(): Int? {
    val mileages = mapNotNull { it.serviceMileage }.sorted()
    if (mileages.size < 2) return null
    val gaps = mileages.zipWithNext { a, b -> b - a }.filter { it > 0 }
    if (gaps.isEmpty()) return null
    return gaps.average().toInt()
}

private fun List<MaintenanceHistory>.averageCost(): Int? {
    val costs = mapNotNull { it.cost }.filter { it > 0 }
    if (costs.isEmpty()) return null
    return costs.average().toInt()
}
