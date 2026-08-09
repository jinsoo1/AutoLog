package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.local.entity.MileagePointRow
import com.jsworld.android.autolog.data.local.entity.MonthlyAmountRow
import com.jsworld.android.autolog.data.local.entity.MonthlyFuelCostRow
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelRecordDao {

    @Query(
        """
        SELECT * FROM fuel_records
        WHERE carId = :carId
        ORDER BY filledAt DESC, mileage DESC, id DESC
        """
    )
    fun observeByCar(carId: Long): Flow<List<FuelRecordEntity>>

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    fun observeById(id: Long): Flow<FuelRecordEntity?>

    @Insert
    suspend fun insert(record: FuelRecordEntity): Long

    @Update
    suspend fun update(record: FuelRecordEntity)

    @Query("DELETE FROM fuel_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 월별 지출 합계(그래프용). `filledAt` 이 yyyy-MM-dd 이므로 앞 7글자가 곧 yyyy-MM 이다.
     *
     * 플러그인 하이브리드는 같은 달에 주유와 충전이 함께 있으므로 **unit 별로 나눠서** 낸다.
     */
    @Query(
        """
        SELECT substr(filledAt, 1, 7) AS month,
               unit                       AS unit,
               SUM(COALESCE(amount, 0))   AS totalAmount,
               SUM(COALESCE(quantity, 0)) AS totalQuantity
        FROM fuel_records
        WHERE carId = :carId
        GROUP BY substr(filledAt, 1, 7), unit
        ORDER BY month ASC
        """
    )
    fun observeMonthlyCost(carId: Long): Flow<List<MonthlyFuelCostRow>>

    /**
     * 입력 편의를 위한 최근 주유소/충전소 제안.
     * 주유소와 충전소는 서로 다른 장소이므로 종류(unit)별로 나눠서 제안한다.
     */
    @Query(
        """
        SELECT station FROM fuel_records
        WHERE carId = :carId
          AND unit = :unit
          AND station IS NOT NULL AND TRIM(station) <> ''
        GROUP BY station
        ORDER BY MAX(filledAt) DESC
        LIMIT :limit
        """
    )
    fun observeRecentStations(carId: Long, unit: String, limit: Int = 5): Flow<List<String>>

    /** 리포트용 — 월별 총지출(단위 구분 없이 합산) */
    @Query(
        """
        SELECT substr(filledAt, 1, 7) AS month,
               SUM(COALESCE(amount, 0)) AS total
        FROM fuel_records
        WHERE carId = :carId
        GROUP BY substr(filledAt, 1, 7)
        """
    )
    fun observeMonthlyTotal(carId: Long): Flow<List<MonthlyAmountRow>>

    /** 리포트용 — 주행거리 관측점(주유 기록의 날짜·누적 km) */
    @Query(
        """
        SELECT filledAt AS date, mileage AS mileage
        FROM fuel_records
        WHERE carId = :carId AND mileage IS NOT NULL
        """
    )
    fun observeMileagePoints(carId: Long): Flow<List<MileagePointRow>>

    /** 직전 주유 기록의 주행거리 — "이전 기록 이후 몇 km 달렸는지" 계산에 쓴다 */
    @Query(
        """
        SELECT mileage FROM fuel_records
        WHERE carId = :carId AND mileage IS NOT NULL
        ORDER BY filledAt DESC, mileage DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun getLatestMileage(carId: Long): Int?
}
