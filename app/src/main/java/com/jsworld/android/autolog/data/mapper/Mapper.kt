package com.jsworld.android.autolog.data.mapper

import android.R.attr.type
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.CarMaintenanceSetting
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyFuelCost
import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import com.jsworld.android.autolog.domain.model.MaintenanceType
import com.jsworld.android.autolog.domain.model.SettingOption
import com.jsworld.android.autolog.domain.model.SettingWithHistory as ItemSettingWithHistory
import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceRecordRow
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.local.entity.MonthlyFuelCostRow
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

fun CarMaintenanceRecordRow.toDomain(): CarMaintenanceRecord {
    return CarMaintenanceRecord(
        historyId = historyId,
        settingId = settingId,
        typeId = typeId,
        typeName = typeName,
        serviceDate = serviceDate,
        serviceMileage = serviceMileage,
        place = place,
        cost = cost,
        memo = memo,
        isRepair = isRepair
    )
}

fun FuelRecordEntity.toDomain(): FuelRecord =
    FuelRecord(
        id = id,
        carId = carId,
        filledAt = filledAt,
        mileage = mileage,
        amount = amount,
        quantity = quantity,
        unitPrice = unitPrice,
        unit = FuelUnit.fromSymbol(unit),
        station = station,
        memo = memo,
        photoPath = photoPath
    )

fun FuelRecord.toEntity(): FuelRecordEntity =
    FuelRecordEntity(
        id = id,
        carId = carId,
        filledAt = filledAt,
        mileage = mileage,
        amount = amount,
        quantity = quantity,
        unitPrice = unitPrice,
        unit = unit.symbol,
        station = station,
        memo = memo,
        photoPath = photoPath
    )

fun MonthlyFuelCostRow.toDomain(): MonthlyFuelCost =
    MonthlyFuelCost(
        month = month,
        unit = FuelUnit.fromSymbol(unit),
        totalAmount = totalAmount,
        totalQuantity = totalQuantity
    )
