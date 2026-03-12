package com.jsworld.android.autolog.ui.view.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.Car
import com.jsworld.android.autolog.ui.data.item.CarCardUi
import com.jsworld.android.autolog.ui.data.item.Notice
import com.jsworld.android.autolog.ui.data.room.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.ui.data.room.repository.CarRepository
import com.jsworld.android.autolog.ui.data.room.repository.NoticeReadRepository
import com.jsworld.android.autolog.ui.data.room.repository.NoticeRepository
import com.jsworld.android.autolog.ui.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarListViewModel @Inject constructor(
    private val repository: CarRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val noticeRepo: NoticeRepository,
    private val readRepo: NoticeReadRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val notices = MutableStateFlow<List<Notice>>(emptyList())

    val unreadCount: StateFlow<Int> = combine(
        notices,
        readRepo.observeReadIds()
    ) { list, readIds ->
        list.count { !readIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            notices.value = noticeRepo.loadNotices()
        }
    }

    val uiCars: StateFlow<List<CarCardUi>> =
        repository.getAllCars()
            .flatMapLatest { cars ->
                if (cars.isEmpty()) flowOf(emptyList())
                else {
                    combine(
                        cars.map { car ->
                            carMaintenanceRepository.observeMaintenanceDigestForCarList(car.id)
                                .map { digest ->
                                    CarCardUi(
                                        car = car,
                                        summary = digest.summary,
                                        dangerCount = digest.dangerCount
                                    )
                                }
                        }
                    ) { list ->
                        list.toList().sortedWith(
                            compareByDescending<CarCardUi> { it.car.isPrimary }   // ✅ 대표 먼저
                                .thenByDescending { it.dangerCount }             // ✅ 위험개수 많은 순
                                .thenBy { it.car.name }                         // ✅ 동률이면 가나다
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cars: StateFlow<List<Car>> =
        repository.getAllCars()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun selectPrimaryCar(car: Car) {
        viewModelScope.launch {
            repository.togglePrimaryCar(car)
            widgetUpdater.requestUpdate()
        }
    }



}