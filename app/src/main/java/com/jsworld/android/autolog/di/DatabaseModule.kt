package com.jsworld.android.autolog.di

import android.content.Context
import androidx.room.Room
import com.jsworld.android.autolog.data.local.dao.BackupDao
import com.jsworld.android.autolog.data.local.dao.CarDao
import com.jsworld.android.autolog.data.local.dao.CarExportDao
import com.jsworld.android.autolog.data.local.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceFullDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.data.local.dao.MileageHistoryDao
import com.jsworld.android.autolog.data.local.db.AutoLogDatabase
import com.jsworld.android.autolog.data.local.db.MIGRATION_1_2
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
        )
            .addMigrations(MIGRATION_1_2)
            .build()


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

    @Provides
    fun provideMileageHistoryDao(db: AutoLogDatabase): MileageHistoryDao =
        db.mileageHistoryDao()

    @Provides
    fun provideCarExportDao(db: AutoLogDatabase): CarExportDao =
        db.carExportDao()

    @Provides
    @Singleton
    fun provideBackupDao(
        database: AutoLogDatabase
    ): BackupDao {
        return database.backupDao()
    }
}