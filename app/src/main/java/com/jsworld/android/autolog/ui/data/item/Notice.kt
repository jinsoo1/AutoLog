package com.jsworld.android.autolog.ui.data.item

data class Notice(
    val id: String,
    val version: String?,
    val date: String?,
    val title: String,
    val content: String,
    val next: List<String> = emptyList()
)

data class NoticeUiState(
    val loading: Boolean = true,
    val notices: List<Notice> = emptyList(),
    val readIds: Set<String> = emptySet(),
    val error: String? = null
)