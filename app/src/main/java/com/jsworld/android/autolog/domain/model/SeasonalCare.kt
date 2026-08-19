package com.jsworld.android.autolog.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 계절별 관리 — "이번 겨울 전에 확인할 3가지".
 *
 * 단순 리마인더가 아니라 **읽히는 카드**다. 주행거리로는 안 잡히는 것들
 * (배터리는 추워지면, 와이퍼는 장마 전에)을 계절이 대신 알려준다.
 *
 * 새 데이터를 만들지 않는다. 항목 이름은 앱에 이미 있는 정비 항목 그대로 쓰고,
 * "마지막 언제 했는지"는 사용자 자신의 기록에서 읽는다.
 */

/**
 * 카드를 띄우는 창(window). 12개월을 빈틈없이 나눠 갖는다 —
 * 어느 달에 열어도 볼 게 하나는 있어야 한다.
 *
 * 이름은 **대비하는 대상**이지 지금 계절이 아니다.
 * 장마 카드는 5~6월에 떠야 장마 전에 와이퍼를 바꿀 수 있다.
 */
enum class Season {
    /** 3~4월 — 겨우내 쌓인 것 정리 */
    SPRING,

    /** 5~6월 — 장마 오기 전 */
    MONSOON,

    /** 7~8월 — 한여름 */
    SUMMER,

    /** 9~11월 — 추워지기 전 */
    PRE_WINTER,

    /** 12~2월 — 한겨울 */
    WINTER
}

/** 항목 하나와 "왜 지금인지". 이유가 없으면 계절 카드일 이유도 없다. */
data class SeasonalTip(
    /** ⚠️ [com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems] 의 이름과 글자까지 같아야 한다 */
    val itemName: String,
    val reason: String
)

data class SeasonalCareGuide(
    val season: Season,
    val title: String,
    val subtitle: String,
    val tips: List<SeasonalTip>
)

fun seasonOf(month: Int): Season = when (month) {
    3, 4 -> Season.SPRING
    5, 6 -> Season.MONSOON
    7, 8 -> Season.SUMMER
    9, 10, 11 -> Season.PRE_WINTER
    else -> Season.WINTER
}

/**
 * 겨울 항목은 추워지기 전과 한겨울에 같다 — 볼 것이 달라지는 게 아니라
 * 말을 거는 시점이 다를 뿐이라, 목록은 나누지 않고 문구만 나눈다.
 */
private val WINTER_TIPS = listOf(
    SeasonalTip("배터리", "추우면 성능이 뚝 떨어져요"),
    SeasonalTip("냉각수(부동액)", "어는점을 확인해요"),
    SeasonalTip("타이어 공기압 점검", "기온이 내려가면 공기압도 내려가요")
)

private val GUIDES = mapOf(
    Season.SPRING to SeasonalCareGuide(
        season = Season.SPRING,
        title = "봄에 확인하면 좋은 3가지",
        subtitle = "겨우내 쌓인 것부터 털어내요",
        tips = listOf(
            SeasonalTip("에어컨(캐빈) 필터", "겨우내 쌓인 먼지, 에어컨 켜기 전에"),
            SeasonalTip("하부(누유/부식) 점검", "겨울 제설제가 남아 있어요"),
            SeasonalTip("휠 얼라인먼트 점검", "겨우내 팬 포트홀에 틀어졌을 수 있어요")
        )
    ),
    Season.MONSOON to SeasonalCareGuide(
        season = Season.MONSOON,
        title = "장마 오기 전에 확인할 3가지",
        subtitle = "빗길 사고는 시야와 접지에서 나요",
        tips = listOf(
            SeasonalTip("와이퍼 블레이드", "비 올 때 시야가 먼저예요"),
            SeasonalTip("타이어 교체", "빗길 제동은 홈 깊이가 좌우해요"),
            SeasonalTip("브레이크패드", "젖은 노면은 제동거리가 길어져요")
        )
    ),
    Season.SUMMER to SeasonalCareGuide(
        season = Season.SUMMER,
        title = "한여름에 확인할 3가지",
        subtitle = "더위에 약한 것부터 봐요",
        tips = listOf(
            SeasonalTip("냉각수(부동액)", "과열은 여름에 나요"),
            SeasonalTip("에어컨 냉매 점검/보충", "시원해지지 않으면 점검해요"),
            SeasonalTip("타이어 공기압 점검", "기온이 오르면 공기압도 올라가요")
        )
    ),
    Season.PRE_WINTER to SeasonalCareGuide(
        season = Season.PRE_WINTER,
        title = "이번 겨울 전에 확인할 3가지",
        subtitle = "기온이 떨어지기 전에 보면 좋아요",
        tips = WINTER_TIPS
    ),
    Season.WINTER to SeasonalCareGuide(
        season = Season.WINTER,
        title = "겨울철에 확인할 3가지",
        subtitle = "추울 때 특히 말썽인 것들이에요",
        tips = WINTER_TIPS
    )
)

fun seasonalGuide(today: LocalDate): SeasonalCareGuide =
    GUIDES.getValue(seasonOf(today.monthValue))

/**
 * '올해는 넘어가기'의 단위. 겨울은 해를 넘기므로(12월·1월이 같은 겨울)
 * 1~2월은 **작년 키**를 쓴다 — 12월에 넘긴 카드가 1월에 다시 뜨면 넘긴 게 아니다.
 */
fun seasonKey(today: LocalDate): String {
    val season = seasonOf(today.monthValue)
    val year = if (season == Season.WINTER && today.monthValue <= 2) today.year - 1 else today.year
    return "${season.name}-$year"
}

/**
 * 카드에 그릴 한 줄.
 *
 * [settingId] 가 null 이면 그 항목을 아직 관리 목록에 켜두지 않은 것이다.
 * 이때도 줄을 지우지 않는다 — "무엇을 봐야 하나"가 이 카드의 값이고,
 * 관리 스타일을 '가볍게'로 고른 사람은 대부분의 계절 항목이 꺼져 있다.
 */
data class SeasonalCareRow(
    val itemName: String,
    val reason: String,
    val settingId: Long?,
    val lastServiceDate: LocalDate?
)

fun buildSeasonalCareRows(
    guide: SeasonalCareGuide,
    items: List<MaintenanceUiModel>,
    lastServiceDates: Map<Long, LocalDate>
): List<SeasonalCareRow> {
    val byName = items.associateBy { it.name }
    return guide.tips.map { tip ->
        val setting = byName[tip.itemName]
        SeasonalCareRow(
            itemName = tip.itemName,
            reason = tip.reason,
            settingId = setting?.settingId,
            lastServiceDate = setting?.settingId?.let { lastServiceDates[it] }
        )
    }
}

/** "마지막 2년 전" / "마지막 3개월 전" / "아직 기록 없음" */
fun lastCareLabel(last: LocalDate?, today: LocalDate): String {
    if (last == null) return "아직 기록 없음"
    val months = ChronoUnit.MONTHS.between(YearMonth.from(last), YearMonth.from(today))
    return when {
        months <= 0L -> "마지막 이번 달"
        months < 12L -> "마지막 ${months}개월 전"
        else -> "마지막 ${months / 12}년 전"
    }
}
