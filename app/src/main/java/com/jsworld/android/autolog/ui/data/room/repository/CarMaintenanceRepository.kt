package com.jsworld.android.autolog.ui.data.room.repository

import androidx.room.withTransaction
import com.jsworld.android.autolog.ui.data.item.CarMaintenanceDigest
import com.jsworld.android.autolog.ui.data.item.CarMaintenanceSetting
import com.jsworld.android.autolog.ui.data.item.MaintenanceSort
import com.jsworld.android.autolog.ui.data.item.MaintenanceStatus
import com.jsworld.android.autolog.ui.data.item.MaintenanceSummaryUi
import com.jsworld.android.autolog.ui.data.item.MaintenanceTypePickUi
import com.jsworld.android.autolog.ui.data.item.MaintenanceUiModel
import com.jsworld.android.autolog.ui.data.item.SettingOption
import com.jsworld.android.autolog.ui.data.room.dao.CarDao
import com.jsworld.android.autolog.ui.data.room.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceFullDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.ui.data.room.dao.MileageHistoryDao
import com.jsworld.android.autolog.ui.data.room.database.AutoLogDatabase
import com.jsworld.android.autolog.ui.data.room.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.ui.data.room.entity.MileageHistoryEntity
import com.jsworld.android.autolog.ui.data.room.mapper.toDomain
import com.jsworld.android.autolog.ui.data.room.with.SettingWithHistory
import com.jsworld.android.autolog.ui.data.item.SettingWithHistory as ItemSettingWithHistory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.collections.map
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private const val SOON_RATIO = 0.15f

@Singleton
class CarMaintenanceRepository @Inject constructor(
    private val database: AutoLogDatabase,
    private val fullDao: MaintenanceFullDao,
    private val maintenanceTypeDao: MaintenanceTypeDao,
    private val carDao: CarDao, // car mileage 가져오려면 필요(또는 CarRepository 사용)
    private val settingDao: CarMaintenanceSettingDao,
    private val maintenanceHistoryDao: MaintenanceHistoryDao,
    private val mileageHistoryDao: MileageHistoryDao,
    private val carSortPrefRepository: CarSortPreferenceRepository
) {

    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
    private fun String.toLocalDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this, ISO) }.getOrNull()
    private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)


    fun getMaintenanceSettingsSorted(carId: Long, sort: MaintenanceSort): Flow<List<CarMaintenanceSetting>> {
        val flow = when (sort) {
            MaintenanceSort.DEFAULT -> settingDao.getSettingsForCar(carId)
            MaintenanceSort.REMAINING_KM -> settingDao.getSettingsForCarOrderByRemainingKm(carId)
            MaintenanceSort.DUE_DATE -> settingDao.getSettingsForCarOrderByDueDate(carId)
            MaintenanceSort.URGENT_MIN -> settingDao.getSettingsForCarOrderByCombined(carId) // "더 급한쪽 우선(min)" 버전
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }


    private fun buildMaintenanceUiModelsFromRoom(
        carMileage: Int,
        roomItems: List<SettingWithHistory>,
        typeMap: Map<Long, MaintenanceTypeEntity>,
        today: LocalDate
    ): List<MaintenanceUiModel> {

        return roomItems.mapNotNull { item ->
            val setting = item.setting
            val type = typeMap[setting.maintenanceTypeId]
            val name = type?.name ?: "정비항목(${setting.maintenanceTypeId})"

            val intervalKm = setting.intervalKm ?: type?.defaultIntervalKm
            val intervalMonths = setting.intervalMonths ?: type?.defaultIntervalMonths

            if (intervalKm == null && intervalMonths == null) return@mapNotNull null

            val lastMileage = item.history.mapNotNull { it.serviceMileage }.maxOrNull()
            val lastDate = item.history.mapNotNull { it.serviceDate?.toLocalDateOrNull() }.maxOrNull()

            val hasNoHistory = (lastMileage == null && lastDate == null)

            val baseLastMileage = lastMileage ?: 0
            val baseDateForCalc = lastDate ?: today

            val kmPart: Pair<MaintenanceStatus, String>? =
                if (intervalKm != null) {
                    val dueMileage = baseLastMileage + intervalKm
                    val remainingKm = dueMileage - carMileage
                    val soonKmThreshold = max(1, (intervalKm * SOON_RATIO).roundToInt())

                    val status = when {
                        remainingKm < 0 -> MaintenanceStatus.OVERDUE
                        remainingKm <= soonKmThreshold -> MaintenanceStatus.SOON
                        else -> MaintenanceStatus.NORMAL
                    }

                    val text = when {
                        remainingKm < 0 -> "초과 ${abs(remainingKm).formatKm()}km"
                        else -> "${remainingKm.formatKm()}km 남음"
                    }

                    status to text
                } else null

            val dayPart: Pair<MaintenanceStatus, String>? =
                if (intervalMonths != null) {
                    val dueDate = baseDateForCalc.plusMonths(intervalMonths.toLong())
                    val remainingDays = ChronoUnit.DAYS.between(today, dueDate)

                    val totalDaysOfCycle =
                        ChronoUnit.DAYS.between(baseDateForCalc, dueDate).coerceAtLeast(1)
                    val soonDaysThreshold =
                        max(1L, ceil(totalDaysOfCycle * SOON_RATIO).toLong())

                    val status = when {
                        remainingDays < 0 -> MaintenanceStatus.OVERDUE
                        remainingDays <= soonDaysThreshold -> MaintenanceStatus.SOON
                        else -> MaintenanceStatus.NORMAL
                    }

                    val text = when {
                        remainingDays < 0 -> "초과 ${abs(remainingDays)}일"
                        else -> "${remainingDays}일 남음"
                    }

                    status to text
                } else null

            val parts = listOfNotNull(kmPart, dayPart)
            if (parts.isEmpty()) return@mapNotNull null

            val finalStatus = when {
                parts.any { it.first == MaintenanceStatus.OVERDUE } -> MaintenanceStatus.OVERDUE
                parts.any { it.first == MaintenanceStatus.SOON } -> MaintenanceStatus.SOON
                else -> MaintenanceStatus.NORMAL
            }

            // 위험 목록은 SOON/OVERDUE만 노출
            if (finalStatus == MaintenanceStatus.NORMAL) return@mapNotNull null

            val remainingText = buildString {
                append(parts.joinToString(" · ") { it.second })
                // 원하면 “표시되는 경우에만” 첫기록 안내를 붙이기
                if (hasNoHistory) append(" · 첫 기록이 필요해요(0km/오늘 기준)")
            }

            MaintenanceUiModel(
                name = name,
                status = finalStatus,
                remainingText = remainingText
            )
        }
    }

    fun observeSort(carId: Long): Flow<MaintenanceSort> =
        carSortPrefRepository.observeSort(carId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getSettingsWithHistorySorted(
        carId: Long,
        sort: MaintenanceSort
    ): Flow<List<SettingWithHistory>> {   // ItemSettingWithHistory 말고 SettingWithHistory
        return when (sort) {
            MaintenanceSort.DEFAULT -> fullDao.getSettingsWithHistoryDefault(carId)
            MaintenanceSort.REMAINING_KM -> fullDao.getSettingsWithHistoryOrderByRemainingKm(carId)
            MaintenanceSort.DUE_DATE -> fullDao.getSettingsWithHistoryOrderByDueDate(carId)
            MaintenanceSort.URGENT_MIN -> fullDao.getSettingsWithHistoryOrderByCombined(carId)
        }
        // 이미 SettingWithHistory면 map { it.toDomain() } 필요 없음
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMaintenanceStatusList(carId: Long): Flow<List<MaintenanceUiModel>> {
        val carFlow = carDao.getCarById(carId) // Flow<CarEntity?>

        return observeSort(carId).flatMapLatest { sort ->
            val settingsFlow = getSettingsWithHistorySorted(carId, sort) // 정렬 반영

            combine(carFlow, settingsFlow) { carEntity, roomItems ->
                val carMileage = carEntity?.mileage ?: return@combine emptyList()

                val typeIds = roomItems.map { it.setting.maintenanceTypeId }.distinct()
                val typeMap = maintenanceTypeDao.getTypesByIds(typeIds).associateBy { it.id }

                buildMaintenanceUiModelsFromRoom(
                    carMileage = carMileage,
                    roomItems = roomItems,
                    typeMap = typeMap,
                    today = LocalDate.now()
                )
            }
        }
    }


    fun getSettingsWithHistory(carId: Long): Flow<List<ItemSettingWithHistory>> =
        fullDao.getSettingsWithHistory(carId)
            .map { list -> list.map { it.toDomain() } }


    fun observeSettingOptions(carId: Long): Flow<List<SettingOption>> =
        fullDao.observeSettingOptions(carId).map { list -> list.map { it.toDomain() } }

    suspend fun insertHistory(
        settingId: Long,
        serviceDate: String?,
        serviceMileage: Int?,
        place: String?,
        cost: Int?,
        memo: String?
    ) {
        maintenanceHistoryDao.insertHistory(
            MaintenanceHistoryEntity(
                settingId = settingId,
                serviceDate = serviceDate,
                serviceMileage = serviceMileage,
                place = place,
                cost = cost,
                memo = memo
            )
        )
    }

    fun observeSetting(settingId: Long): Flow<CarMaintenanceSetting?> =
        settingDao.observeBySettingId(settingId).map { it?.toDomain() }

    suspend fun updateSettingIntervals(settingId: Long, km: Int?, months: Int?) {
        settingDao.updateIntervals(settingId, km, months)
    }

    // Picker용: types + settings(비활성 포함) 결합해서 체크 상태 만들기
    fun observePickerItems(carId: Long): Flow<List<MaintenanceTypePickUi>> {
        return combine(
            maintenanceTypeDao.observeAll(),                       // Flow<List<MaintenanceTypeEntity>>
            settingDao.observeByCarIdIncludingInactive(carId) // Flow<List<CarMaintenanceSettingEntity>>
        ) { types, settings ->
            val byTypeId = settings.associateBy { it.maintenanceTypeId }

            types.map { t ->
                val s = byTypeId[t.id]
                MaintenanceTypePickUi(
                    typeId = t.id,
                    typeName = t.name,
                    defaultKm = t.defaultIntervalKm,
                    defaultMonths = t.defaultIntervalMonths,
                    checked = (s?.isActive == true),
                    settingId = s?.id
                )
            }.sortedBy { it.typeName }
        }
    }

    // 토글 로직(요청한 그대로)
    suspend fun setTypeEnabled(carId: Long, typeId: Long, enabled: Boolean) {
        val existing = settingDao.getOneByCarIdAndTypeId(carId, typeId)

        if (enabled) {
            if (existing != null) {
                // 있으면 enable
                settingDao.enableSetting(existing.id)
            } else {
                // 없으면 insert (interval은 null로 두면 기본주기 사용)
                settingDao.insertSetting(
                    CarMaintenanceSettingEntity(
                        carId = carId,
                        maintenanceTypeId = typeId,
                        intervalKm = null,
                        intervalMonths = null,
                        isActive = true
                    )
                )
            }
        } else {
            // 해제는 disable (내역 유지)
            if (existing != null) {
                settingDao.disableSetting(existing.id)
            }
        }
    }

    // Picker는 이걸 써야 복원으로 이동함
    fun observeAllByCarId(carId: Long): Flow<List<CarMaintenanceSetting>> =
        settingDao.observeAllByCarId(carId).map { it.map { e -> e.toDomain() } }

    // CarDetail 정렬/표시는 활성만 쓰고 싶다면 이걸 사용
    fun observeActiveByCarId(carId: Long): Flow<List<CarMaintenanceSetting>> =
        settingDao.observeActiveByCarId(carId).map { it.map { e -> e.toDomain() } }

    suspend fun setActive(settingId: Long, active: Boolean) = settingDao.setActive(settingId, active)

    suspend fun getByCarIdAndTypeIdOnce(carId: Long, typeId: Long): CarMaintenanceSetting? =
        settingDao.getByCarIdAndTypeIdOnce(carId, typeId)?.toDomain()

    /** 체크 ON인데 설정이 없을 때: 기본값(null)로 추가 + 활성 */
    suspend fun insertDefaultActive(carId: Long, typeId: Long): Long {
        return settingDao.insertSetting(
            CarMaintenanceSettingEntity(
                id = 0,
                carId = carId,
                maintenanceTypeId = typeId,
                intervalKm = null,
                intervalMonths = null,
                isActive = true
            )
        )
    }

    fun getHistoryById(id: Long) = maintenanceHistoryDao.getHistoryById(id)

    fun getHistoriesForSetting(settingId: Long) = maintenanceHistoryDao.getHistoriesForSetting(settingId)

    suspend fun updateHistory(entity: MaintenanceHistoryEntity) {
        maintenanceHistoryDao.updateHistory(entity)
    }

    suspend fun deleteHistory(historyId: Long) {
        maintenanceHistoryDao.deleteHistoryById(historyId)
    }

    suspend fun getCarIdBySettingId(settingId: Long): Long =
        settingDao.getCarIdBySettingId(settingId)

    suspend fun updateCarMileage(carId: Long, mileage: Int) {
        val now = System.currentTimeMillis()

        database.withTransaction {
            carDao.updateMileageWithTimestamp(
                carId = carId,
                mileage = mileage,
                updatedAt = now
            )

            mileageHistoryDao.insertHistory(
                MileageHistoryEntity(
                    carId = carId,
                    mileage = mileage,
                    recordedAt = now,
                    memo = "주행거리 업데이트"
                )
            )
        }
    }

    fun observeMaintenanceDigestForCarList(carId: Long): Flow<CarMaintenanceDigest> {
        val carFlow = carDao.getCarById(carId)
        val settingsFlow = fullDao.getSettingsWithHistoryOrderByCombined(carId) // URGENT_MIN 고정

        return combine(carFlow, settingsFlow) { carEntity, roomItems ->
            val carMileage = carEntity?.mileage ?: 0
            val today = LocalDate.now()

            val typeIds = roomItems.map { it.setting.maintenanceTypeId }.distinct()
            val typeMap = if (typeIds.isEmpty()) emptyMap()
            else maintenanceTypeDao.getTypesByIds(typeIds).associateBy { it.id }

            val dangerList = buildMaintenanceUiModelsFromRoom(
                carMileage = carMileage,
                roomItems = roomItems,
                typeMap = typeMap,
                today = today
            )

            val top = dangerList.firstOrNull()
            val summary = if (top == null) {
                MaintenanceSummaryUi(
                    status = MaintenanceStatus.NORMAL,
                    title = "상태 좋아요",
                    detail = "현재 정비할 항목이 없어요."
                )
            } else {
                MaintenanceSummaryUi(
                    status = top.status,
                    title = top.name,
                    detail = top.remainingText
                )
            }

            CarMaintenanceDigest(
                summary = summary,
                dangerCount = dangerList.size
            )
        }
    }

    suspend fun enableTypeForCar(
        carId: Long,
        typeId: Long,
        intervalKm: Int?,
        intervalMonths: Int?
    ) {
        val existing = settingDao.getOneByCarIdAndTypeId(carId, typeId)

        if (existing != null) {
            // 이미 있으면 활성화 + (선택) 주기 업데이트
            settingDao.enableSetting(existing.id)
            settingDao.updateIntervals(existing.id, intervalKm, intervalMonths)
        } else {
            // 없으면 새로 생성
            settingDao.insertSetting(
                CarMaintenanceSettingEntity(
                    id = 0,
                    carId = carId,
                    maintenanceTypeId = typeId,
                    intervalKm = intervalKm,
                    intervalMonths = intervalMonths,
                    isActive = true
                )
            )
        }
    }

    suspend fun addMaintenanceTypeAndEnableForCar(
        carId: Long,
        name: String,
        defaultKm: Int?,
        defaultMonths: Int?,
        // (선택) 이 차량만 별도 주기 쓰고 싶으면 넘김, 아니면 null/null
        carIntervalKm: Int?,
        carIntervalMonths: Int?
    ): Long {

        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "항목 이름이 비어있어요" }
        require(defaultKm != null || defaultMonths != null) { "기본 주기는 km/개월 중 하나는 입력해야 해요" }

        // 중복 타입이면 재사용
        val existing = maintenanceTypeDao.findByName(trimmed)
        val typeId = existing?.id ?: maintenanceTypeDao.insertType(
            MaintenanceTypeEntity(
                id = 0,
                name = trimmed,
                defaultIntervalKm = defaultKm,
                defaultIntervalMonths = defaultMonths
            )
        )

        // 이 차량에 즉시 활성화
        // 차량 전용 주기가 없으면 null로 두고 → 타입 기본값 사용
        enableTypeForCar(
            carId = carId,
            typeId = typeId,
            intervalKm = carIntervalKm,
            intervalMonths = carIntervalMonths
        )

        return typeId
    }

    suspend fun addMaintenanceTypeAndEnableForCarRejectDuplicate(
        carId: Long,
        name: String,
        defaultKm: Int?,
        defaultMonths: Int?,
        useCarOverride: Boolean,
        carIntervalKm: Int?,
        carIntervalMonths: Int?
    ): Long {

        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "항목 이름이 비어있어요" }
        require(defaultKm != null || defaultMonths != null) { "기본 주기는 km/개월 중 하나는 입력해야 해요" }

        // 중복 체크 (대소문자 무시)
        val existing = maintenanceTypeDao.findByName(trimmed)
        if (existing != null) {
            throw IllegalStateException("이미 존재하는 항목이에요. 다른 이름으로 추가해 주세요.")
        }

        // 새 타입 생성
        val typeId = maintenanceTypeDao.insertType(
            MaintenanceTypeEntity(
                id = 0,
                name = trimmed,
                defaultIntervalKm = defaultKm,
                defaultIntervalMonths = defaultMonths
            )
        )

        // 이 차량에 활성화
        enableTypeForCar(
            carId = carId,
            typeId = typeId,
            intervalKm = if (useCarOverride) carIntervalKm else null,
            intervalMonths = if (useCarOverride) carIntervalMonths else null
        )

        return typeId
    }



}