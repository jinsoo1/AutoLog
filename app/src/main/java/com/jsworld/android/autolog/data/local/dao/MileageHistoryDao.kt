package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageHistoryDao {

    @Insert
    suspend fun insertHistory(history: MileageHistoryEntity)

    /**
     * 주행거리를 낮춰 고쳤을 때, 그보다 높은 이력을 지운다.
     *
     * 주행거리계는 거꾸로 가지 않으므로 사용자가 값을 낮췄다면 앞의 입력이 오타였다는 뜻이다.
     * 리포트의 월 주행거리는 관측점의 **최댓값**으로 계산하기 때문에(ExpenseReportCalc.drivenKmIn),
     * 잘못 올린 행을 남겨두면 되돌려도 그 값이 계속 최댓값으로 남아 그 달이 부풀고
     * 다음 달은 0으로 눌린다. 실제로 "5천km 올렸다 되돌렸는데 리포트가 그대로"인 문제가 있었다.
     *
     * 주유·정비 기록에서 오는 관측점은 건드리지 않는다 — 그건 각자 편집 화면이 있다.
     */
    @Query("DELETE FROM mileage_history WHERE carId = :carId AND mileage > :mileage")
    suspend fun deleteHistoriesAbove(carId: Long, mileage: Int)

    @Query("""
        SELECT * FROM mileage_history
        WHERE carId = :carId
        ORDER BY recordedAt DESC
    """)
    fun getHistories(carId: Long): Flow<List<MileageHistoryEntity>>

    @Query("""
        SELECT * FROM mileage_history
        WHERE carId = :carId
        ORDER BY recordedAt ASC
    """)
    fun getHistoriesAsc(carId: Long): Flow<List<MileageHistoryEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM mileage_history
            WHERE carId = :carId
              AND recordedAt >= :startMillis
        )
    """)
    suspend fun hasMileageUpdateSince(carId: Long, startMillis: Long): Boolean

    @Query("""
    SELECT * FROM mileage_history
    WHERE carId = :carId
    ORDER BY recordedAt DESC
    LIMIT 1
""")
    suspend fun getLatestHistory(carId: Long): MileageHistoryEntity?
}