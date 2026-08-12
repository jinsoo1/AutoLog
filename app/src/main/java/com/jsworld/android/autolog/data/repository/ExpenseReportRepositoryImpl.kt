package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.dao.FuelRecordDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.data.local.dao.MileageHistoryDao
import com.jsworld.android.autolog.domain.model.ExpenseCostRow
import com.jsworld.android.autolog.domain.model.ExpenseReportCalc
import com.jsworld.android.autolog.domain.model.MileagePoint
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class ExpenseReportRepositoryImpl @Inject constructor(
    private val fuelRecordDao: FuelRecordDao,
    private val maintenanceHistoryDao: MaintenanceHistoryDao,
    private val mileageHistoryDao: MileageHistoryDao
) : ExpenseReportRepository {

    override fun observeMonthlyExpenses(carId: Long): Flow<List<MonthlyExpense>> =
        combine(
            fuelRecordDao.observeMonthlyTotal(carId),
            maintenanceHistoryDao.observeMonthlyCostRows(carId),
            fuelRecordDao.observeMileagePoints(carId),
            maintenanceHistoryDao.observeMileagePoints(carId),
            mileageHistoryDao.getHistoriesAsc(carId)
        ) { fuelMonthly, costRows, fuelPoints, maintPoints, mileageHistories ->

            // 주행거리 관측점은 세 곳에서 모은다 — 주유 기록, 정비 기록, 주행거리 업데이트.
            val points = buildList {
                fuelPoints.forEach { add(MileagePoint(it.date, it.mileage)) }
                maintPoints.forEach { add(MileagePoint(it.date, it.mileage)) }
                mileageHistories.forEach {
                    // recordedAt = 0(차량 등록 시 초기값)은 등록일을 모르므로
                    // 가장 이른 날짜로 취급된다 — 기준점 역할로 충분하다.
                    val date = Instant.ofEpochMilli(it.recordedAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    add(MileagePoint(date, it.mileage))
                }
            }

            ExpenseReportCalc.build(
                fuelByMonth = fuelMonthly.associate { it.month to it.total },
                maintenanceRows = costRows.map {
                    ExpenseCostRow(it.month, it.typeName, it.cost, it.isCare)
                },
                mileagePoints = points,
                current = YearMonth.now()
            )
        }
}
