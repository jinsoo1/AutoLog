package com.jsworld.android.autolog.data.datastore

import com.jsworld.android.autolog.domain.repository.NoticeReadRepository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.noticeDataStore by preferencesDataStore(name = "notice_prefs")

@Singleton
class NoticeReadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NoticeReadRepository {
    private val KEY_READ_IDS = stringSetPreferencesKey("read_notice_ids")

    override fun observeReadIds(): Flow<Set<String>> =
        context.noticeDataStore.data.map { prefs ->
            prefs[KEY_READ_IDS] ?: emptySet()
        }

    override suspend fun markRead(id: String) {
        context.noticeDataStore.edit { prefs ->
            val current = prefs[KEY_READ_IDS] ?: emptySet()
            prefs[KEY_READ_IDS] = current + id
        }
    }
}
