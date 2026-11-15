package com.jsworld.android.autolog.presentation.viewModel

import com.jsworld.android.autolog.R
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.data.repository.CarExcelExporter
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.presentation.state.ExcelExportUiState
import com.jsworld.android.autolog.domain.repository.CarRepository
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
    @ApplicationContext private val appContext: Context,
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
                message = appContext.getString(R.string.excel_select_car_first),
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
                        message = appContext.getString(R.string.vm_excel_error),
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