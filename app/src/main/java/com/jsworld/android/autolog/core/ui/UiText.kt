package com.jsworld.android.autolog.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * 화면에 보여줄 문장을 **아직 문자열로 굳히지 않은** 상태로 들고 다니는 타입.
 *
 * 도메인 계층의 문장 빌더(리포트 내러티브·세차 주기·예측 문구 등)는 Context 가 없어
 * 리소스를 직접 읽을 수 없다. 대신 "어느 리소스에 어떤 인자"까지만 결정해 넘기고,
 * 화면·알림·엑셀처럼 Context 가 있는 끝단에서 [asString] 으로 굳힌다.
 *
 * 인자에 [UiText] 를 중첩할 수 있다 — "정비 3건 · 수리 1건"처럼 조각을 이어 붙일 때
 * 각 조각의 언어도 함께 따라가야 하기 때문이다.
 */
sealed interface UiText {

    /** 사용자가 직접 입력한 값처럼 번역 대상이 아닌 문자열 */
    data class Raw(val value: String) : UiText

    /** 문자열 리소스 + 포맷 인자. 인자에 UiText 가 들어오면 먼저 굳힌 뒤 포맷한다 */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    /** 조각들을 구분자로 이어 붙인 문장 */
    data class Joined(val parts: List<UiText>, val separator: String = " · ") : UiText

    companion object {
        val Empty: UiText = Raw("")
    }
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> {
        if (args.isEmpty()) context.getString(id)
        else context.getString(
            id,
            *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray()
        )
    }
    is UiText.Joined -> parts.joinToString(separator) { it.asString(context) }
}

@Composable
@ReadOnlyComposable
fun UiText.asString(): String = asString(LocalContext.current)

fun String.asUiText(): UiText = UiText.Raw(this)

/** 아무 문장도 담지 않은 상태인지 — 화면에서 빈 줄을 그리지 않기 위한 판정 */
fun UiText.isBlank(): Boolean = when (this) {
    is UiText.Raw -> value.isBlank()
    is UiText.Res -> false
    is UiText.Joined -> parts.all { it.isBlank() }
}
