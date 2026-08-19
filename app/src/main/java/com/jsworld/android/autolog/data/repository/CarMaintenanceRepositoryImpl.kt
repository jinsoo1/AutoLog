package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.domain.repository.CarSortPreferenceRepository

import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository

import androidx.room.withTransaction
import com.jsworld.android.autolog.domain.model.CarMaintenanceDigest
import com.jsworld.android.autolog.domain.model.CarMaintenanceSetting
import com.jsworld.android.autolog.domain.model.MaintenanceSort
import com.jsworld.android.autolog.domain.model.MaintenanceStarterPack
import com.jsworld.android.autolog.domain.model.isItemApplicableToFuel
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceSummaryUi
import com.jsworld.android.autolog.domain.model.MaintenanceTypePickUi
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.SettingOption
import com.jsworld.android.autolog.data.local.dao.CarDao
import com.jsworld.android.autolog.data.local.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceFullDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.data.local.dao.MileageHistoryDao
import com.jsworld.android.autolog.data.local.db.AutoLogDatabase
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity
import com.jsworld.android.autolog.data.mapper.toDomain
import com.jsworld.android.autolog.data.local.entity.SettingWithHistory
import com.jsworld.android.autolog.domain.model.SettingWithHistory as ItemSettingWithHistory
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
class CarMaintenanceRepositoryImpl @Inject constructor(
    private val database: AutoLogDatabase,
    private val fullDao: MaintenanceFullDao,
    private val maintenanceTypeDao: MaintenanceTypeDao,
    private val carDao: CarDao, // car mileage 가져오려면 필요(또는 CarRepository 사용)
    private val settingDao: CarMaintenanceSettingDao,
    private val maintenanceHistoryDao: MaintenanceHistoryDao,
    private val mileageHistoryDao: MileageHistoryDao,
    private val carSortPrefRepository: CarSortPreferenceRepository
) : CarMaintenanceRepository {

    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
    private fun String.toLocalDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this, ISO) }.getOrNull()
    private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)


    override fun getMaintenanceSettingsSorted(carId: Long, sort: MaintenanceSort): Flow<List<CarMaintenanceSetting>> {
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

            // 예측·정렬에 쓰라고 숫자로도 남긴다(아래 kmPart/dayPart 는 표시 문장을 만든다).
            val remainingKmValue = intervalKm?.let { (baseLastMileage + it) - carMileage }
            val remainingDaysValue = intervalMonths?.let {
                ChronoUnit.DAYS.between(today, baseDateForCalc.plusMonths(it.toLong()))
            }

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

            val remainingText = buildString {
                append(parts.joinToString(" · ") { it.second })
                // 첫 기록이 없으면 계산 기준(0km/오늘)을 밝혀준다 — 임박한 항목에만 붙인다.
                if (hasNoHistory && finalStatus != MaintenanceStatus.NORMAL) {
                    append(" · 첫 기록이 필요해요(0km/오늘 기준)")
                }
            }

            // 주기 소진율 — km·개월 둘 다 있으면 더 많이 소진된 쪽을 쓴다(상태 판정과 같은 기준).
            val kmRatio = if (intervalKm != null && intervalKm > 0) {
                ((carMileage - baseLastMileage).toFloat() / intervalKm).coerceIn(0f, 1f)
            } else null

            val dayRatio = if (intervalMonths != null) {
                val dueDate = baseDateForCalc.plusMonths(intervalMonths.toLong())
                val total = ChronoUnit.DAYS.between(baseDateForCalc, dueDate).coerceAtLeast(1)
                val used = ChronoUnit.DAYS.between(baseDateForCalc, today)
                (used.toFloat() / total).coerceIn(0f, 1f)
            } else null

            MaintenanceUiModel(
                settingId = setting.id,
                name = name,
                status = finalStatus,
                remainingText = remainingText,
                progressRatio = listOfNotNull(kmRatio, dayRatio).maxOrNull(),
                hasHistory = !hasNoHistory,
                remainingKm = remainingKmValue,
                remainingDays = remainingDaysValue
            )
        }
    }

    override fun observeSort(carId: Long): Flow<MaintenanceSort> =
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
    override fun observeMaintenanceStatusList(carId: Long): Flow<List<MaintenanceUiModel>> {
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
                ).filter { it.status != MaintenanceStatus.NORMAL }
            }
        }
    }

    /**
     * 정상 항목까지 포함한 전체 정비 상태. 임박한 순서(URGENT_MIN)로 정렬된다.
     * 홈 탭의 "다음 정비", 항목 상세의 진행률이 이걸 쓴다.
     */
    override fun observeMaintenanceOverview(carId: Long): Flow<List<MaintenanceUiModel>> {
        val carFlow = carDao.getCarById(carId)
        val settingsFlow = fullDao.getSettingsWithHistoryOrderByCombined(carId)

        return combine(carFlow, settingsFlow) { carEntity, roomItems ->
            val carMileage = carEntity?.mileage ?: return@combine emptyList()

            val typeIds = roomItems.map { it.setting.maintenanceTypeId }.distinct()
            val typeMap = if (typeIds.isEmpty()) emptyMap()
            else maintenanceTypeDao.getTypesByIds(typeIds).associateBy { it.id }

            buildMaintenanceUiModelsFromRoom(
                carMileage = carMileage,
                roomItems = roomItems,
                typeMap = typeMap,
                today = LocalDate.now()
            )
        }
    }


    override fun getSettingsWithHistory(carId: Long): Flow<List<ItemSettingWithHistory>> =
        fullDao.getSettingsWithHistory(carId)
            .map { list -> list.map { it.toDomain() } }


    override fun observeSettingOptions(carId: Long): Flow<List<SettingOption>> =
        fullDao.observeSettingOptions(carId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertHistory(
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

    override fun observeSetting(settingId: Long): Flow<CarMaintenanceSetting?> =
        settingDao.observeBySettingId(settingId).map { it?.toDomain() }

    override suspend fun updateSettingIntervals(settingId: Long, km: Int?, months: Int?) {
        settingDao.updateIntervals(settingId, km, months)
    }

    // Picker용: types + settings(비활성 포함) 결합해서 체크 상태 만들기
    override fun observePickerItems(carId: Long): Flow<List<MaintenanceTypePickUi>> {
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
    override suspend fun setTypeEnabled(carId: Long, typeId: Long, enabled: Boolean) {
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
    override fun observeAllByCarId(carId: Long): Flow<List<CarMaintenanceSetting>> =
        settingDao.observeAllByCarId(carId).map { it.map { e -> e.toDomain() } }

    // CarDetail 정렬/표시는 활성만 쓰고 싶다면 이걸 사용
    override fun observeActiveByCarId(carId: Long): Flow<List<CarMaintenanceSetting>> =
        settingDao.observeActiveByCarId(carId).map { it.map { e -> e.toDomain() } }

    override suspend fun setActive(settingId: Long, active: Boolean) = settingDao.setActive(settingId, active)

    override suspend fun getByCarIdAndTypeIdOnce(carId: Long, typeId: Long): CarMaintenanceSetting? =
        settingDao.getByCarIdAndTypeIdOnce(carId, typeId)?.toDomain()

    /** 체크 ON인데 설정이 없을 때: 기본값(null)로 추가 + 활성 */
    override suspend fun insertDefaultActive(carId: Long, typeId: Long): Long {
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

    override fun getHistoryById(id: Long) = maintenanceHistoryDao.getHistoryById(id)

    override fun getHistoriesForSetting(settingId: Long) = maintenanceHistoryDao.getHistoriesForSetting(settingId)

    override suspend fun updateHistory(entity: MaintenanceHistoryEntity) {
        maintenanceHistoryDao.updateHistory(entity)
    }

    override suspend fun deleteHistory(historyId: Long) {
        maintenanceHistoryDao.deleteHistoryById(historyId)
    }

    override suspend fun getCarIdBySettingId(settingId: Long): Long =
        settingDao.getCarIdBySettingId(settingId)

    override suspend fun updateCarMileage(carId: Long, mileage: Int) {
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

    override fun observeMaintenanceDigestForCarList(carId: Long): Flow<CarMaintenanceDigest> {
        val carFlow = carDao.getCarById(carId)
        val settingsFlow = fullDao.getSettingsWithHistoryOrderByCombined(carId) // URGENT_MIN 고정

        return combine(carFlow, settingsFlow) { carEntity, roomItems ->
            val carMileage = carEntity?.mileage ?: 0
            val today = LocalDate.now()

            val typeIds = roomItems.map { it.setting.maintenanceTypeId }.distinct()
            val typeMap = if (typeIds.isEmpty()) emptyMap()
            else maintenanceTypeDao.getTypesByIds(typeIds).associateBy { it.id }

            // 기록 없는 항목은 0km/오늘 기준 계산이라 가짜 초과로 뜬다 —
            // 홈·리포트·알림과 같은 원칙으로 위험 요약에서 제외한다.
            val dangerList = buildMaintenanceUiModelsFromRoom(
                carMileage = carMileage,
                roomItems = roomItems,
                typeMap = typeMap,
                today = today
            ).filter { it.status != MaintenanceStatus.NORMAL && it.hasHistory }

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

    override suspend fun enableTypeForCar(
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

    override suspend fun addMaintenanceTypeAndEnableForCar(
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

    override suspend fun applyStarterPack(
        carId: Long,
        pack: MaintenanceStarterPack,
        fuelType: String?
    ): Int {
        val names = when (pack) {
            MaintenanceStarterPack.LIGHT -> DefaultMaintenanceItems.lightPack
            MaintenanceStarterPack.STANDARD -> DefaultMaintenanceItems.standardPack
            MaintenanceStarterPack.FULL -> DefaultMaintenanceItems.fullPack
        }

        var enabled = 0
        for (name in names) {
            // 전기차에 엔진오일을 켜는 일이 없도록 연료 타입 필터를 여기서 건다.
            if (!isItemApplicableToFuel(name, fuelType)) continue
            val type = maintenanceTypeDao.findByName(name) ?: continue
            // 주기는 넘기지 않는다 → 항목 기본 주기를 그대로 쓴다.
            enableTypeForCar(carId, type.id, intervalKm = null, intervalMonths = null)
            enabled++
        }
        return enabled
    }

    override suspend fun getOrCreateRepairSetting(carId: Long, name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "수리 이름이 비어있어요" }

        // 같은 이름의 항목이 있으면 재사용한다 — 이름 unique 제약 때문이기도 하지만,
        // 같은 부품을 다시 수리했을 때 이력이 한 항목에 쌓이는 게 맞다.
        // (그 항목에 주기가 있다면 주기 정비로 기록되는데, 그것도 올바른 동작이다)
        val typeId = maintenanceTypeDao.findByName(trimmed)?.id
            ?: maintenanceTypeDao.insertType(
                MaintenanceTypeEntity(
                    id = 0,
                    name = trimmed,
                    // 주기 없음 = 수리. 임박 계산에서 제외된다.
                    defaultIntervalKm = null,
                    defaultIntervalMonths = null
                )
            )

        val existing = settingDao.getOneByCarIdAndTypeId(carId, typeId)
        if (existing != null) {
            if (!existing.isActive) settingDao.setActive(existing.id, true)
            return existing.id
        }

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

    override suspend fun addMaintenanceTypeAndEnableForCarRejectDuplicate(
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