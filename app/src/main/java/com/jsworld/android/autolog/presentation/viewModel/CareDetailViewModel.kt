package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CareDetailViewModel @Inject constructor(
    private val careRepository: CareRepository
) : ViewModel() {

    private val recordsMap = mutableMapOf<Long, StateFlow<List<CareRecord>>>()
    private val itemsMap = mutableMapOf<Long, StateFlow<List<CarePickItem>>>()

    /** 이 차량의 세차·관리 기록 전부(최신순) */
    fun careRecordsState(carId: Long): StateFlow<List<CareRecord>> =
        recordsMap.getOrPut(carId) {
            careRepository.observeRecords(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    /** 항목 관리 목록 — 기본 제공 + 사용자 추가, 켜짐 여부와 주기 포함 */
    fun carePickItemsState(carId: Long): StateFlow<List<CarePickItem>> =
        itemsMap.getOrPut(carId) {
            careRepository.observeItems(carId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun setItemEnabled(carId: Long, name: String, enabled: Boolean) {
        viewModelScope.launch { careRepository.setItemEnabled(carId, name, enabled) }
    }

    fun setInterval(itemId: Long, months: Int?, washCount: Int?) {
        viewModelScope.launch { careRepository.updateInterval(itemId, months, washCount) }
    }

    fun save(
        carId: Long,
        itemName: String,
        performedAt: String,
        cost: Int?,
        method: String?,
        place: String?,
        memo: String?,
        /** 세차와 함께 한 관리 항목들 — 같은 날짜·장소로 기록만 남긴다(비용은 세차에) */
        together: List<String> = emptyList(),
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            careRepository.addRecord(carId, itemName, performedAt, cost, method, place, memo)
            together.forEach { name ->
                careRepository.addRecord(
                    carId = carId,
                    itemName = name,
                    performedAt = performedAt,
                    cost = null,
                    method = null,
                    place = place,
                    memo = null
                )
            }
            onDone()
        }
    }

    fun update(
        recordId: Long,
        performedAt: String,
        cost: Int?,
        method: String?,
        place: String?,
        memo: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            careRepository.updateRecord(recordId, performedAt, cost, method, place, memo)
            onDone()
        }
    }

    fun delete(recordId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            careRepository.deleteRecord(recordId)
            onDone()
        }
    }
}
