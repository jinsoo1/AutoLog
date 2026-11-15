package com.jsworld.android.autolog.presentation.state

import java.time.LocalDate

data class EditHistoryUiState(
    val loading: Boolean = true,
    val historyId: Long = 0,
    val settingId: Long = 0,
    val carId: Long = 0,
    /** 기록의 정비 항목 이름(저장된 원문). 못 찾으면 null → 항목 카드를 숨긴다 */
    val typeName: String? = null,
    /** 항목 카드의 교체 주기 — 차량별 설정, 없으면 항목 기본값 */
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val usingDefaultIntervals: Boolean = false,

    val date: String = "",         // yyyy-MM-dd
    val mileage: String = "",      // 숫자 문자열
    val place: String = "",
    val cost: String = "",
    val memo: String = "",

    val prevDate: LocalDate? = null,
    val prevMileage: Int? = null,
    val nextDate: LocalDate? = null,
    val nextMileage: Int? = null,
    val isLast: Boolean = false,

    val autoUpdateCarMileage: Boolean = true,
    val showUpdateCarDialog: Boolean = false,
    val pendingCarMileage: Int? = null,

    val error: String? = null,

    val currentCarMileage: Int = 0,
    val maxHistoryMileage: Int? = null  // 해당 setting의 최대 기록
)
