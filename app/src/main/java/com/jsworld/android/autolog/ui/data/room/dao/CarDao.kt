package com.jsworld.android.autolog.ui.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    /**
     * 차량 관련 기본 쿼리
     */
    @Query("SELECT * FROM cars ORDER BY id DESC")
    fun getAllCars(): Flow<List<CarEntity>>

    @Insert
    suspend fun insertCar(car: CarEntity): Long

    @Update
    suspend fun updateCar(car: CarEntity)

    @Query("UPDATE cars SET mileage = :mileage WHERE id = :carId")
    suspend fun updateMileage(carId: Long, mileage: Int)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1")
    fun getCarById(carId: Long): Flow<Car?>

    @Query("SELECT * FROM cars WHERE id = :carId")
    fun observeById(carId: Long): Flow<CarEntity?>

    /**
     * 대표 차량 관련 쿼리
     */
    @Query("SELECT * FROM cars WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryCar(): Flow<CarEntity?>

    @Query("UPDATE cars SET isPrimary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE cars SET isPrimary = 1 WHERE id = :carId")
    suspend fun setPrimary(carId: Long)

    @Query("UPDATE cars SET isPrimary = 0 WHERE id = :carId")
    suspend fun unsetPrimary(carId: Long)

    @Query("UPDATE cars SET isPrimary = 0 WHERE id != :carId")
    suspend fun clearPrimaryExcept(carId: Long)

}