package com.jsworld.android.autolog.ui.data.item

import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import com.jsworld.android.autolog.ui.data.room.entity.MileageHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.SettingWithHistoryEntity

data class CarExportData(
    val car: CarEntity,
    val settingsWithHistory: List<SettingWithHistoryEntity>,
    val mileageHistories: List<MileageHistoryEntity>
)