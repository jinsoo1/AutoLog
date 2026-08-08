package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity
import com.jsworld.android.autolog.data.local.entity.SettingWithHistoryEntity

data class CarExportData(
    val car: CarEntity,
    val settingsWithHistory: List<SettingWithHistoryEntity>,
    val mileageHistories: List<MileageHistoryEntity>,
    val fuelRecords: List<FuelRecordEntity>
)