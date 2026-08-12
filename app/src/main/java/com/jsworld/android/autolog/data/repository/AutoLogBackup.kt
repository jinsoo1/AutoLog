package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity
import com.jsworld.android.autolog.domain.model.isCareItemName
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
    val mileageHistories: List<MileageHistoryBackup>,
    /**
     * 주유(충전) 기록.
     *
     * ⚠️ 기본값을 반드시 유지할 것. 이 필드가 없던 과거 백업도 그대로 복원돼야 한다.
     * (기본값이 있으므로 CURRENT_BACKUP_VERSION 을 올리지 않는다)
     *
     * 영수증 사진은 백업에 담지 않는다 — photoPath 만 남으므로 복원 후 파일이 없을 수 있다.
     */
    val fuelRecords: List<FuelRecordBackup> = emptyList()
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
    val defaultIntervalMonths: Int?,
    /**
     * 세차·관리 항목 플래그.
     *
     * ⚠️ 기본값을 반드시 유지할 것 — 이 필드가 없던 과거 백업도 그대로 복원돼야 한다.
     * 과거 백업은 false 로 들어오지만, 복원 후 이름 규칙으로 다시 채워준다
     * (BackupRepository.restore 참조).
     */
    val isCare: Boolean = false
)

@Serializable
data class MaintenanceSettingBackup(
    val id: Long,
    val carId: Long,
    val maintenanceTypeId: Long,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val isActive: Boolean,
    /**
     * "세차 N회마다" 주기.
     *
     * ⚠️ 기본값을 반드시 유지할 것 — 이 필드가 없던 과거 백업도 그대로 복원돼야 한다.
     */
    val intervalWashCount: Int? = null
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
data class FuelRecordBackup(
    val id: Long,
    val carId: Long,
    val filledAt: String,
    val mileage: Int?,
    val amount: Int?,
    val quantity: Double?,
    val unitPrice: Int?,
    val unit: String,
    val station: String?,
    val memo: String?,
    /** 파일 자체는 백업되지 않는다. 복원 후 존재하지 않을 수 있는 경로다. */
    val photoPath: String? = null
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
        defaultIntervalMonths = defaultIntervalMonths,
        isCare = isCare
    )

fun CarMaintenanceSettingEntity.toBackup(): MaintenanceSettingBackup =
    MaintenanceSettingBackup(
        id = id,
        carId = carId,
        maintenanceTypeId = maintenanceTypeId,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        isActive = isActive,
        intervalWashCount = intervalWashCount
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
        defaultIntervalMonths = defaultIntervalMonths,
        // 이 필드가 없던 과거 백업(false)은 이름 규칙으로 되살린다 —
        // 그러지 않으면 복원 후 세차 기록이 정비 타임라인에 섞인다.
        isCare = isCare || isCareItemName(name)
    )

fun MaintenanceSettingBackup.toEntity(): CarMaintenanceSettingEntity =
    CarMaintenanceSettingEntity(
        id = id,
        carId = carId,
        maintenanceTypeId = maintenanceTypeId,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        isActive = isActive,
        intervalWashCount = intervalWashCount
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

fun FuelRecordEntity.toBackup(): FuelRecordBackup =
    FuelRecordBackup(
        id = id,
        carId = carId,
        filledAt = filledAt,
        mileage = mileage,
        amount = amount,
        quantity = quantity,
        unitPrice = unitPrice,
        unit = unit,
        station = station,
        memo = memo,
        photoPath = photoPath
    )

fun FuelRecordBackup.toEntity(): FuelRecordEntity =
    FuelRecordEntity(
        id = id,
        carId = carId,
        filledAt = filledAt,
        mileage = mileage,
        amount = amount,
        quantity = quantity,
        unitPrice = unitPrice,
        unit = unit,
        station = station,
        memo = memo,
        photoPath = photoPath
    )
