package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.Car
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getAllCars(): Flow<List<Car>>
    /** @return 새 차량의 id */
    suspend fun addCar(input: Car): Long
    suspend fun updateMileage(carId: Long, mileage: Int)
    suspend fun deleteCar(car: Car)
    fun getPrimaryCar(): Flow<Car?>
    suspend fun togglePrimaryCar(car: Car)
    fun getCarById(carId: Long): Flow<Car?>
    suspend fun updateCar(car: Car)
    suspend fun getCarsNeedingWeeklyMileageUpdate(weekStartMillis: Long): List<Car>
}
