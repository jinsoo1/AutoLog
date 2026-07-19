package com.jsworld.android.autolog.domain.repository

import kotlinx.coroutines.flow.Flow

interface NoticeReadRepository {
    fun observeReadIds(): Flow<Set<String>>
    suspend fun markRead(id: String)
}
