package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import com.jsworld.android.autolog.presentation.state.MaintenanceHistoryListUiState
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

@HiltViewModel
class MaintenanceHistoryListViewModel @Inject constructor(
    private val settingRepository: CarMaintenanceRepository,
    private val typeRepository: MaintenanceTypeRepository,
    private val historyRepository: MaintenanceHistoryRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUi(settingId: Long): Flow<MaintenanceHistoryListUiState> {
        return settingRepository.observeSetting(settingId)
            .flatMapLatest { setting ->
                if (setting == null) {
                    flowOf(MaintenanceHistoryListUiState(loading = false))
                } else {
                    combine(
                        typeRepository.observeType(setting.maintenanceTypeId),
                        historyRepository.observeHistories(settingId)
                    ) { type, histories ->
                        MaintenanceHistoryListUiState(
                            loading = false,
                            typeName = type?.name ?: "정비 내역",
                            histories = histories
                        )
                    }
                }
            }
            .onStart { emit(MaintenanceHistoryListUiState(loading = true)) }
    }
}