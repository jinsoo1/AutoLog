package com.jsworld.android.autolog.ui.data.module

import android.content.Context
import androidx.room.Room
import com.jsworld.android.autolog.ui.data.room.dao.CarDao
import com.jsworld.android.autolog.ui.data.room.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceFullDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.ui.data.room.database.AutoLogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AutoLogDatabase =
        Room.databaseBuilder(
            context,
            AutoLogDatabase::class.java,
            "autolog_db"
        ).build()

    @Provides
    fun provideCarDao(db: AutoLogDatabase): CarDao = db.carDao()

    @Provides
    fun provideTypeDao(db: AutoLogDatabase): MaintenanceTypeDao =
        db.maintenanceTypeDao()

    @Provides
    fun provideSettingDao(db: AutoLogDatabase): CarMaintenanceSettingDao =
        db.carMaintenanceSettingDao()

    @Provides
    fun provideHistoryDao(db: AutoLogDatabase): MaintenanceHistoryDao =
        db.maintenanceHistoryDao()

    @Provides
    fun provideFullDao(db: AutoLogDatabase): MaintenanceFullDao =
        db.maintenanceFullDao()
}