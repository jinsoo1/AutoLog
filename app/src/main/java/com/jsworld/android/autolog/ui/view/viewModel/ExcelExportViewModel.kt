package com.jsworld.android.autolog.ui.view.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.export.CarExcelExporter
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.ExcelExportUiState
import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExcelExportViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val carExcelExporter: CarExcelExporter
) : ViewModel() {

    val cars = carRepository.getAllCars()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedCarId = MutableStateFlow<Long?>(null)
    val selectedCarId: StateFlow<Long?> = _selectedCarId.asStateFlow()

    private val _exportState = MutableStateFlow<ExcelExportUiState>(ExcelExportUiState.Idle)
    val exportState: StateFlow<ExcelExportUiState> = _exportState.asStateFlow()

    fun selectCar(carId: Long) {
        _selectedCarId.value = carId
    }

    fun createExcelFileName(): String? {
        val selectedId = _selectedCarId.value ?: return null
        val selectedCar = cars.value.firstOrNull { it.id == selectedId } ?: return null

        return carExcelExporter.createDefaultFileName(selectedCar.name)
    }

    fun exportSelectedCarToUri(outputUri: Uri) {
        val carId = _selectedCarId.value

        if (carId == null) {
            _exportState.value = ExcelExportUiState.Error(
                message = "차량을 먼저 선택해주세요.",
                detail = "selectedCarId is null"
            )
            return
        }

        viewModelScope.launch {
            _exportState.value = ExcelExportUiState.Loading

            val result = carExcelExporter.exportCar(
                carId = carId,
                outputUri = outputUri
            )

            _exportState.value = result.fold(
                onSuccess = {
                    ExcelExportUiState.Success
                },
                onFailure = { throwable ->
                    ExcelExportUiState.Error(
                        message = "엑셀 파일 저장 중 오류가 발생했습니다.",
                        detail = throwable.toCrashText()
                    )
                }
            )
        }
    }

    fun resetExportState() {
        _exportState.value = ExcelExportUiState.Idle
    }

    private fun Throwable.toCrashText(): String {
        return buildString {
            appendLine("Excel export crash")
            appendLine()
            appendLine("Class: ${this@toCrashText::class.java.name}")
            appendLine("Message: ${this@toCrashText.message}")
            appendLine()
            appendLine("Stacktrace:")
            appendLine(stackTraceToString())

            cause?.let { cause ->
                appendLine()
                appendLine("Cause:")
                appendLine("Class: ${cause::class.java.name}")
                appendLine("Message: ${cause.message}")
                appendLine(cause.stackTraceToString())
            }
        }
    }
}