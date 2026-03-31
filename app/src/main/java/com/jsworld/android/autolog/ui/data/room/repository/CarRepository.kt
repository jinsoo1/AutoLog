package com.jsworld.android.autolog.ui.data.room.repository

import androidx.room.withTransaction
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.room.dao.CarDao
import com.jsworld.android.autolog.ui.data.room.dao.MileageHistoryDao
import com.jsworld.android.autolog.ui.data.room.database.AutoLogDatabase
import com.jsworld.android.autolog.ui.data.room.entity.MileageHistoryEntity
import com.jsworld.android.autolog.ui.data.room.mapper.toDomain
import com.jsworld.android.autolog.ui.data.room.mapper.toEntity
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


@Singleton
class CarRepository @Inject constructor(
    private val database: AutoLogDatabase,
    private val carDao: CarDao,
    private val mileageHistoryDao: MileageHistoryDao
) {

    fun getAllCars(): Flow<List<Car>> =
        carDao.getAllCars().map { list -> list.map { it.toDomain() } }

    suspend fun addCar(input: Car) {
        val carId = carDao.insertCar(input.toEntity())

        if (input.mileage > 0) {
            mileageHistoryDao.insertHistory(
                MileageHistoryEntity(
                    carId = carId,
                    mileage = input.mileage,
                    recordedAt = 0L,
                    memo = "초기 등록 주행거리"
                )
            )
        }
    }

    suspend fun updateMileage(carId: Long, mileage: Int) {
        val now = System.currentTimeMillis()

        database.withTransaction {
            carDao.updateMileageWithTimestamp(
                carId = carId,
                mileage = mileage,
                updatedAt = now
            )

            mileageHistoryDao.insertHistory(
                MileageHistoryEntity(
                    carId = carId,
                    mileage = mileage,
                    recordedAt = now,
                    memo = "주행거리 업데이트"
                )
            )
        }
    }

    suspend fun deleteCar(car: Car) {
        carDao.deleteCar(car.toEntity())
    }

    fun getPrimaryCar(): Flow<Car?> =
        carDao.getPrimaryCar().map { it?.toDomain() }

    suspend fun togglePrimaryCar(car: Car) {
        if (car.isPrimary) {
            carDao.unsetPrimary(car.id)
        } else {
            carDao.clearPrimary()
            carDao.setPrimary(car.id)
        }
    }

    fun getCarById(carId: Long): Flow<Car?> =
        carDao.observeById(carId).map { it?.toDomain() }

    suspend fun updateCar(car: Car) {
        carDao.updateCar(car.toEntity())
        if (car.isPrimary) {
            carDao.clearPrimaryExcept(car.id)
        }
    }

    suspend fun getCarsNeedingWeeklyMileageUpdate(weekStartMillis: Long): List<Car> {
        return carDao.getCarsNeedingWeeklyMileageUpdate(weekStartMillis)
            .map { it.toDomain() }
    }
}