package com.jsworld.android.autolog.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 앱 공통 모양 스케일.
 *
 * Material3 기본보다 반경을 한 단계씩 키워 부드러운 인상을 준다.
 * (카드 = medium/large, 다이얼로그 = extraLarge, 텍스트필드 = extraSmall)
 */
val AutoLogShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
