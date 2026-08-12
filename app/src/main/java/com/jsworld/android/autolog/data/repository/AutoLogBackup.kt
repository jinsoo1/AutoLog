package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.local.entity.CareItemEntity
import com.jsworld.android.autolog.data.local.entity.CareRecordEntity
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
    val fuelRecords: List<FuelRecordBackup> = emptyList(),
    /**
     * 세차·관리 항목/기록 (DB v4 부터 별도 테이블).
     *
     * ⚠️ 기본값을 반드시 유지할 것 — 이 필드가 없던 과거 백업도 복원돼야 한다.
     * 과거 백업의 세차 기록은 maintenance* 목록에 들어 있으므로,
     * 복원 시 [withLegacyCareConverted] 로 이 목록으로 옮긴다.
     */
    val careItems: List<CareItemBackup> = emptyList(),
    val careRecords: List<CareRecordBackup> = emptyList()
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
data class CareItemBackup(
    val id: Long,
    val carId: Long,
    val name: String,
    val intervalMonths: Int?,
    val intervalWashCount: Int?,
    val isActive: Boolean
)

@Serializable
data class CareRecordBackup(
    val id: Long,
    val careItemId: Long,
    val performedAt: String?,
    val cost: Int?,
    val method: String?,
    val place: String?,
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

fun CareItemEntity.toBackup(): CareItemBackup =
    CareItemBackup(
        id = id,
        carId = carId,
        name = name,
        intervalMonths = intervalMonths,
        intervalWashCount = intervalWashCount,
        isActive = isActive
    )

fun CareItemBackup.toEntity(): CareItemEntity =
    CareItemEntity(
        id = id,
        carId = carId,
        name = name,
        intervalMonths = intervalMonths,
        intervalWashCount = intervalWashCount,
        isActive = isActive
    )

fun CareRecordEntity.toBackup(): CareRecordBackup =
    CareRecordBackup(
        id = id,
        careItemId = careItemId,
        performedAt = performedAt,
        cost = cost,
        method = method,
        place = place,
        memo = memo
    )

fun CareRecordBackup.toEntity(): CareRecordEntity =
    CareRecordEntity(
        id = id,
        careItemId = careItemId,
        performedAt = performedAt,
        cost = cost,
        method = method,
        place = place,
        memo = memo
    )

/**
 * DB v4 이전 백업의 세차 기록을 새 구조로 옮긴다.
 *
 * 과거 백업에는 세차가 정비 목록(maintenance*)에 섞여 있다 — 이름 규칙으로 골라
 * careItems/careRecords 로 변환하고 정비 목록에서 뺀다. 이미 careItems 가 있는
 * 새 백업은 그대로 돌려준다(정비 목록에 세차가 없으므로 할 일이 없다).
 *
 * 순수 함수라 단위 테스트로 지킨다.
 */
fun AutoLogBackup.withLegacyCareConverted(): AutoLogBackup {
    if (careItems.isNotEmpty() || careRecords.isNotEmpty()) return this

    val careTypeIds = maintenanceTypes
        .filter { isCareItemName(it.name) }
        .map { it.id }
        .toSet()
    if (careTypeIds.isEmpty()) return this

    val typeNameById = maintenanceTypes.associate { it.id to it.name }
    val careSettings = maintenanceSettings.filter { it.maintenanceTypeId in careTypeIds }
    val careSettingIds = careSettings.map { it.id }.toSet()

    // (carId, name) 별로 하나의 care_item 을 만든다 — 같은 이름 중복 설정은 합친다.
    var nextItemId = 1L
    val itemByKey = LinkedHashMap<Pair<Long, String>, CareItemBackup>()
    careSettings.forEach { s ->
        val name = typeNameById[s.maintenanceTypeId] ?: return@forEach
        val key = s.carId to name
        val existing = itemByKey[key]
        itemByKey[key] = CareItemBackup(
            id = existing?.id ?: nextItemId++,
            carId = s.carId,
            name = name,
            intervalMonths = existing?.intervalMonths ?: s.intervalMonths,
            intervalWashCount = null,
            isActive = (existing?.isActive ?: false) || s.isActive
        )
    }

    val settingById = careSettings.associateBy { it.id }
    var nextRecordId = 1L
    val records = maintenanceHistories
        .filter { it.settingId in careSettingIds }
        .mapNotNull { h ->
            val setting = settingById[h.settingId] ?: return@mapNotNull null
            val name = typeNameById[setting.maintenanceTypeId] ?: return@mapNotNull null
            val item = itemByKey[setting.carId to name] ?: return@mapNotNull null
            CareRecordBackup(
                id = nextRecordId++,
                careItemId = item.id,
                performedAt = h.serviceDate,
                cost = h.cost,
                method = null,
                place = h.place,
                memo = h.memo
            )
        }

    return copy(
        maintenanceTypes = maintenanceTypes.filterNot { it.id in careTypeIds },
        maintenanceSettings = maintenanceSettings.filterNot { it.id in careSettingIds },
        maintenanceHistories = maintenanceHistories.filterNot { it.settingId in careSettingIds },
        careItems = itemByKey.values.toList(),
        careRecords = records
    )
}
