package com.jsworld.android.autolog.presentation.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자 입력 필드에 천 단위 콤마를 표시한다. (예: 37900 → 37,900)
 *
 * 상태 값은 숫자 문자열 그대로 두고 화면 표시만 바꾸므로
 * 기존 저장/검증 로직에 영향이 없고, 커서 위치도 정확히 매핑된다.
 */
object ThousandsSeparatorTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        // 숫자 외 문자가 섞여 있으면 변환하지 않는다(방어)
        if (digits.isEmpty() || digits.any { !it.isDigit() }) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                val remaining = digits.length - 1 - i
                if (remaining > 0 && remaining % 3 == 0) append(',')
            }
        }

        val totalCommas = (digits.length - 1) / 3

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(digits.length)
                val commasAfter = (digits.length - clamped) / 3
                return clamped + (totalCommas - commasAfter)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                val commasBefore = formatted.take(clamped).count { it == ',' }
                return (clamped - commasBefore).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
