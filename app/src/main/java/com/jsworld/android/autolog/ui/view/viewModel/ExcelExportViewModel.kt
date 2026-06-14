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

    val cars: StateFlow<List<Car>> =
        carRepository.getAllCars()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _selectedCarId = MutableStateFlow<Long?>(null)
    val selectedCarId: StateFlow<Long?> = _selectedCarId.asStateFlow()

    private val _exportState = MutableStateFlow<ExcelExportUiState>(
        ExcelExportUiState.Idle
    )
    val exportState: StateFlow<ExcelExportUiState> =
        _exportState.asStateFlow()

    fun selectCar(carId: Long) {
        _selectedCarId.value = carId
    }

    fun createExcelFileName(): String? {
        val selectedId = _selectedCarId.value ?: return null
        val selectedCar = cars.value.firstOrNull { it.id == selectedId }
            ?: return null

        return carExcelExporter.createDefaultFileName(selectedCar.name)
    }

    fun exportSelectedCarToUri(outputUri: Uri) {
        val carId = _selectedCarId.value

        if (carId == null) {
            _exportState.value = ExcelExportUiState.Error("차량을 먼저 선택해주세요.")
            return
        }

        viewModelScope.launch {
            _exportState.value = ExcelExportUiState.Loading

            val success = carExcelExporter.exportCar(
                carId = carId,
                outputUri = outputUri
            )

            _exportState.value =
                if (success) {
                    ExcelExportUiState.Success
                } else {
                    ExcelExportUiState.Error("엑셀 파일 저장에 실패했습니다.")
                }
        }
    }

    fun resetExportState() {
        _exportState.value = ExcelExportUiState.Idle
    }
}