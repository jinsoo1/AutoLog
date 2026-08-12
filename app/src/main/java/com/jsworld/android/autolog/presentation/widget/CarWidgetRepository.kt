package com.jsworld.android.autolog.presentation.widget

import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.data.local.dao.CarDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceFullDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceTypeDao
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private const val SOON_RATIO = 0.15f

@Singleton
class CarWidgetRepository @Inject constructor(
    private val carDao: CarDao,
    private val fullDao: MaintenanceFullDao,
    private val maintenanceTypeDao: MaintenanceTypeDao
) {
    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
    private fun String.toLocalDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this, ISO) }.getOrNull()

    private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)

    suspend fun getCarWidgetUiOnce(carId: Long, maxRows: Int = 4): CarWidgetUi {
        val car = carDao.getCarById(carId).first() ?: run {
            return CarWidgetUi("차량", "-", 0, MaintenanceStatus.NORMAL, 0, emptyList())
        }

        val carMileage = car.mileage
        val today = LocalDate.now()

        // 위젯은 차량별 정렬 설정 말고 "가장 급한 순" 고정이 보기 좋음
        val roomItems = fullDao.getSettingsWithHistoryOrderByCombined(carId).first()

        val typeIds = roomItems.map { it.setting.maintenanceTypeId }.distinct()
        val typeMap = if (typeIds.isEmpty()) emptyMap()
        else maintenanceTypeDao.getTypesByIds(typeIds).associateBy { it.id }

        val rowsAllWithKey: List<RowWithKey> = roomItems.mapNotNull { item ->
            val setting = item.setting
            val type = typeMap[setting.maintenanceTypeId] ?: return@mapNotNull null

            val intervalKm = setting.intervalKm ?: type.defaultIntervalKm
            val intervalMonths = setting.intervalMonths ?: type.defaultIntervalMonths
            if (intervalKm == null && intervalMonths == null) return@mapNotNull null

            val lastMileage = item.history.mapNotNull { it.serviceMileage }.maxOrNull()
            val lastDate = item.history.mapNotNull { it.serviceDate?.toLocalDateOrNull() }.maxOrNull()

            // 기록이 없으면 0km/오늘 기준 계산이라 가짜 초과가 된다 —
            // 홈·리포트·알림과 같은 원칙으로 위험 대신 중립 행으로 안내만 한다.
            if (lastMileage == null && lastDate == null) {
                return@mapNotNull RowWithKey(
                    row = MaintenanceProgressRow(
                        name = type.name,
                        status = MaintenanceStatus.NORMAL,
                        progress = 0f,
                        remainText = "첫 기록 필요"
                    ),
                    statusRank = 2,
                    urgentKey = Long.MAX_VALUE // 정상 항목들보다도 뒤로
                )
            }

            val baseLastMileage = lastMileage ?: 0
            val baseDateForCalc = lastDate ?: today

            // remaining 계산(정렬에 필요)
            val remainingKm: Int? = if (intervalKm != null && intervalKm > 0) {
                val dueMileage = baseLastMileage + intervalKm
                dueMileage - carMileage
            } else null

            val remainingDays: Long? = if (intervalMonths != null && intervalMonths > 0) {
                val dueDate = baseDateForCalc.plusMonths(intervalMonths.toLong())
                ChronoUnit.DAYS.between(today, dueDate)
            } else null

            // status 결정(기존 로직 유지)
            val soonKmThreshold = intervalKm?.let { max(1, (it * SOON_RATIO).roundToInt()) }
            val kmStatus: MaintenanceStatus? = remainingKm?.let { r ->
                when {
                    r < 0 -> MaintenanceStatus.OVERDUE
                    soonKmThreshold != null && r <= soonKmThreshold -> MaintenanceStatus.SOON
                    else -> MaintenanceStatus.NORMAL
                }
            }

            val dayStatus: MaintenanceStatus? = remainingDays?.let { r ->
                val dueDate = baseDateForCalc.plusMonths(intervalMonths!!.toLong())
                val totalDays = ChronoUnit.DAYS.between(baseDateForCalc, dueDate).coerceAtLeast(1)
                val soonDaysThreshold = max(1L, ceil(totalDays * SOON_RATIO).toLong())
                when {
                    r < 0 -> MaintenanceStatus.OVERDUE
                    r <= soonDaysThreshold -> MaintenanceStatus.SOON
                    else -> MaintenanceStatus.NORMAL
                }
            }

            val finalStatus = when {
                kmStatus == MaintenanceStatus.OVERDUE || dayStatus == MaintenanceStatus.OVERDUE -> MaintenanceStatus.OVERDUE
                kmStatus == MaintenanceStatus.SOON || dayStatus == MaintenanceStatus.SOON -> MaintenanceStatus.SOON
                else -> MaintenanceStatus.NORMAL
            }

            // progress/텍스트(기존 유지, 여기선 생략 가능)
            val finalProgress = listOfNotNull(
                // kmProgress
                if (intervalKm != null && intervalKm > 0) {
                    val used = (carMileage - baseLastMileage).coerceAtLeast(0)
                    val p = used.toFloat() / intervalKm.toFloat()
                    if (finalStatus == MaintenanceStatus.OVERDUE) 1f else p.coerceIn(0f, 1f)
                } else null,
                // dayProgress
                if (intervalMonths != null && intervalMonths > 0) {
                    val dueDate = baseDateForCalc.plusMonths(intervalMonths.toLong())
                    val totalDays = ChronoUnit.DAYS.between(baseDateForCalc, dueDate).coerceAtLeast(1)
                    val usedDays = ChronoUnit.DAYS.between(baseDateForCalc, today).coerceAtLeast(0)
                    val p = usedDays.toFloat() / totalDays.toFloat()
                    if (finalStatus == MaintenanceStatus.OVERDUE) 1f else p.coerceIn(0f, 1f)
                } else null
            ).maxOrNull() ?: 0f

            val kmRemainText = remainingKm?.let { if (it < 0) "초과 ${abs(it).formatKm()}km" else "잔여 ${it.formatKm()}km" }
            val dayRemainText = remainingDays?.let { if (it < 0) "초과 ${kotlin.math.abs(it)}일" else "잔여 ${it}일" }
            val remainText = listOfNotNull(kmRemainText, dayRemainText).joinToString(" · ").ifBlank { "-" }

            val row = MaintenanceProgressRow(
                name = type.name,
                status = finalStatus,
                progress = finalProgress,
                remainText = remainText
            )

            // 정렬 키 계산:
            // OVERDUE면 abs(초과량) 기준으로 "작을수록" 먼저(최근 초과 우선)
            // SOON/NORMAL이면 잔여가 "작을수록" 먼저
            val kmKey = remainingKm?.let { r -> (if (r < 0) abs(r) else r).toLong() }
            val dayKey = remainingDays?.let { r -> if (r < 0) kotlin.math.abs(r) else r }
            val urgentKey = listOfNotNull(kmKey, dayKey).minOrNull() ?: Long.MAX_VALUE

            val statusRank = when (finalStatus) {
                MaintenanceStatus.OVERDUE -> 0
                MaintenanceStatus.SOON -> 1
                MaintenanceStatus.NORMAL -> 2
            }

            RowWithKey(
                row = row,
                statusRank = statusRank,
                urgentKey = urgentKey
            )
        }

        // 위젯은 너무 많으면 지저분 → TOP N
        val rows = rowsAllWithKey
            .sortedWith(
                compareBy<RowWithKey> { it.statusRank }
                    .thenBy { it.urgentKey }        // 남은 거리/일 중 더 급한 값
                    .thenBy { it.row.name }         // 마지막 타이브레이커
            )
            .map { it.row }
            .take(maxRows)

        val dangerCount = rowsAllWithKey.count { it.row.status != MaintenanceStatus.NORMAL }

        val overall = when {
            rowsAllWithKey.any { it.row.status == MaintenanceStatus.OVERDUE } -> MaintenanceStatus.OVERDUE
            rowsAllWithKey.any { it.row.status == MaintenanceStatus.SOON } -> MaintenanceStatus.SOON
            else -> MaintenanceStatus.NORMAL
        }

        return CarWidgetUi(
            carName = car.name,
            plate = car.plate,
            mileage = carMileage,
            overallStatus = overall,
            dangerCount = dangerCount,
            rows = rows
        )
    }
}