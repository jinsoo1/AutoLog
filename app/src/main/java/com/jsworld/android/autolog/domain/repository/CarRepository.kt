package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.Car
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getAllCars(): Flow<List<Car>>
    suspend fun addCar(input: Car)
    suspend fun updateMileage(carId: Long, mileage: Int)
    suspend fun deleteCar(car: Car)
    fun getPrimaryCar(): Flow<Car?>
    suspend fun togglePrimaryCar(car: Car)
    fun getCarById(carId: Long): Flow<Car?>
    suspend fun updateCar(car: Car)
    suspend fun getCarsNeedingWeeklyMileageUpdate(weekStartMillis: Long): List<Car>
}
