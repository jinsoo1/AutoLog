package com.jsworld.android.autolog.ui.data.room.repository

import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.room.dao.CarDao
import com.jsworld.android.autolog.ui.data.room.mapper.toDomain
import com.jsworld.android.autolog.ui.data.room.mapper.toEntity
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


@Singleton
class CarRepository @Inject constructor(
    private val carDao: CarDao
) {

    fun getAllCars(): Flow<List<Car>> =
        carDao.getAllCars().map { list -> list.map { it.toDomain() } }

//    fun getCarById(carId: Long): Flow<Car?> =
//        carDao.getCarById(carId)

    suspend fun addCar(input: Car) {
        carDao.insertCar(input.toEntity())
    }

    suspend fun updateMileage(carId: Long, mileage: Int) {
        carDao.updateMileage(carId, mileage)
    }

    suspend fun deleteCar(car: Car) {
        carDao.deleteCar(car.toEntity())
    }


    /**
     * 대표 차량 관련 쿼리
     */
    fun getPrimaryCar(): Flow<Car?> =
        carDao.getPrimaryCar().map { it?.toDomain() }

    suspend fun togglePrimaryCar(car: Car) {

        if (car.isPrimary) {
            // ⭐ 이미 대표 → 해제
            carDao.unsetPrimary(car.id)
        } else {
            // ⭐ 대표 아님 → 다른 대표 제거 후 설정
            carDao.clearPrimary()
            carDao.setPrimary(car.id)
        }
    }

    fun getCarById(carId: Long): Flow<Car?> =
        carDao.observeById(carId).map { it?.toDomain() }

    suspend fun updateCar(car: Car) {
        carDao.updateCar(car.toEntity())
        if (car.isPrimary) {
            carDao.clearPrimaryExcept(car.id) // 대표차량 1대만(원하면 유지)
        }
    }
}