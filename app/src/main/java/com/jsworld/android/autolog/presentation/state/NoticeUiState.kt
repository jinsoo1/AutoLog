package com.jsworld.android.autolog.presentation.state

import com.jsworld.android.autolog.domain.model.Notice

data class NoticeUiState(
    val loading: Boolean = true,
    val notices: List<Notice> = emptyList(),
    val readIds: Set<String> = emptySet(),
    val error: String? = null
)
