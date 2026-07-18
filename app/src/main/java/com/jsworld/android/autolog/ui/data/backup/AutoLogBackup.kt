package com.jsworld.android.autolog.ui.data.backup

import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import com.jsworld.android.autolog.ui.data.room.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.ui.data.room.entity.MileageHistoryEntity
import kotlinx.serialization.Serializable

/**
 * 오토로그 전체 백업 데이터
 *
 * backupVersion:
 * JSON 백업 파일 구조가 변경될 때 증가시킨다.
 *
 * databaseVersion:
 * 백업 당시 Room Database 버전이다.
 */
@Serializable
data class AutoLogBackup(
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val databaseVersion: Int,
    val createdAt: Long,
    val cars: List<CarBackup>,
    val maintenanceTypes: List<MaintenanceTypeBackup>,
    val maintenanceSettings: List<MaintenanceSettingBackup>,
    val maintenanceHistories: List<MaintenanceHistoryBackup>,
    val mileageHistories: List<MileageHistoryBackup>
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = 1
    }
}

@Serializable
data class CarBackup(
    val id: Long,
    val name: String,
    val plate: String,
    val year: String?,
    val mileage: Int,
    val fuelType: String?,
    val notes: String?,
    val isPrimary: Boolean,
    val lastMileageUpdatedAt: Long?
)

@Serializable
data class MaintenanceTypeBackup(
    val id: Long,
    val name: String,
    val defaultIntervalKm: Int?,
    val defaultIntervalMonths: Int?
)

@Serializable
data class MaintenanceSettingBackup(
    val id: Long,
    val carId: Long,
    val maintenanceTypeId: Long,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val isActive: Boolean
)

@Serializable
data class MaintenanceHistoryBackup(
    val id: Long,
    val settingId: Long,
    val serviceDate: String?,
    val serviceMileage: Int?,
    val place: String?,
    val cost: Int?,
    val memo: String?
)

@Serializable
data class MileageHistoryBackup(
    val id: Long,
    val carId: Long,
    val mileage: Int,
    val recordedAt: Long,
    val memo: String?
)

fun CarEntity.toBackup(): CarBackup =
    CarBackup(
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

fun MaintenanceTypeEntity.toBackup(): MaintenanceTypeBackup =
    MaintenanceTypeBackup(
        id = id,
        name = name,
        defaultIntervalKm = defaultIntervalKm,
        defaultIntervalMonths = defaultIntervalMonths
    )

fun CarMaintenanceSettingEntity.toBackup(): MaintenanceSettingBackup =
    MaintenanceSettingBackup(
        id = id,
        carId = carId,
        maintenanceTypeId = maintenanceTypeId,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        isActive = isActive
    )

fun MaintenanceHistoryEntity.toBackup(): MaintenanceHistoryBackup =
    MaintenanceHistoryBackup(
        id = id,
        settingId = settingId,
        serviceDate = serviceDate,
        serviceMileage = serviceMileage,
        place = place,
        cost = cost,
        memo = memo
    )

fun MileageHistoryEntity.toBackup(): MileageHistoryBackup =
    MileageHistoryBackup(
        id = id,
        carId = carId,
        mileage = mileage,
        recordedAt = recordedAt,
        memo = memo
    )

fun CarBackup.toEntity(): CarEntity =
    CarEntity(
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

fun MaintenanceTypeBackup.toEntity(): MaintenanceTypeEntity =
    MaintenanceTypeEntity(
        id = id,
        name = name,
        defaultIntervalKm = defaultIntervalKm,
        defaultIntervalMonths = defaultIntervalMonths
    )

fun MaintenanceSettingBackup.toEntity(): CarMaintenanceSettingEntity =
    CarMaintenanceSettingEntity(
        id = id,
        carId = carId,
        maintenanceTypeId = maintenanceTypeId,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        isActive = isActive
    )

fun MaintenanceHistoryBackup.toEntity(): MaintenanceHistoryEntity =
    MaintenanceHistoryEntity(
        id = id,
        settingId = settingId,
        serviceDate = serviceDate,
        serviceMileage = serviceMileage,
        place = place,
        cost = cost,
        memo = memo
    )

fun MileageHistoryBackup.toEntity(): MileageHistoryEntity =
    MileageHistoryEntity(
        id = id,
        carId = carId,
        mileage = mileage,
        recordedAt = recordedAt,
        memo = memo
    )