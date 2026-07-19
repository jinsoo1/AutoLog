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