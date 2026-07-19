package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.domain.repository.NoticeRepository

import android.content.Context
import com.jsworld.android.autolog.domain.model.Notice
import com.jsworld.android.autolog.core.util.Constant.AUTOLOG_NOTICE
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.isNotBlank

@Singleton
class NoticeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NoticeRepository {
    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun loadNotices(): List<Notice> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.assets.open(AUTOLOG_NOTICE)
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(jsonText)
            val arr = root.optJSONArray("notices") ?: JSONArray()

            val list = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val nextArr = o.optJSONArray("next")
                    val next = buildList {
                        if (nextArr != null) for (j in 0 until nextArr.length()) add(nextArr.getString(j))
                    }
                    add(
                        Notice(
                            id = o.getString("id"),
                            version = o.optString("version").takeIf { it.isNotBlank() },
                            date = o.optString("date").takeIf { it.isNotBlank() },
                            title = o.getString("title"),
                            content = o.optString("content"),
                            next = next
                        )
                    )
                }
            }

            // date 내림차순 정렬 (date 없으면 맨 아래)
            list.sortedWith(
                compareByDescending<Notice> { n ->
                    n.date?.let { runCatching { LocalDate.parse(it, ISO) }.getOrNull() }
                }.thenByDescending { it.id } // 같은 날짜면 id로 한 번 더
            )
        }.getOrElse { emptyList() }
    }
}