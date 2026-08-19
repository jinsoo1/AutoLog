package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.CarScheduleRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
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
    private val carRepository: CarRepository,
    private val userPrefsRepository: UserPrefsRepository
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

    /**
     * @param onNeedsAlertSetup 첫 일정을 등록했을 때 — 화면이 알림 채널 생성과
     *   하루 1회 체인 예약을 해준다. 앱을 다시 켤 때까지 기다리면 그 사이 알림이 없다.
     */
    fun add(
        carId: Long,
        type: ScheduleType,
        title: String,
        dueDate: String,
        repeatMonths: Int?,
        memo: String?,
        onNeedsAlertSetup: () -> Unit = {},
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val wasEmpty = runCatching { scheduleRepository.getAll().isEmpty() }
                .getOrDefault(true)

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

            val alertOn = runCatching { userPrefsRepository.observeScheduleAlertEnabled().first() }
                .getOrDefault(true)
            if (wasEmpty && alertOn) onNeedsAlertSetup()

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
