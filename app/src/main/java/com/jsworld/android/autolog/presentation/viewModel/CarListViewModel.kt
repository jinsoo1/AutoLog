package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarCardUi
import com.jsworld.android.autolog.domain.model.Notice
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.NoticeReadRepository
import com.jsworld.android.autolog.domain.repository.NoticeRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.widget.WidgetUpdater
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
    private val userPrefsRepository: UserPrefsRepository,
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
                            compareByDescending<CarCardUi> { it.car.isPrimary }   // 대표 먼저
                                .thenByDescending { it.dangerCount }             // 위험개수 많은 순
                                .thenBy { it.car.name }                         // 동률이면 가나다
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

    /**
     * 백업 리마인더 배너 노출 여부.
     * 데이터가 있는데 한 번도 백업하지 않았거나, 마지막 백업이 오래됐고,
     * 배너를 닫은 지도 충분히 지났을 때만 노출.
     */
    val showBackupBanner: StateFlow<Boolean> = combine(
        repository.getAllCars(),
        userPrefsRepository.observeLastBackupAt(),
        userPrefsRepository.observeBackupBannerDismissedAt()
    ) { cars, lastBackup, dismissedAt ->
        val now = System.currentTimeMillis()
        val hasData = cars.isNotEmpty()
        val staleBackup = lastBackup == 0L || (now - lastBackup) > BACKUP_REMIND_INTERVAL_MS
        val notDismissedRecently = (now - dismissedAt) > BANNER_SNOOZE_MS
        hasData && staleBackup && notDismissedRecently
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dismissBackupBanner() {
        viewModelScope.launch {
            userPrefsRepository.setBackupBannerDismissedAt(System.currentTimeMillis())
        }
    }

    companion object {
        private const val BACKUP_REMIND_INTERVAL_MS = 14L * 24 * 60 * 60 * 1000
        private const val BANNER_SNOOZE_MS = 7L * 24 * 60 * 60 * 1000
    }
}