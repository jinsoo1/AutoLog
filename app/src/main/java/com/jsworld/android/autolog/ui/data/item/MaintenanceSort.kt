package com.jsworld.android.autolog.ui.data.item

enum class MaintenanceSort(val id: Int, val label: String) {
    DEFAULT(0, "기본"),
    REMAINING_KM(1, "잔여 km 순"),
    DUE_DATE(2, "도래 날짜 순"),
    URGENT_MIN(3, "더 급한쪽 우선");

    companion object {
        fun from(id: Int): MaintenanceSort =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}