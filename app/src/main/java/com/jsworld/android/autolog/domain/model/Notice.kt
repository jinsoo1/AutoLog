package com.jsworld.android.autolog.domain.model

data class Notice(
    val id: String,
    val version: String?,
    val date: String?,
    val title: String,
    val content: String,
    val next: List<String> = emptyList()
)
