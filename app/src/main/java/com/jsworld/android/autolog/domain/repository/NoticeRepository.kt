package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.Notice

interface NoticeRepository {
    suspend fun loadNotices(): List<Notice>
}
