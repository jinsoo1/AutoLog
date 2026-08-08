package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 탭 화면들이 공유하는 "지금 보고 있는 차량" 상태.
 *
 * MainActivity 에서 한 번 만들어 NavHost 전체에 내려준다. 탭마다 따로 만들면
 * 탭을 옮길 때 선택이 흔들리기 때문에 액티비티 스코프로 유지한다.
 */
@HiltViewModel
class CarContextViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    val cars: StateFlow<List<Car>> =
        carRepository.getAllCars()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 저장된 선택이 없거나 그 차량이 삭제됐으면 대표 차량 → 첫 차량 순으로 대체한다.
     * (선택 값을 지우지는 않는다. 복원 등으로 같은 id 가 다시 생기면 그대로 살아난다)
     */
    val selectedCar: StateFlow<Car?> =
        combine(
            carRepository.getAllCars(),
            userPrefsRepository.observeSelectedCarId()
        ) { cars, selectedId ->
            cars.firstOrNull { it.id == selectedId }
                ?: cars.firstOrNull { it.isPrimary }
                ?: cars.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectCar(carId: Long) {
        viewModelScope.launch { userPrefsRepository.setSelectedCarId(carId) }
    }
}
