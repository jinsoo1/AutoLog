package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val expenseReportRepository: ExpenseReportRepository
) : ViewModel() {

    // null = 로딩 중, emptyList = 기록 없음 — 빈 상태 화면이 로딩 중에 깜빡이지 않게 구분한다.
    private val expensesMap = mutableMapOf<Long, StateFlow<List<MonthlyExpense>?>>()

    fun expensesState(carId: Long): StateFlow<List<MonthlyExpense>?> =
        expensesMap.getOrPut(carId) {
            expenseReportRepository.observeMonthlyExpenses(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }
}
