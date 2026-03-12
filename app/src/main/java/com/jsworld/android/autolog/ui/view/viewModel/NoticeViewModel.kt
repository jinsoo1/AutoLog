package com.jsworld.android.autolog.ui.view.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.Notice
import com.jsworld.android.autolog.ui.data.item.NoticeUiState
import com.jsworld.android.autolog.ui.data.room.repository.NoticeReadRepository
import com.jsworld.android.autolog.ui.data.room.repository.NoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NoticeViewModel @Inject constructor(
    private val repo: NoticeRepository,
    private val readRepo: NoticeReadRepository
) : ViewModel() {

    private val noticesFlow = MutableStateFlow<List<Notice>>(emptyList())
    private val loadingFlow = MutableStateFlow(true)
    private val errorFlow = MutableStateFlow<String?>(null)

    val ui: StateFlow<NoticeUiState> = combine(
        noticesFlow,
        readRepo.observeReadIds(),
        loadingFlow,
        errorFlow
    ) { notices, readIds, loading, error ->
        NoticeUiState(
            loading = loading,
            notices = notices,
            readIds = readIds,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoticeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadingFlow.value = true
            errorFlow.value = null

            val list = repo.loadNotices()
            noticesFlow.value = list
            loadingFlow.value = false
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            readRepo.markRead(id)
        }
    }
}