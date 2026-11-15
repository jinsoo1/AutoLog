package com.jsworld.android.autolog.domain.model

import androidx.annotation.StringRes
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.UiText

/**
 * 온보딩에서 고르는 정비 관리 스타일.
 * "유저 등급"이 아니라 관리 스타일로 이름 지었다 — 가볍게 써도 틀린 게 아니라는 뉘앙스.
 */
enum class MaintenanceStarterPack(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
) {
    LIGHT(R.string.pack_light_title, R.string.pack_light_desc),
    STANDARD(R.string.pack_standard_title, R.string.pack_standard_desc),
    FULL(R.string.pack_full_title, R.string.pack_full_desc);

    val title: UiText get() = UiText.Res(titleRes)
    val description: UiText get() = UiText.Res(descriptionRes)
}
