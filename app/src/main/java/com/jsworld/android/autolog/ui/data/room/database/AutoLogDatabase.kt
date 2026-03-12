package com.jsworld.android.autolog.ui.data.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jsworld.android.autolog.ui.data.room.dao.CarDao
import com.jsworld.android.autolog.ui.data.room.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceFullDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import com.jsworld.android.autolog.ui.data.room.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceTypeEntity

@Database(
    entities = [
        CarEntity::class,
        MaintenanceTypeEntity::class,
        CarMaintenanceSettingEntity::class,
        MaintenanceHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AutoLogDatabase : RoomDatabase() {

    abstract fun carDao(): CarDao
    abstract fun maintenanceTypeDao(): MaintenanceTypeDao
    abstract fun carMaintenanceSettingDao(): CarMaintenanceSettingDao
    abstract fun maintenanceHistoryDao(): MaintenanceHistoryDao
    abstract fun maintenanceFullDao(): MaintenanceFullDao
}