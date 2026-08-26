package com.jsworld.android.autolog.presentation.state

import com.jsworld.android.autolog.domain.model.Symptom
import com.jsworld.android.autolog.domain.model.SymptomCategory

data class SymptomGuideUiState(
    val loading: Boolean = true,
    /** 검색어·카테고리 필터가 적용된 목록 */
    val symptoms: List<Symptom> = emptyList(),
    val query: String = "",
    val category: SymptomCategory? = null
)

data class SymptomDetailUiState(
    val loading: Boolean = true,
    val symptom: Symptom? = null
)
