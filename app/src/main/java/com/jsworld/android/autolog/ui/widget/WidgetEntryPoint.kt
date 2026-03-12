package com.jsworld.android.autolog.ui.widget

import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun carRepository(): CarRepository
    fun carWidgetRepository(): CarWidgetRepository
}