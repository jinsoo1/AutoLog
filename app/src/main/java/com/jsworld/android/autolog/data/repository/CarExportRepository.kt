package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.repository.CarExportData
import com.jsworld.android.autolog.data.local.dao.CarExportDao
import com.jsworld.android.autolog.data.local.dao.CareDao
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CarExportRepository @Inject constructor(
    private val carExportDao: CarExportDao,
    private val careDao: CareDao
) {

    /**
     * 차량 1대의 엑셀 출력용 데이터 가져오기
     */
    suspend fun getCarExportData(carId: Long): CarExportData? {
        val car = carExportDao.getCarForExport(carId)
            ?: return null

        val settingsWithHistory =
            carExportDao.getSettingsWithHistoryForExport(carId)

        val mileageHistories =
            carExportDao.getMileageHistoriesForExport(carId)

        val fuelRecords =
            carExportDao.getFuelRecordsForExport(carId)

        val careRecords = careDao.getRecordsForCar(carId)

        return CarExportData(
            car = car,
            settingsWithHistory = settingsWithHistory,
            mileageHistories = mileageHistories,
            fuelRecords = fuelRecords,
            careRecords = careRecords
        )
    }
}