package com.jsworld.android.autolog.presentation.widget

import com.jsworld.android.autolog.domain.repository.CarRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun carRepository(): CarRepository
    fun carWidgetRepository(): CarWidgetRepository
}