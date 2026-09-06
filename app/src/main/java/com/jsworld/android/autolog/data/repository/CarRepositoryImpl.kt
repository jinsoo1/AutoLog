package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.domain.repository.CarRepository

import androidx.room.withTransaction
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.data.local.dao.CarDao
import com.jsworld.android.autolog.data.local.dao.MileageHistoryDao
import com.jsworld.android.autolog.data.local.db.AutoLogDatabase
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity
import com.jsworld.android.autolog.data.mapper.toDomain
import com.jsworld.android.autolog.data.mapper.toEntity
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


@Singleton
class CarRepositoryImpl @Inject constructor(
    private val database: AutoLogDatabase,
    private val carDao: CarDao,
    private val mileageHistoryDao: MileageHistoryDao
) : CarRepository {

    override fun getAllCars(): Flow<List<Car>> =
        carDao.getAllCars().map { list -> list.map { it.toDomain() } }

    override suspend fun addCar(input: Car): Long {
        val carId = carDao.insertCar(input.toEntity())

        // 등록 주행거리는 항상 기준점으로 남긴다 — 0km(새로 뽑은 차)도 유효한 값이다.
        // 이 행이 없으면 등록한 달은 앞선 관측점이 없어 주행거리가 "계산 불가"로 뜬다
        // (ExpenseReportCalc.drivenKmIn). 등록 화면에서 주행거리를 필수로 받으므로
        // "모르는 값을 0으로 적은" 경우는 여기 오지 않는다.
        mileageHistoryDao.insertHistory(
            MileageHistoryEntity(
                carId = carId,
                mileage = input.mileage,
                recordedAt = 0L,
                memo = "초기 등록 주행거리"
            )
        )

        return carId
    }

    override suspend fun updateMileage(carId: Long, mileage: Int) {
        val now = System.currentTimeMillis()

        database.withTransaction {
            carDao.updateMileageWithTimestamp(
                carId = carId,
                mileage = mileage,
                updatedAt = now
            )

            // 낮춰 고친 경우, 그보다 높은 이력은 오타였다는 뜻이므로 지운다.
            // 남겨두면 리포트가 최댓값을 쓰기 때문에 되돌려도 반영되지 않는다.
            mileageHistoryDao.deleteHistoriesAbove(carId = carId, mileage = mileage)

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

    override suspend fun deleteCar(car: Car) {
        carDao.deleteCar(car.toEntity())
    }

    override fun getPrimaryCar(): Flow<Car?> =
        carDao.getPrimaryCar().map { it?.toDomain() }

    override suspend fun togglePrimaryCar(car: Car) {
        if (car.isPrimary) {
            carDao.unsetPrimary(car.id)
        } else {
            carDao.clearPrimary()
            carDao.setPrimary(car.id)
        }
    }

    override fun getCarById(carId: Long): Flow<Car?> =
        carDao.observeById(carId).map { it?.toDomain() }

    override suspend fun updateCar(car: Car) {
        carDao.updateCar(car.toEntity())
        if (car.isPrimary) {
            carDao.clearPrimaryExcept(car.id)
        }
    }

    override suspend fun getCarsNeedingWeeklyMileageUpdate(weekStartMillis: Long): List<Car> {
        return carDao.getCarsNeedingWeeklyMileageUpdate(weekStartMillis)
            .map { it.toDomain() }
    }
}