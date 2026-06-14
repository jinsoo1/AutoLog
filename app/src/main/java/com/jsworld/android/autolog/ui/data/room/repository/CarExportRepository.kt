package com.jsworld.android.autolog.ui.data.room.repository

import com.jsworld.android.autolog.ui.data.item.CarExportData
import com.jsworld.android.autolog.ui.data.room.dao.CarExportDao
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CarExportRepository @Inject constructor(
    private val carExportDao: CarExportDao
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

        return CarExportData(
            car = car,
            settingsWithHistory = settingsWithHistory,
            mileageHistories = mileageHistories
        )
    }
}