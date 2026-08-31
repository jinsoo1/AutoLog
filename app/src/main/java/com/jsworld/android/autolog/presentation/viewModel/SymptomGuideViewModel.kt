package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.domain.model.Symptom
import com.jsworld.android.autolog.domain.model.SymptomCategory
import com.jsworld.android.autolog.domain.model.filterSymptoms
import com.jsworld.android.autolog.domain.repository.SymptomGuideRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.state.SymptomDetailUiState
import com.jsworld.android.autolog.presentation.state.SymptomGuideUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SymptomGuideViewModel @Inject constructor(
    private val repo: SymptomGuideRepository,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    /**
     * 첫 진입 안내를 아직 안 봤으면 true. 목록이 뜬 뒤에 띄우기 위해
     * 로딩과 함께 보고 화면에서 판단한다.
     */
    val noticeSeen: StateFlow<Boolean> =
        userPrefsRepository.observeSymptomGuideNoticeSeen()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun markNoticeSeen() {
        viewModelScope.launch { userPrefsRepository.setSymptomGuideNoticeSeen(true) }
    }

    private val symptomsFlow = MutableStateFlow<List<Symptom>>(emptyList())
    private val loadingFlow = MutableStateFlow(true)
    private val queryFlow = MutableStateFlow("")
    private val categoryFlow = MutableStateFlow<SymptomCategory?>(null)

    val ui: StateFlow<SymptomGuideUiState> = combine(
        symptomsFlow, loadingFlow, queryFlow, categoryFlow
    ) { symptoms, loading, query, category ->
        SymptomGuideUiState(
            loading = loading,
            symptoms = filterSymptoms(symptoms, category, query),
            query = query,
            category = category
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SymptomGuideUiState())

    init {
        viewModelScope.launch {
            symptomsFlow.value = repo.loadSymptoms()
            loadingFlow.value = false
        }
    }

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun setCategory(category: SymptomCategory?) {
        categoryFlow.value = category
    }
}

@HiltViewModel
class SymptomDetailViewModel @Inject constructor(
    repo: SymptomGuideRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val symptomId: String = savedStateHandle["symptomId"] ?: ""

    private val _ui = MutableStateFlow(SymptomDetailUiState())
    val ui: StateFlow<SymptomDetailUiState> = _ui

    init {
        viewModelScope.launch {
            val symptom = repo.loadSymptoms().firstOrNull { it.id == symptomId }
            _ui.value = SymptomDetailUiState(loading = false, symptom = symptom)
        }
    }
}
