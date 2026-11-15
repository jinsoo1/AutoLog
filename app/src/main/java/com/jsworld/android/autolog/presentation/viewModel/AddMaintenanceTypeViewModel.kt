package com.jsworld.android.autolog.presentation.viewModel

import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.domain.model.DuplicateItemNameException
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
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
    @ApplicationContext private val appContext: Context,
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
                val message = when (it) {
                    is DuplicateItemNameException -> appContext.getString(R.string.vm_add_duplicate)
                    else -> it.message ?: appContext.getString(R.string.vm_add_failed)
                }
                _events.tryEmit(UiEvent.Snackbar(message))
            }
        }
    }
}

