package com.jsworld.android.autolog.domain.model

import androidx.annotation.StringRes
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.UiText

enum class MaintenanceSort(val id: Int, @StringRes val labelRes: Int) {
    DEFAULT(0, R.string.sort_default),
    REMAINING_KM(1, R.string.sort_remaining_km),
    DUE_DATE(2, R.string.sort_due_date),
    URGENT_MIN(3, R.string.sort_urgent_first);

    val label: UiText get() = UiText.Res(labelRes)

    companion object {
        fun from(id: Int): MaintenanceSort =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
