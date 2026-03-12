package com.jsworld.android.autolog.ui.data.item

data class EditSettingUiState(
    val loading: Boolean = true,
    val typeName: String = "",
    val defaultKm: Int? = null,
    val defaultMonths: Int? = null,
    val currentKm: Int? = null,
    val currentMonths: Int? = null,

    val lastServiceDate: String? = null,     // "yyyy-MM-dd"
    val lastServiceMileage: Int? = null,
    val lastPlace: String? = null,
    val lastCost: Int? = null,
    val lastMemo: String? = null
)