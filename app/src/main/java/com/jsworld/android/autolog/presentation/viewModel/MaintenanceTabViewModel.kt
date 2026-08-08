package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MaintenanceTabViewModel @Inject constructor(
    private val historyRepository: MaintenanceHistoryRepository
) : ViewModel() {

    private val recordsMap = mutableMapOf<Long, StateFlow<List<CarMaintenanceRecord>>>()

    fun recordsState(carId: Long): StateFlow<List<CarMaintenanceRecord>> =
        recordsMap.getOrPut(carId) {
            historyRepository.observeCarRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
}
