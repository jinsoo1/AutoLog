package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.data.local.entity.CareItemEntity
import com.jsworld.android.autolog.data.local.entity.CareRecordEntity
import com.jsworld.android.autolog.data.local.entity.CareRecordRow
import com.jsworld.android.autolog.data.local.entity.MonthlyAmountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CareDao {

    /* ── 항목 ── */

    @Query("SELECT * FROM care_items WHERE carId = :carId ORDER BY id ASC")
    fun observeItems(carId: Long): Flow<List<CareItemEntity>>

    @Query("SELECT * FROM care_items WHERE carId = :carId AND name = :name LIMIT 1")
    suspend fun findItem(carId: Long, name: String): CareItemEntity?

    @Insert
    suspend fun insertItem(item: CareItemEntity): Long

    @Query("UPDATE care_items SET isActive = :active WHERE id = :itemId")
    suspend fun setItemActive(itemId: Long, active: Boolean)

    @Query(
        """
        UPDATE care_items
        SET intervalDays = :days, intervalWashCount = :washCount
        WHERE id = :itemId
        """
    )
    suspend fun updateInterval(itemId: Long, days: Int?, washCount: Int?)

    /* ── 기록 ── */

    @Query(
        """
        SELECT r.id AS id,
               r.careItemId AS careItemId,
               i.name AS itemName,
               r.performedAt AS performedAt,
               r.cost AS cost,
               r.method AS method,
               r.place AS place,
               r.memo AS memo
        FROM care_records r
        JOIN care_items i ON i.id = r.careItemId
        WHERE i.carId = :carId
        ORDER BY r.performedAt DESC, r.id DESC
        """
    )
    fun observeRecords(carId: Long): Flow<List<CareRecordRow>>

    @Insert
    suspend fun insertRecord(record: CareRecordEntity): Long

    @Update
    suspend fun updateRecord(record: CareRecordEntity)

    @Query("SELECT * FROM care_records WHERE id = :recordId")
    suspend fun getRecord(recordId: Long): CareRecordEntity?

    @Query("DELETE FROM care_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: Long)

    /** 리포트용 — 월별 세차·관리 지출 합계 */
    @Query(
        """
        SELECT substr(r.performedAt, 1, 7) AS month,
               SUM(COALESCE(r.cost, 0)) AS total
        FROM care_records r
        JOIN care_items i ON i.id = r.careItemId
        WHERE i.carId = :carId AND r.performedAt IS NOT NULL
        GROUP BY substr(r.performedAt, 1, 7)
        """
    )
    fun observeMonthlyCost(carId: Long): Flow<List<MonthlyAmountRow>>

    /**
     * 리포트 '금액 미입력 N건' 표기용.
     *
     * 세는 단위는 기록이 아니라 하루 묶음이다 — 세차에 비용을 적고 함께 한 왁스를
     * 비워두는 게 정상 사용이라, 기록 단위로 세면 멀쩡한 날이 미입력으로 잡힌다.
     * 그날 아무 항목에도 비용이 없을 때만 1건으로 센다.
     */
    @Query(
        """
        SELECT month, COUNT(*) AS total FROM (
            SELECT substr(r.performedAt, 1, 7) AS month,
                   COUNT(r.cost) AS costed
            FROM care_records r
            JOIN care_items i ON i.id = r.careItemId
            WHERE i.carId = :carId AND r.performedAt IS NOT NULL
            GROUP BY r.performedAt
        )
        WHERE costed = 0
        GROUP BY month
        """
    )
    fun observeMonthlyMissingCostCount(carId: Long): Flow<List<MonthlyAmountRow>>

    /* ── 백업·내보내기 ── */

    @Query("SELECT * FROM care_items")
    suspend fun getAllItems(): List<CareItemEntity>

    @Query("SELECT * FROM care_records")
    suspend fun getAllRecords(): List<CareRecordEntity>

    @Query("SELECT * FROM care_items WHERE carId = :carId ORDER BY id ASC")
    suspend fun getItemsForCar(carId: Long): List<CareItemEntity>

    @Query(
        """
        SELECT r.id AS id,
               r.careItemId AS careItemId,
               i.name AS itemName,
               r.performedAt AS performedAt,
               r.cost AS cost,
               r.method AS method,
               r.place AS place,
               r.memo AS memo
        FROM care_records r
        JOIN care_items i ON i.id = r.careItemId
        WHERE i.carId = :carId
        ORDER BY r.performedAt DESC, r.id DESC
        """
    )
    suspend fun getRecordsForCar(carId: Long): List<CareRecordRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CareItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<CareRecordEntity>)

    @Query("DELETE FROM care_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM care_items")
    suspend fun deleteAllItems()
}
