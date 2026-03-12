package com.jsworld.android.autolog.ui.data.room.mapper

import android.R.attr.type
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.CarMaintenanceSetting
import com.jsworld.android.autolog.ui.data.item.MaintenanceHistory
import com.jsworld.android.autolog.ui.data.item.MaintenanceType
import com.jsworld.android.autolog.ui.data.item.SettingOption
import com.jsworld.android.autolog.ui.data.room.with.SettingWithHistory as RoomSettingWithHistory
import com.jsworld.android.autolog.ui.data.item.SettingWithHistory as ItemSettingWithHistory
import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import com.jsworld.android.autolog.ui.data.room.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.ui.data.room.entity.SettingOptionEntity
import com.jsworld.android.autolog.ui.data.room.entity.SettingWithHistoryEntity
import com.jsworld.android.autolog.ui.data.room.with.SettingWithTypeAndHistories


fun CarEntity.toDomain() = Car(
    id = id,
    name = name,
    plate = plate,
    year = year,
    mileage = mileage,
    fuelType = fuelType,
    notes = notes,
    isPrimary = isPrimary
)

fun Car.toEntity() = CarEntity(
    id = id,
    name = name,
    plate = plate,
    year = year,
    mileage = mileage,
    fuelType = fuelType,
    notes = notes,
    isPrimary = isPrimary
)

fun MaintenanceTypeEntity.toDomain() = MaintenanceType(
    id = id,
    name = name,
    defaultIntervalKm = defaultIntervalKm,
    defaultIntervalMonths = defaultIntervalMonths
)

fun MaintenanceType.toEntity() = MaintenanceTypeEntity(
    id = id,
    name = name,
    defaultIntervalKm = defaultIntervalKm,
    defaultIntervalMonths = defaultIntervalMonths
)


fun CarMaintenanceSettingEntity.toDomain() =
    CarMaintenanceSetting(id, carId, maintenanceTypeId, intervalKm, intervalMonths, isActive)


fun MaintenanceHistoryEntity.toDomain() =
    MaintenanceHistory(id, settingId, serviceDate, serviceMileage, place, cost, memo)


fun SettingWithHistoryEntity.toDomain() = ItemSettingWithHistory(
    setting = setting.toDomain(),
    type = type.toDomain(),
    histories = histories.map { it.toDomain() }
)

fun SettingWithTypeAndHistories.toDomain() = com.jsworld.android.autolog.ui.data.item.SettingWithHistory(
    setting = setting.toDomain(),
    type = type.toDomain(),
    histories = histories.map { it.toDomain() }
)

fun SettingOptionEntity.toDomain() =
    SettingOption(settingId, typeName, lastServiceDate, lastServiceMileage)


