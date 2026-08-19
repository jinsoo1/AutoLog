package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.data.local.entity.CarScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarScheduleDao {

    @Query("SELECT * FROM car_schedules WHERE carId = :carId ORDER BY dueDate ASC")
    fun observeByCar(carId: Long): Flow<List<CarScheduleEntity>>

    /** 알림 워커용 — 전 차량을 한 번에 검사한다 */
    @Query("SELECT * FROM car_schedules ORDER BY dueDate ASC")
    suspend fun getAll(): List<CarScheduleEntity>

    @Query("SELECT * FROM car_schedules WHERE id = :id")
    suspend fun getById(id: Long): CarScheduleEntity?

    @Insert
    suspend fun insert(schedule: CarScheduleEntity): Long

    @Update
    suspend fun update(schedule: CarScheduleEntity)

    @Query("UPDATE car_schedules SET dueDate = :dueDate WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDate: String)

    @Query("DELETE FROM car_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    /* ── 백업·복원 ── */

    @Query("SELECT * FROM car_schedules")
    suspend fun getAllForBackup(): List<CarScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<CarScheduleEntity>)

    @Query("DELETE FROM car_schedules")
    suspend fun deleteAll()
}
