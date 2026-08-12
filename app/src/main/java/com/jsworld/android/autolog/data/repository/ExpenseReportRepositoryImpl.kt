package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.dao.CareDao
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
    private val mileageHistoryDao: MileageHistoryDao,
    private val careDao: CareDao
) : ExpenseReportRepository {

    override fun observeMonthlyExpenses(carId: Long): Flow<List<MonthlyExpense>> {
        // 세차는 별도 테이블 — 월 합계와 금액 미입력 건수를 따로 가져와 합친다.
        val careFlow = combine(
            careDao.observeMonthlyCost(carId),
            careDao.observeMonthlyMissingCostCount(carId)
        ) { cost, missing ->
            cost.associate { it.month to it.total } to
                missing.associate { it.month to it.total.toInt() }
        }

        return combine(
            fuelRecordDao.observeMonthlyTotal(carId),
            maintenanceHistoryDao.observeMonthlyCostRows(carId),
            fuelRecordDao.observeMileagePoints(carId),
            maintenanceHistoryDao.observeMileagePoints(carId),
            mileageHistoryDao.getHistoriesAsc(carId),
            careFlow
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val fuelMonthly = values[0] as List<com.jsworld.android.autolog.data.local.entity.MonthlyAmountRow>
            @Suppress("UNCHECKED_CAST")
            val costRows = values[1] as List<com.jsworld.android.autolog.data.local.entity.MaintenanceCostRow>
            @Suppress("UNCHECKED_CAST")
            val fuelPoints = values[2] as List<com.jsworld.android.autolog.data.local.entity.MileagePointRow>
            @Suppress("UNCHECKED_CAST")
            val maintPoints = values[3] as List<com.jsworld.android.autolog.data.local.entity.MileagePointRow>
            @Suppress("UNCHECKED_CAST")
            val mileageHistories = values[4] as List<com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity>
            @Suppress("UNCHECKED_CAST")
            val care = values[5] as Pair<Map<String, Long>, Map<String, Int>>

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
                maintenanceRows = costRows.map { ExpenseCostRow(it.month, it.typeName, it.cost) },
                mileagePoints = points,
                current = YearMonth.now(),
                careByMonth = care.first,
                careMissingByMonth = care.second
            )
        }
    }
}
