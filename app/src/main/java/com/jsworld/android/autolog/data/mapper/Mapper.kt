package com.jsworld.android.autolog.data.mapper

import android.R.attr.type
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarMaintenanceSetting
import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import com.jsworld.android.autolog.domain.model.MaintenanceType
import com.jsworld.android.autolog.domain.model.SettingOption
import com.jsworld.android.autolog.domain.model.SettingWithHistory as ItemSettingWithHistory
import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.local.entity.SettingOptionRow
import com.jsworld.android.autolog.data.local.entity.SettingWithHistoryEntity
import com.jsworld.android.autolog.data.local.entity.SettingWithTypeAndHistories


fun CarEntity.toDomain(): Car {
    return Car(
        id = id,
        name = name,
        plate = plate,
        year = year,
        mileage = mileage,
        fuelType = fuelType,
        notes = notes,
        isPrimary = isPrimary,
        lastMileageUpdatedAt = lastMileageUpdatedAt
    )
}

fun Car.toEntity(): CarEntity {
    return CarEntity(
        id = id,
        name = name,
        plate = plate,
        year = year,
        mileage = mileage,
        fuelType = fuelType,
        notes = notes,
        isPrimary = isPrimary,
        lastMileageUpdatedAt = lastMileageUpdatedAt
    )
}

fun MaintenanceTypeEntity.toDomain(): MaintenanceType {
    return MaintenanceType(
        id = id,
        name = name,
        defaultIntervalKm = defaultIntervalKm,
        defaultIntervalMonths = defaultIntervalMonths
    )
}

fun MaintenanceType.toEntity(): MaintenanceTypeEntity {
    return MaintenanceTypeEntity(
        id = id,
        name = name,
        defaultIntervalKm = defaultIntervalKm,
        defaultIntervalMonths = defaultIntervalMonths
    )
}

fun CarMaintenanceSettingEntity.toDomain(): CarMaintenanceSetting {
    return CarMaintenanceSetting(
        id = id,
        carId = carId,
        maintenanceTypeId = maintenanceTypeId,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        isActive = isActive
    )
}


fun MaintenanceHistoryEntity.toDomain(): MaintenanceHistory {
    return MaintenanceHistory(
        id = id,
        settingId = settingId,
        serviceDate = serviceDate,
        serviceMileage = serviceMileage,
        place = place,
        cost = cost,
        memo = memo
    )
}

fun SettingWithHistoryEntity.toDomain() = ItemSettingWithHistory(
    setting = setting.toDomain(),
    type = type.toDomain(),
    histories = histories.map { it.toDomain() }
)

fun SettingWithTypeAndHistories.toDomain() = com.jsworld.android.autolog.domain.model.SettingWithHistory(
    setting = setting.toDomain(),
    type = type.toDomain(),
    histories = histories.map { it.toDomain() }
)

fun SettingOptionRow.toDomain(): SettingOption {
    return SettingOption(
        settingId = settingId,
        typeName = typeName,
        lastServiceDate = lastServiceDate,
        lastServiceMileage = lastServiceMileage
    )
}
