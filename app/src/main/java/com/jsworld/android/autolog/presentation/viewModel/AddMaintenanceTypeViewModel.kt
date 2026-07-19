package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddMaintenanceTypeViewModel @Inject constructor(
    private val repo: CarMaintenanceRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    sealed interface UiEvent {
        data class Snackbar(val message: String) : UiEvent
        object Done : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun save(
        carId: Long,
        name: String,
        defaultKm: Int?,
        defaultMonths: Int?,
        useCarOverride: Boolean,
        carKm: Int?,
        carMonths: Int?
    ) {
        viewModelScope.launch {
            runCatching {
                repo.addMaintenanceTypeAndEnableForCarRejectDuplicate(
                    carId = carId,
                    name = name,
                    defaultKm = defaultKm,
                    defaultMonths = defaultMonths,
                    useCarOverride = useCarOverride,
                    carIntervalKm = if (useCarOverride) carKm else null,
                    carIntervalMonths = if (useCarOverride) carMonths else null
                )
            }.onSuccess {
                _events.tryEmit(UiEvent.Done)
            }.onFailure {
                _events.tryEmit(UiEvent.Snackbar(it.message ?: "추가에 실패했어요"))
            }
        }
    }
}

