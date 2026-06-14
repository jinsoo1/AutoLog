package com.jsworld.android.autolog.ui.data.export

import android.content.Context
import android.net.Uri
import com.jsworld.android.autolog.ui.data.item.CarExportData
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.ui.data.room.repository.CarExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
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
    ): Boolean = withContext(Dispatchers.IO) {
        val data = carExportRepository.getCarExportData(carId)
            ?: return@withContext false

        val workbook = XSSFWorkbook()

        try {
            createSummarySheet(
                workbook = workbook,
                data = data
            )

            createMaintenanceSettingSheet(
                workbook = workbook,
                data = data
            )

            createMaintenanceHistorySheet(
                workbook = workbook,
                data = data
            )

            createMileageHistorySheet(
                workbook = workbook,
                data = data
            )

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                workbook.write(outputStream)
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            workbook.close()
        }
    }

    /**
     * 저장창에 보여줄 기본 파일명 생성
     */
    fun createDefaultFileName(carName: String): String {
        val safeCarName = carName.toSafeFileName()
        val nowText = System.currentTimeMillis().toFileDateText()

        return "차량정비내역_${safeCarName}_${nowText}.xlsx"
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
        val sheet = workbook.createSheet("요약")

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        // 제목
        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf("차량 정비 요약"),
            style = titleStyle
        )

        // 차량 정보 섹션
        createRow(
            sheet = sheet,
            rowIndex = 2,
            values = listOf("차량 정보"),
            style = sectionStyle
        )

        createRow(sheet, 3, listOf("차량명", data.car.name))
        createRow(sheet, 4, listOf("차량번호", data.car.plate))
        createRow(sheet, 5, listOf("연식", data.car.year ?: ""))
        createRow(sheet, 6, listOf("현재 주행거리", "${data.car.mileage} km"))
        createRow(sheet, 7, listOf("연료", data.car.fuelType ?: ""))
        createRow(sheet, 8, listOf("메모", data.car.notes ?: ""))
        createRow(sheet, 9, listOf("대표차량", if (data.car.isPrimary) "Y" else "N"))
        createRow(
            sheet = sheet,
            rowIndex = 10,
            values = listOf(
                "마지막 주행거리 갱신일",
                data.car.lastMileageUpdatedAt?.toDateText() ?: ""
            )
        )

        // 정비 요약 섹션
        createRow(
            sheet = sheet,
            rowIndex = 12,
            values = listOf("정비 요약"),
            style = sectionStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 13,
            values = listOf(
                "정비항목",
                "최근 정비일",
                "최근 정비거리",
                "교체주기 km",
                "교체주기 개월",
                "다음 교체거리"
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
                    type.name,
                    latestHistory?.serviceDate ?: "",
                    lastServiceMileage?.let { "$it km" } ?: "",
                    intervalKm?.let { "$it km" } ?: "",
                    intervalMonths?.let { "$it 개월" } ?: "",
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
        val sheet = workbook.createSheet("정비히스토리")

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf("정비히스토리"),
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
            values = listOf("전체 요약"),
            style = sectionStyle
        )

        createRow(sheet, 3, listOf("총 정비 기록 수", "${totalCount}건"))
        createRow(sheet, 4, listOf("총 정비 비용", "${totalCost}원"))
        createRow(sheet, 5, listOf("최근 정비일", latestRow?.history?.serviceDate ?: ""))
        createRow(sheet, 6, listOf("최근 정비항목", latestRow?.typeName ?: ""))

        var rowIndex = 8

        if (allHistories.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = rowIndex,
                values = listOf("정비 이력이 없습니다.")
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
                values = listOf("[$typeName]"),
                style = sectionStyle
            )
            rowIndex++

            // 그룹 헤더
            createRow(
                sheet = sheet,
                rowIndex = rowIndex,
                values = listOf(
                    "정비일",
                    "주행거리",
                    "정비장소",
                    "비용",
                    "메모"
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
                        history.cost?.let { "${it}원" } ?: "",
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
        val sheet = workbook.createSheet("주행거리히스토리")

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf("주행거리히스토리"),
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
            values = listOf("전체 요약"),
            style = sectionStyle
        )

        createRow(sheet, 3, listOf("총 기록 수", "${histories.size}건"))
        createRow(sheet, 4, listOf("현재 주행거리", "${data.car.mileage} km"))
        createRow(sheet, 5, listOf("최근 기록일", latestHistory?.recordedAt?.toDateText() ?: ""))
        createRow(sheet, 6, listOf("최근 기록 주행거리", latestHistory?.mileage?.let { "$it km" } ?: ""))
        createRow(sheet, 7, listOf("누적 증가 주행거리", "${totalIncrease.coerceAtLeast(0)} km"))

        createRow(
            sheet = sheet,
            rowIndex = 9,
            values = listOf("주행거리 기록"),
            style = sectionStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 10,
            values = listOf(
                "기록일",
                "주행거리",
                "이전 기록 대비 증가",
                "메모"
            ),
            style = headerStyle
        )

        if (histories.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = 11,
                values = listOf("주행거리 기록이 없습니다.", "", "", "")
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
                    history.recordedAt.toDateText(),
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

    private fun createMaintenanceSettingSheet(
        workbook: XSSFWorkbook,
        data: CarExportData
    ) {
        val sheet = workbook.createSheet("정비항목설정")

        val titleStyle = createTitleStyle(workbook)
        val sectionStyle = createSectionStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)

        createRow(
            sheet = sheet,
            rowIndex = 0,
            values = listOf("정비항목설정"),
            style = titleStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 2,
            values = listOf("차량별 정비 항목 설정"),
            style = sectionStyle
        )

        createRow(
            sheet = sheet,
            rowIndex = 3,
            values = listOf(
                "정비항목",
                "차량별 주기 km",
                "차량별 주기 개월",
                "기본 주기 km",
                "기본 주기 개월",
                "적용 주기 km",
                "적용 주기 개월",
                "사용 여부"
            ),
            style = headerStyle
        )

        if (data.settingsWithHistory.isEmpty()) {
            createRow(
                sheet = sheet,
                rowIndex = 4,
                values = listOf("등록된 정비항목 설정이 없습니다.", "", "", "", "", "", "", "")
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
                    type.name,
                    setting.intervalKm?.let { "$it km" } ?: "",
                    setting.intervalMonths?.let { "$it 개월" } ?: "",
                    type.defaultIntervalKm?.let { "$it km" } ?: "",
                    type.defaultIntervalMonths?.let { "$it 개월" } ?: "",
                    appliedIntervalKm?.let { "$it km" } ?: "",
                    appliedIntervalMonths?.let { "$it 개월" } ?: "",
                    if (setting.isActive) "사용" else "미사용"
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
            .ifEmpty { "차량" }
    }

    private fun Long.toDateText(): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.KOREA
        )

        return formatter.format(Date(this))
    }

    private fun Long.toFileDateText(): String {
        val formatter = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.KOREA
        )

        return formatter.format(Date(this))
    }
}