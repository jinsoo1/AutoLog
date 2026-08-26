package com.jsworld.android.autolog.data.repository

import android.content.Context
import com.jsworld.android.autolog.core.util.Constant.AUTOLOG_SYMPTOM_GUIDE
import com.jsworld.android.autolog.domain.model.Powertrain
import com.jsworld.android.autolog.domain.model.Symptom
import com.jsworld.android.autolog.domain.model.SymptomCategory
import com.jsworld.android.autolog.domain.model.SymptomEscalation
import com.jsworld.android.autolog.domain.model.SymptomRiskLevel
import com.jsworld.android.autolog.domain.repository.SymptomGuideRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * assets/symptom_guide.json 을 읽는다. 공지사항과 같은 정적 로컬 데이터 방식 —
 * 콘텐츠 수정·추가가 화면 로직 변경 없이 가능하다.
 */
@Singleton
class SymptomGuideRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SymptomGuideRepository {

    // 파일은 바뀌지 않으므로 앱 프로세스 동안 한 번만 파싱한다.
    private var cached: List<Symptom>? = null

    override suspend fun loadSymptoms(): List<Symptom> = withContext(Dispatchers.IO) {
        cached ?: runCatching { parse() }.getOrElse { emptyList() }.also { cached = it }
    }

    private fun parse(): List<Symptom> {
        val jsonText = context.assets.open(AUTOLOG_SYMPTOM_GUIDE)
            .bufferedReader()
            .use { it.readText() }

        val arr = JSONObject(jsonText).optJSONArray("symptoms") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                // 카테고리를 못 읽는 항목은 조용히 버린다 — 데이터 한 건의 오타가
                // 가이드 전체를 비우는 것보다 낫다.
                val category = SymptomCategory.fromOrNull(o.optString("category")) ?: continue
                add(
                    Symptom(
                        id = o.getString("id"),
                        category = category,
                        title = o.getString("title"),
                        keywords = o.optJSONArray("keywords").toStringList(),
                        riskLevel = SymptomRiskLevel.fromLevel(o.optInt("riskLevel", 2)),
                        summary = o.optString("summary"),
                        checks = o.optJSONArray("checks").toStringList(),
                        distinguishPoints = o.optJSONArray("distinguishPoints").toStringList(),
                        observationPoints = o.optJSONArray("observationPoints").toStringList(),
                        escalations = o.optJSONArray("escalations").toEscalations(),
                        // ⚠️ optString 은 JSON null 을 문자열 "null"로 돌려준다 — isNull 로 먼저 거른다
                        immediateAction = if (o.isNull("immediateAction")) null
                        else o.optString("immediateAction").takeIf { it.isNotBlank() },
                        powertrains = o.optJSONArray("powertrains").toPowertrains()
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        this ?: return emptyList()
        return buildList { for (i in 0 until length()) add(getString(i)) }
    }

    /**
     * 필드가 없거나 비어 있으면 전 차종 공통으로 읽는다. 오타로 값을 못 읽어도
     * 빈 집합이 되어 "공통"이 되므로, 실수가 배지 누락으로만 끝난다.
     */
    private fun JSONArray?.toPowertrains(): Set<Powertrain> {
        this ?: return emptySet()
        return buildSet {
            for (i in 0 until length()) {
                Powertrain.fromOrNull(getString(i))?.let { add(it) }
            }
        }
    }

    private fun JSONArray?.toEscalations(): List<SymptomEscalation> {
        this ?: return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val o = getJSONObject(i)
                add(
                    SymptomEscalation(
                        condition = o.getString("condition"),
                        level = SymptomRiskLevel.fromLevel(o.optInt("level", 3))
                    )
                )
            }
        }
    }
}
