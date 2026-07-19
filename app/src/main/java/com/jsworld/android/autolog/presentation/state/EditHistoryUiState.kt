package com.jsworld.android.autolog.presentation.state

import java.time.LocalDate

data class EditHistoryUiState(
    val loading: Boolean = true,
    val historyId: Long = 0,
    val settingId: Long = 0,
    val carId: Long = 0,

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
