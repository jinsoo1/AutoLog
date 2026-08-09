package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.MonthlyExpense
import kotlinx.coroutines.flow.Flow

interface ExpenseReportRepository {
    /**
     * 차량의 월별 지출 목록 — 가장 오래된 기록의 달부터 이번 달까지,
     * 기록 없는 달은 0원으로 채워져 온다. 기록이 하나도 없으면 빈 목록.
     */
    fun observeMonthlyExpenses(carId: Long): Flow<List<MonthlyExpense>>
}
