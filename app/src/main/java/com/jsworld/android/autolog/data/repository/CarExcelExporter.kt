package com.jsworld.android.autolog.data.repository

import androidx.annotation.StringRes
import com.jsworld.android.autolog.core.ui.asString
import com.jsworld.android.autolog.core.ui.LocalizedNames
import com.jsworld.android.autolog.R
import android.content.Context
import android.net.Uri
import com.jsworld.android.autolog.data.repository.CarExportData
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.repository.CarExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CarExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carExportRepository: CarExportRepository
) {

    private fun s(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    /** 저장된 한국어 원문 항목 이름 → 표시 언어 */
    private fun String.itemName(): String = LocalizedNames.item(this).asString(context)

    private data class MaintenanceHistoryExportRow(
        val typeName: String,
        val history: MaintenanceHistoryEntity
    )

    /**
     * 사용자가 선택한 Uri에 차량 정비 엑셀 파일 저장
     *
     * 이 방식은 ACTION_CREATE_DOCUMENT로 받은 Uri를 사용하므로
     * WRITE_EXTERNAL_STORAGE 권한이 필요 없습니다.
     */
    suspend fun exportCar(
        carId: Long,
        outputUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var workbook: XSSFWorkbook? = null

        try {
            val data = carExportRepository.getCarExportData(carId)
                ?: return@withContext Result.failure(
                    IllegalStateException("차량 내보내기 데이터를 찾을 수 없습니다. carId=$carId")
                )

            workbook = XSSFWorkbook()

            android.util.Log.d("CarExcelExporter", "createSummarySheet start")
            createSummarySheet(workbook, data)

            android.util.Log.d("CarExcelExporter", "createMaintenanceSettingSheet start")
            createMaintenanceSettingSheet(workbook, data)

            android.util.Log.d("CarExcelExporter", "createMaintenanceHistorySheet start")
            createMaintenanceHistorySheet(workbook, data)

            android.util.Log.d("CarExcelExporter", "createMileageHistorySheet start")
            createMileageHistorySheet(workbook, data)

            android.util.Log.d("CarExcelExporter", "createFuelSheet start")
            createFuelSheet(workbook, data)

            android.util.Log.d("CarExcelExporter", "createCareSheet start")
            createCareSheet(workbook, data)

            android.util.Log.d("CarExcelExporter", "write workbook start")

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                workbook.write(outputStream)
            } ?: return@withContext Result.failure(
                IllegalStateException("파일 저장 스트림을 열 수 없습니다. uri=$outputUri")
            )

            android.util.Log.d("CarExcelExporter", "export success")
            Result.success(Unit)

        } catch (t: Throwable) {
            android.util.Log.e("CarExcelExporter", "Excel export failed", t)
            Result.failure(t)
        } finally {
            try {
                workbook?.close()
            } catch (t: Throwable) {
                android.util.Log.e("CarExcelExporter", "Workbook close failed", t)
            }
        }
    }

    /**
     * 저장창에 보여줄 기본 파일명 생성
     */
    fun createDefaultFileName(carName: String): String {
        val safeCarName = carName.toSafeFileName()
        val nowText = System.currentTimeMillis().toFileDateText()

        return "${s(R.string.xl_file_prefix)}_${safeCarName}_${nowText}.xlsx"
    }

    /**
     * 시트 1: 요약
     *
     * 구성:
     * - 차량 기본 정보
     * - 정비 항목별 최근 정비 상태
     */
    private fun createSummarySheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        val sheet = workbook.createSheet(s(R.string.xl_sheet_summary))

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        // 제목
        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf(s(R.string.xl_summary_title)),
            style = titleStyle
        )

        // 차량 정보 섹션
        createRow(
            sheet = sheet,
            rowIndex = 2,
            values = listOf(s(R.string.xl_car_info)),
            style = sectionStyle
        )

        createRow(sheet, 3, listOf(s(R.string.xl_car_name), data.car.name))
        createRow(sheet, 4, listOf(s(R.string.xl_plate), data.car.plate))
        createRow(sheet, 5, listOf(s(R.string.car_year), data.car.year ?: ""))
        createRow(sheet, 6, listOf(s(R.string.xl_current_mileage), "${data.car.mileage} km"))
        createRow(sheet, 7, listOf(s(R.string.xl_fuel), LocalizedNames.fuelType(data.car.fuelType).asString(context)))
        createRow(sheet, 8, listOf(s(R.string.common_memo), data.car.notes ?: ""))
        createRow(sheet, 9, listOf(s(R.string.xl_primary), if (data.car.isPrimary) "Y" else "N"))
        createRow(
            sheet = sheet,
            rowIndex = 10,
            values = listOf(
                s(R.string.xl_last_mileage_update),
                data.car.lastMileageUpdatedAt?.toDateText() ?: ""
            )
        )

        // 정비 요약 섹션
        createRow(
            sheet = sheet,
            rowIndex = 12,
            values = listOf(s(R.string.xl_maint_summary)),
            style = sectionStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 13,
            values = listOf(
                s(R.string.xl_item),
                s(R.string.xl_last_date),
                s(R.string.xl_last_mileage),
                s(R.string.xl_cycle_km),
                s(R.string.xl_cycle_months),
                s(R.string.xl_next_mileage)
            ),
            style = headerStyle
        )

        data.settingsWithHistory.forEachIndexed { index, item ->
            val setting = item.setting
            val type = item.type

            val latestHistory = item.histories
                .sortedWith(
                    compareByDescending<MaintenanceHistoryEntity> {
                        it.serviceDate ?: ""
                    }.thenByDescending {
                        it.serviceMileage ?: 0
                    }
                )
                .firstOrNull()

            val intervalKm = setting.intervalKm ?: type.defaultIntervalKm
            val intervalMonths = setting.intervalMonths ?: type.defaultIntervalMonths
            val lastServiceMileage = latestHistory?.serviceMileage

            val nextServiceMileage =
                if (lastServiceMileage != null && intervalKm != null) {
                    lastServiceMileage + intervalKm
                } else {
                    null
                }

            createRow(
                sheet = sheet,
                rowIndex = 14 + index,
                values = listOf(
                    type.name.itemName(),
                    latestHistory?.serviceDate ?: "",
                    lastServiceMileage?.let { "$it km" } ?: "",
                    intervalKm?.let { "$it km" } ?: "",
                    intervalMonths?.let { s(R.string.xl_months_value, it) } ?: "",
                    nextServiceMileage?.let { "$it km" } ?: ""
                )
            )
        }

        setColumnWidths(sheet)
    }

    private fun createMaintenanceHistorySheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        val sheet = workbook.createSheet(s(R.string.xl_sheet_history))

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf(s(R.string.xl_sheet_history)),
            style = titleStyle
        )

        val allHistories = data.settingsWithHistory.flatMap { item ->
            item.histories.map { history ->
                MaintenanceHistoryExportRow(
                    typeName = item.type.name,
                    history = history
                )
            }
        }

        val totalCount = allHistories.size
        val totalCost = allHistories.sumOf { it.history.cost ?: 0 }

        val latestRow = allHistories
            .sortedWith(
                compareByDescending<MaintenanceHistoryExportRow> {
                    it.history.serviceDate ?: ""
                }.thenByDescending {
                    it.history.serviceMileage ?: 0
                }
            )
            .firstOrNull()

        // 전체 요약
        createRow(
            sheet = sheet,
            rowIndex = 2,
            values = listOf(s(R.string.xl_total_summary)),
            style = sectionStyle
        )

        createRow(sheet, 3, listOf(s(R.string.xl_total_count), s(R.string.count_cases, totalCount)))
        createRow(sheet, 4, listOf(s(R.string.xl_total_cost), s(R.string.money_amount, totalCost)))
        createRow(sheet, 5, listOf(s(R.string.xl_last_date), latestRow?.history?.serviceDate ?: ""))
        createRow(sheet, 6, listOf(s(R.string.xl_latest_item), latestRow?.typeName?.itemName() ?: ""))

        var rowIndex = 8

        if (allHistories.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = rowIndex,
                values = listOf(s(R.string.xl_no_history))
            )

            setMaintenanceHistoryColumnWidths(sheet)
            return
        }

        data.settingsWithHistory.forEach { item ->
            val typeName = item.type.name

            val histories = item.histories
                .sortedWith(
                    compareByDescending<MaintenanceHistoryEntity> {
                        it.serviceDate ?: ""
                    }.thenByDescending {
                        it.serviceMileage ?: 0
                    }
                )

            if (histories.isEmpty()) {
                return@forEach
            }

            // 정비항목 그룹 제목
            createRow(
                sheet = sheet,
                rowIndex = rowIndex,
                values = listOf("[${typeName.itemName()}]"),
                style = sectionStyle
            )
            rowIndex++

            // 그룹 헤더
            createRow(
                sheet = sheet,
                rowIndex = rowIndex,
                values = listOf(
                    s(R.string.xl_h_date),
                    s(R.string.xl_h_mileage),
                    s(R.string.xl_h_place),
                    s(R.string.common_cost),
                    s(R.string.common_memo)
                ),
                style = headerStyle
            )
            rowIndex++

            // 그룹 데이터
            histories.forEach { history ->
                createRow(
                    sheet = sheet,
                    rowIndex = rowIndex,
                    values = listOf(
                        history.serviceDate ?: "",
                        history.serviceMileage?.let { "$it km" } ?: "",
                        history.place ?: "",
                        history.cost?.let { s(R.string.money_amount, it) } ?: "",
                        history.memo ?: ""
                    )
                )
                rowIndex++
            }

            // 그룹 사이 공백
            rowIndex++
        }

        setMaintenanceHistoryColumnWidths(sheet)
    }

    private fun setMaintenanceHistoryColumnWidths(sheet: Sheet) {
        sheet.setColumnWidth(0, 18 * 256) // 정비일 / 항목
        sheet.setColumnWidth(1, 16 * 256) // 주행거리 / 값
        sheet.setColumnWidth(2, 22 * 256) // 정비장소
        sheet.setColumnWidth(3, 16 * 256) // 비용
        sheet.setColumnWidth(4, 35 * 256) // 메모
    }

    private fun createMileageHistorySheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        val sheet = workbook.createSheet(s(R.string.xl_sheet_mileage))

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf(s(R.string.xl_sheet_mileage)),
            style = titleStyle
        )

        val histories = data.mileageHistories
            .sortedByDescending { it.recordedAt }

        val latestHistory = histories.firstOrNull()
        val oldestHistory = histories.lastOrNull()

        val totalIncrease =
            if (latestHistory != null && oldestHistory != null) {
                latestHistory.mileage - oldestHistory.mileage
            } else {
                0
            }

        createRow(
            sheet = sheet,
            rowIndex = 2,
            values = listOf(s(R.string.xl_total_summary)),
            style = sectionStyle
        )

        createRow(sheet, 3, listOf(s(R.string.xl_m_total_count), s(R.string.count_cases, histories.size)))
        createRow(sheet, 4, listOf(s(R.string.xl_current_mileage), "${data.car.mileage} km"))
        createRow(sheet, 5, listOf(s(R.string.xl_m_latest_date), latestHistory?.recordedAt?.toRecordedAtText() ?: ""))
        createRow(sheet, 6, listOf(s(R.string.xl_m_latest_mileage), latestHistory?.mileage?.let { "$it km" } ?: ""))
        createRow(sheet, 7, listOf(s(R.string.xl_m_total_increase), "${totalIncrease.coerceAtLeast(0)} km"))

        createRow(
            sheet = sheet,
            rowIndex = 9,
            values = listOf(s(R.string.xl_m_section)),
            style = sectionStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 10,
            values = listOf(
                s(R.string.xl_m_recorded_date),
                s(R.string.xl_h_mileage),
                s(R.string.xl_m_increase),
                s(R.string.common_memo)
            ),
            style = headerStyle
        )

        if (histories.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = 11,
                values = listOf(s(R.string.xl_m_empty), "", "", "")
            )

            setMileageHistoryColumnWidths(sheet)
            return
        }

        histories.forEachIndexed { index, history ->
            val previousOlderHistory = histories.getOrNull(index + 1)

            val increasedMileage =
                if (previousOlderHistory != null) {
                    history.mileage - previousOlderHistory.mileage
                } else {
                    null
                }

            createRow(
                sheet = sheet,
                rowIndex = 11 + index,
                values = listOf(
                    history.recordedAt.toRecordedAtText(),
                    "${history.mileage} km",
                    increasedMileage?.let { "${it.coerceAtLeast(0)} km" } ?: "",
                    history.memo ?: ""
                )
            )
        }

        setMileageHistoryColumnWidths(sheet)
    }

    private fun setMileageHistoryColumnWidths(sheet: Sheet) {
        sheet.setColumnWidth(0, 22 * 256) // 기록일
        sheet.setColumnWidth(1, 18 * 256) // 주행거리
        sheet.setColumnWidth(2, 22 * 256) // 이전 기록 대비 증가
        sheet.setColumnWidth(3, 35 * 256) // 메모
    }

    /**
     * 주유(충전) 기록 시트.
     *
     * 연비는 계산하지 않는 대신 "이전 기록 이후 주행거리"를 함께 적어
     * 스프레드시트에서 사용자가 직접 따져볼 수 있게 한다.
     */
    private fun createFuelSheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        if (data.fuelRecords.isEmpty()) {
            createFuelSheetForUnit(workbook, "L", emptyList(), emptyMap())
            return
        }

        // ⚠️ 단위(L / kWh / kg)가 섞인 기록을 한 시트에 담으면
        // "총 주유량 = 22.6L + 28.4kWh" 처럼 말이 안 되는 합계가 나온다.
        // 플러그인 하이브리드는 항상 섞이므로 **종류별로 시트를 나눈다.**
        // 종류가 하나면 시트도 하나라 기존과 똑같다.
        // "이전 기록 이후 주행"은 종류를 섞어 계산해야 한다.
        // 주유든 충전이든 그 사이에 실제로 달린 거리이기 때문이다.
        val drivenById = data.fuelRecords.drivenDistancesById()

        data.fuelRecords
            .groupBy { it.unit }
            .toList()
            .sortedBy { (unit, _) -> if (unit == "L") 0 else 1 }
            .forEach { (unitLabel, unitRecords) ->
                createFuelSheetForUnit(workbook, unitLabel, unitRecords, drivenById)
            }
    }

    private fun createFuelSheetForUnit(
        workbook: XSSFWorkbook,
        unitLabel: String,
        unitRecords: List<FuelRecordEntity>,
        drivenById: Map<Long, Int>
    ) {
        val actionLabel = s(if (unitLabel == "L") R.string.fuel_action_refuel else R.string.fuel_action_charge)
        val placeLabel = s(if (unitLabel == "L") R.string.fuel_place_refuel else R.string.fuel_place_charge)

        val sheet = workbook.createSheet(s(R.string.xl_sheet_fuel, actionLabel))

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(sheet, 0, listOf(s(R.string.xl_sheet_fuel, actionLabel)), titleStyle)

        // 최신순으로 들어오지만 방어적으로 다시 정렬한다.
        val sorted = unitRecords.sortedWith(
            compareByDescending<FuelRecordEntity> { it.filledAt }.thenByDescending { it.id }
        )

        val totalAmount = sorted.sumOf { it.amount ?: 0 }
        val totalQuantity = sorted.sumOf { it.quantity ?: 0.0 }
        // 이 시트는 단위가 하나뿐이라 합계·평균이 성립한다.
        val averageUnitPrice =
            if (totalQuantity > 0.0) (totalAmount / totalQuantity).toInt() else null

        createRow(sheet, 2, listOf(s(R.string.xl_total_summary)), sectionStyle)
        createRow(sheet, 3, listOf(s(R.string.xl_m_total_count), s(R.string.count_cases, sorted.size)))
        createRow(sheet, 4, listOf(s(R.string.xl_f_total_spend), s(R.string.xl_won_value, totalAmount)))
        createRow(sheet, 5, listOf(s(R.string.xl_f_total_qty, actionLabel), "$totalQuantity $unitLabel"))
        createRow(
            sheet, 6,
            listOf(s(R.string.fuel_avg_price), averageUnitPrice?.let { s(R.string.xl_price_value, it, unitLabel) } ?: "")
        )

        createRow(sheet, 8, listOf(s(R.string.xl_f_section, actionLabel)), sectionStyle)

        createRow(
            sheet = sheet,
            rowIndex = 9,
            values = listOf(
                s(R.string.common_date),
                s(R.string.xl_h_mileage),
                s(R.string.xl_f_driven_since),
                s(R.string.xl_f_amount),
                s(R.string.xl_f_qty, actionLabel),
                s(R.string.fuel_unit_price),
                placeLabel,
                s(R.string.common_memo)
            ),
            style = headerStyle
        )

        if (sorted.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = 10,
                values = listOf(s(R.string.xl_f_empty, actionLabel), "", "", "", "", "", "", "")
            )
            setFuelColumnWidths(sheet)
            return
        }

        sorted.forEachIndexed { index, record ->
            createRow(
                sheet = sheet,
                rowIndex = 10 + index,
                values = listOf(
                    record.filledAt,
                    record.mileage?.let { "$it km" } ?: "",
                    drivenById[record.id]?.let { "$it km" } ?: "",
                    record.amount?.let { s(R.string.xl_won_value, it) } ?: "",
                    record.quantity?.let { "$it $unitLabel" } ?: "",
                    record.unitPrice?.let { s(R.string.xl_price_value, it, unitLabel) } ?: "",
                    record.station ?: "",
                    record.memo ?: ""
                )
            )
        }

        setFuelColumnWidths(sheet)
    }

    /**
     * 각 기록의 "이전 기록 이후 주행거리". 종류(주유/충전)를 섞어 최신순으로 비교한다.
     */
    private fun List<FuelRecordEntity>.drivenDistancesById(): Map<Long, Int> {
        val sorted = sortedWith(
            compareByDescending<FuelRecordEntity> { it.filledAt }.thenByDescending { it.id }
        )
        val result = mutableMapOf<Long, Int>()
        sorted.forEachIndexed { index, record ->
            val current = record.mileage ?: return@forEachIndexed
            val previous = sorted.getOrNull(index + 1)?.mileage ?: return@forEachIndexed
            val delta = current - previous
            if (delta > 0) result[record.id] = delta
        }
        return result
    }

    private fun setFuelColumnWidths(sheet: Sheet) {
        sheet.setColumnWidth(0, 14 * 256) // 날짜
        sheet.setColumnWidth(1, 14 * 256) // 주행거리
        sheet.setColumnWidth(2, 20 * 256) // 이전 기록 이후 주행
        sheet.setColumnWidth(3, 14 * 256) // 금액
        sheet.setColumnWidth(4, 14 * 256) // 주유량
        sheet.setColumnWidth(5, 16 * 256) // 단가
        sheet.setColumnWidth(6, 22 * 256) // 주유소
        sheet.setColumnWidth(7, 30 * 256) // 메모
    }

    /** 세차·관리 기록 — 데이터가 있을 때만 시트를 만든다 */
    private fun createCareSheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        if (data.careRecords.isEmpty()) return

        val sheet = workbook.createSheet(s(R.string.xl_sheet_care))
        val headerStyle = createHeaderStyle(workbook)

        var rowIndex = 0
        createRow(
            sheet, rowIndex++,
            listOf(s(R.string.common_date), s(R.string.xl_c_item), s(R.string.xl_c_method), s(R.string.common_place), s(R.string.xl_c_cost), s(R.string.common_memo)),
            headerStyle
        )

        data.careRecords.forEach { record ->
            createRow(
                sheet, rowIndex++,
                listOf(
                    record.performedAt ?: "",
                    record.itemName.itemName(),
                    record.method?.let { LocalizedNames.washMethod(it).asString(context) } ?: "",
                    record.place ?: "",
                    record.cost?.toString() ?: "",
                    record.memo ?: ""
                )
            )
        }

        setColumnWidths(sheet)
    }

    private fun createMaintenanceSettingSheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        val sheet = workbook.createSheet(s(R.string.xl_sheet_settings))

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf(s(R.string.xl_sheet_settings)),
            style = titleStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 2,
            values = listOf(s(R.string.xl_s_section)),
            style = sectionStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 3,
            values = listOf(
                s(R.string.xl_item),
                s(R.string.xl_s_car_km),
                s(R.string.xl_s_car_months),
                s(R.string.xl_s_default_km),
                s(R.string.xl_s_default_months),
                s(R.string.xl_s_applied_km),
                s(R.string.xl_s_applied_months),
                s(R.string.xl_s_active)
            ),
            style = headerStyle
        )

        if (data.settingsWithHistory.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = 4,
                values = listOf(s(R.string.xl_s_empty), "", "", "", "", "", "", "")
            )

            setMaintenanceSettingColumnWidths(sheet)
            return
        }

        data.settingsWithHistory.forEachIndexed { index, item ->
            val setting = item.setting
            val type = item.type

            val appliedIntervalKm = setting.intervalKm ?: type.defaultIntervalKm
            val appliedIntervalMonths = setting.intervalMonths ?: type.defaultIntervalMonths

            createRow(
                sheet = sheet,
                rowIndex = 4 + index,
                values = listOf(
                    type.name.itemName(),
                    setting.intervalKm?.let { "$it km" } ?: "",
                    setting.intervalMonths?.let { s(R.string.xl_months_value, it) } ?: "",
                    type.defaultIntervalKm?.let { "$it km" } ?: "",
                    type.defaultIntervalMonths?.let { s(R.string.xl_months_value, it) } ?: "",
                    appliedIntervalKm?.let { "$it km" } ?: "",
                    appliedIntervalMonths?.let { s(R.string.xl_months_value, it) } ?: "",
                    s(if (setting.isActive) R.string.xl_used else R.string.xl_unused)
                )
            )
        }

        setMaintenanceSettingColumnWidths(sheet)
    }

    private fun setMaintenanceSettingColumnWidths(sheet: Sheet) {
        sheet.setColumnWidth(0, 20 * 256) // 정비항목
        sheet.setColumnWidth(1, 18 * 256) // 차량별 주기 km
        sheet.setColumnWidth(2, 18 * 256) // 차량별 주기 개월
        sheet.setColumnWidth(3, 18 * 256) // 기본 주기 km
        sheet.setColumnWidth(4, 18 * 256) // 기본 주기 개월
        sheet.setColumnWidth(5, 18 * 256) // 적용 주기 km
        sheet.setColumnWidth(6, 18 * 256) // 적용 주기 개월
        sheet.setColumnWidth(7, 12 * 256) // 사용 여부
    }

    private fun createRow(
        sheet: Sheet,
        rowIndex: Int,
        values: List<String>,
        style: CellStyle? = null
    ) {
        val row = sheet.createRow(rowIndex)

        values.forEachIndexed { columnIndex, value ->
            val cell = row.createCell(columnIndex)
            cell.setCellValue(value)

            if (style != null) {
                cell.cellStyle = style
            }
        }
    }

    private fun createTitleStyle(workbook: XSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        font.bold = true
        font.fontHeightInPoints = 16

        style.setFont(font)

        return style
    }

    private fun createSectionStyle(workbook: XSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        font.bold = true
        font.fontHeightInPoints = 13

        style.setFont(font)

        return style
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        font.bold = true

        style.setFont(font)
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER

        return style
    }


    private fun setColumnWidths(sheet: Sheet) {
        sheet.setColumnWidth(0, 20 * 256) // 정비항목 / 항목명
        sheet.setColumnWidth(1, 22 * 256) // 최근 정비일 / 값
        sheet.setColumnWidth(2, 18 * 256) // 최근 정비거리
        sheet.setColumnWidth(3, 18 * 256) // 교체주기 km
        sheet.setColumnWidth(4, 18 * 256) // 교체주기 개월
        sheet.setColumnWidth(5, 18 * 256) // 다음 교체거리
    }

    private fun String.toSafeFileName(): String {
        return this
            .replace("/", "_")
            .replace("\\", "_")
            .replace(":", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("\"", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")
            .trim()
            .ifEmpty { s(R.string.xl_car_fallback) }
    }

    private fun Long.toDateText(): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        )

        return formatter.format(Date(this))
    }

    /**
     * recordedAt = 0 은 날짜가 아니라 "차량 등록 시점"이라는 표식이다
     * (CarRepositoryImpl.addCar — 등록일을 모르는 최초 기준점을 가장 이른 시점에 둔다).
     * 그대로 날짜로 찍으면 1970-01-01 로 보인다.
     */
    private fun Long.toRecordedAtText(): String =
        if (this == 0L) s(R.string.xl_registration_time) else toDateText()

    private fun Long.toFileDateText(): String {
        val formatter = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        )

        return formatter.format(Date(this))
    }
}