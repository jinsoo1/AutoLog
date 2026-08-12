package com.jsworld.android.autolog.di

import com.jsworld.android.autolog.data.datastore.CarSortPreferenceRepositoryImpl
import com.jsworld.android.autolog.data.datastore.NoticeReadRepositoryImpl
import com.jsworld.android.autolog.data.datastore.UserPrefsRepositoryImpl
import com.jsworld.android.autolog.data.repository.CarMaintenanceRepositoryImpl
import com.jsworld.android.autolog.data.repository.CarRepositoryImpl
import com.jsworld.android.autolog.data.repository.CareRepositoryImpl
import com.jsworld.android.autolog.data.repository.ExpenseReportRepositoryImpl
import com.jsworld.android.autolog.data.repository.FuelRecordRepositoryImpl
import com.jsworld.android.autolog.data.repository.MaintenanceHistoryRepositoryImpl
import com.jsworld.android.autolog.data.repository.MaintenanceTypeRepositoryImpl
import com.jsworld.android.autolog.data.repository.NoticeRepositoryImpl
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.CareRepository
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import com.jsworld.android.autolog.domain.repository.CarSortPreferenceRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository
import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository
import com.jsworld.android.autolog.domain.repository.NoticeReadRepository
import com.jsworld.android.autolog.domain.repository.NoticeRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCarRepository(impl: CarRepositoryImpl): CarRepository

    @Binds
    @Singleton
    abstract fun bindMaintenanceTypeRepository(impl: MaintenanceTypeRepositoryImpl): MaintenanceTypeRepository

    @Binds
    abstract fun bindMaintenanceHistoryRepository(impl: MaintenanceHistoryRepositoryImpl): MaintenanceHistoryRepository

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(impl: NoticeRepositoryImpl): NoticeRepository

    @Binds
    @Singleton
    abstract fun bindCarMaintenanceRepository(impl: CarMaintenanceRepositoryImpl): CarMaintenanceRepository

    @Binds
    @Singleton
    abstract fun bindCarSortPreferenceRepository(impl: CarSortPreferenceRepositoryImpl): CarSortPreferenceRepository

    @Binds
    @Singleton
    abstract fun bindUserPrefsRepository(impl: UserPrefsRepositoryImpl): UserPrefsRepository

    @Binds
    @Singleton
    abstract fun bindNoticeReadRepository(impl: NoticeReadRepositoryImpl): NoticeReadRepository

    @Binds
    @Singleton
    abstract fun bindFuelRecordRepository(impl: FuelRecordRepositoryImpl): FuelRecordRepository

    @Binds
    @Singleton
    abstract fun bindExpenseReportRepository(impl: ExpenseReportRepositoryImpl): ExpenseReportRepository

    @Binds
    @Singleton
    abstract fun bindCareRepository(impl: CareRepositoryImpl): CareRepository
}
