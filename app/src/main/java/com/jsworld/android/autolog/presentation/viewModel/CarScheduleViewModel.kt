package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.CarScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CarScheduleViewModel @Inject constructor(
    private val scheduleRepository: CarScheduleRepository,
    private val carRepository: CarRepository
) : ViewModel() {

    private val map = mutableMapOf<Long, StateFlow<List<CarSchedule>>>()

    fun schedulesState(carId: Long): StateFlow<List<CarSchedule>> =
        map.getOrPut(carId) {
            scheduleRepository.observeByCar(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /** 정기검사 제안에 쓸 연식 */
    fun carYear(carId: Long): Flow<String?> =
        carRepository.getAllCars().map { cars -> cars.firstOrNull { it.id == carId }?.year }

    fun add(
        carId: Long,
        type: ScheduleType,
        title: String,
        dueDate: String,
        repeatMonths: Int?,
        memo: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            scheduleRepository.add(
                CarSchedule(
                    id = 0,
                    carId = carId,
                    type = type,
                    title = title,
                    dueDate = dueDate,
                    repeatMonths = repeatMonths,
                    memo = memo?.takeIf { it.isNotBlank() }
                )
            )
            onDone()
        }
    }

    fun update(schedule: CarSchedule, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            scheduleRepository.update(schedule)
            onDone()
        }
    }

    fun delete(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            scheduleRepository.delete(id)
            onDone()
        }
    }

    /** 완료 — 반복이면 다음 회차로, 아니면 삭제. 결과 문구를 콜백으로 준다 */
    fun markDone(id: Long, onResult: (nextDueDate: String?) -> Unit = {}) {
        viewModelScope.launch { onResult(scheduleRepository.markDone(id)) }
    }
}
