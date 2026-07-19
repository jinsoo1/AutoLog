package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.presentation.state.EditHistoryUiState
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltViewModel
class MaintenanceHistoryEditViewModel @Inject constructor(
    private val repo: CarMaintenanceRepository,
    private val carRepo: CarRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _ui = MutableStateFlow(EditHistoryUiState())
    val ui: StateFlow<EditHistoryUiState> = _ui.asStateFlow()

    private var autoJob: Job? = null
    private var loadedHistoryId: Long? = null

    fun load(historyId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }

            val h = repo.getHistoryById(historyId).firstOrNull()
            if (h == null) {
                _ui.update { it.copy(loading = false, error = "정비 내역을 찾을 수 없습니다.") }
                return@launch
            }

            val carId = repo.getCarIdBySettingId(h.settingId)
            val list = repo.getHistoriesForSetting(h.settingId).first()

            val idx = list.indexOfFirst { it.id == historyId }
            val prev = list.getOrNull(idx - 1)
            val next = list.getOrNull(idx + 1)
            val isLast = (idx >= 0 && idx == list.lastIndex)

            val maxHistoryMileage = list.mapNotNull { it.serviceMileage }.maxOrNull()

            val car = carRepo.getCarById(carId).first()
            val currentCarMileage = car?.mileage ?: 0

            _ui.update { old ->
                old.copy(
                    loading = false,
                    historyId = historyId,
                    settingId = h.settingId,
                    carId = carId,
                    date = h.serviceDate.orEmpty(),
                    mileage = h.serviceMileage?.toString().orEmpty(),
                    place = h.place.orEmpty(),
                    cost = h.cost?.toString().orEmpty(),
                    memo = h.memo.orEmpty(),
                    prevDate = prev?.serviceDate?.toLocalDateOrNull(),
                    prevMileage = prev?.serviceMileage,
                    nextDate = next?.serviceDate?.toLocalDateOrNull(),
                    nextMileage = next?.serviceMileage,
                    isLast = isLast,
                    maxHistoryMileage = maxHistoryMileage,
                    currentCarMileage = currentCarMileage
                )
            }

            // autoUpdate 구독 (carId 확정 후)
            autoJob?.cancel()
            autoJob = launch {
                userPrefsRepository.observeAutoMileageUpdate(carId).collectLatest { enabled ->
                    _ui.update { it.copy(autoUpdateCarMileage = enabled) }
                }
            }
        }
    }

    fun onDateChange(v: String) = _ui.update { it.copy(date = v, error = null) }
    fun onMileageChange(v: String) = _ui.update { it.copy(mileage = v.filter { c -> c.isDigit() }, error = null) }
    fun onPlaceChange(v: String) = _ui.update { it.copy(place = v) }
    fun onCostChange(v: String) = _ui.update { it.copy(cost = v.filter { c -> c.isDigit() }, error = null) }
    fun onMemoChange(v: String) = _ui.update { it.copy(memo = v) }

    private fun validate(state: EditHistoryUiState): String? {
        val d = state.date.toLocalDateOrNull() ?: return "날짜를 입력해주세요(yyyy-MM-dd)"
        val m = state.mileage.toIntOrNull() ?: return "주행거리를 입력해주세요"

        state.prevDate?.let { if (d.isBefore(it)) return "날짜는 이전 내역(${it})보다 빠를 수 없어요" }
        state.nextDate?.let { if (d.isAfter(it))  return "날짜는 다음 내역(${it})보다 늦을 수 없어요" }

        state.prevMileage?.let { if (m < it) return "주행거리는 이전 내역(${it}km)보다 작을 수 없어요" }
        state.nextMileage?.let { if (m > it) return "주행거리는 다음 내역(${it}km)보다 클 수 없어요" }

        return null
    }

    fun save(onDone: () -> Unit) {
        val state = _ui.value
        val err = validate(state)
        if (err != null) {
            _ui.update { it.copy(error = err) }
            return
        }

        viewModelScope.launch {
            val newMileage = state.mileage.toInt()
            val entity = MaintenanceHistoryEntity(
                id = state.historyId,
                settingId = state.settingId,
                serviceDate = state.date,
                serviceMileage = newMileage,
                place = state.place.ifBlank { null },
                cost = state.cost.toIntOrNull(),
                memo = state.memo.ifBlank { null }
            )

            // 마지막 내역 mileage 변경 시 차량 mileage 처리
            if (state.isLast) {
                val car = carRepo.getCarById(state.carId).first()
                val currentCarMileage = car?.mileage ?: 0

                val shouldAskOrUpdate = newMileage > currentCarMileage

                if (shouldAskOrUpdate && !state.autoUpdateCarMileage) {
                    _ui.update { it.copy(showUpdateCarDialog = true, pendingCarMileage = newMileage) }
                    _pendingEntity = entity
                    return@launch
                }

                repo.updateHistory(entity)

                if (shouldAskOrUpdate && state.autoUpdateCarMileage) {
                    repo.updateCarMileage(state.carId, newMileage)
                }
                widgetUpdater.requestUpdate()
                onDone()
            } else {
                repo.updateHistory(entity)
                widgetUpdater.requestUpdate()
                onDone()
            }
        }
    }

    // 다이얼로그에서 사용
    private var _pendingEntity: MaintenanceHistoryEntity? = null

    fun confirmUpdateCarMileage(onDone: () -> Unit) {
        val state = _ui.value
        val entity = _pendingEntity ?: return
        val newMileage = state.pendingCarMileage ?: return

        viewModelScope.launch {
            repo.updateHistory(entity)

            if (newMileage > state.currentCarMileage) {
                repo.updateCarMileage(state.carId, newMileage)
            }
            widgetUpdater.requestUpdate()
            _ui.update { it.copy(showUpdateCarDialog = false, pendingCarMileage = null) }
            _pendingEntity = null
            onDone()
        }
    }

    fun declineUpdateCarMileage(onDone: () -> Unit) {
        val entity = _pendingEntity ?: return
        viewModelScope.launch {
            repo.updateHistory(entity)
            widgetUpdater.requestUpdate()
            _ui.update { it.copy(showUpdateCarDialog = false, pendingCarMileage = null) }
            _pendingEntity = null
            onDone()
        }
    }

    fun setAutoMileageUpdate(enabled: Boolean) {
        val carId = _ui.value.carId
        viewModelScope.launch {
            userPrefsRepository.setAutoMileageUpdate(carId, enabled)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteHistory(_ui.value.historyId)
            widgetUpdater.requestUpdate()
            onDone()
        }
    }

    fun dismissUpdateCarDialog() {
        _ui.update { it.copy(showUpdateCarDialog = false) }
    }
}

private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this, ISO) }.getOrNull()