package com.jsworld.android.autolog.presentation.theme

import androidx.compose.ui.graphics.Color

// Brand (AutoLog) - Deep Blue 기반
val AutoBlueLight = Color(0xFF1E3A8A)      // primary
val AutoBlueDark  = Color(0xFF93C5FD)      // primary (dark)

val AutoSlateLight = Color(0xFF334155)     // secondary
val AutoSlateDark  = Color(0xFFCBD5E1)     // secondary (dark)

val AutoCyanLight = Color(0xFF0EA5E9)      // tertiary(포인트)
val AutoCyanDark  = Color(0xFF38BDF8)      // tertiary (dark)

/**
 * 표면 계단(surface container ramp).
 *
 * ⚠️ 이 롤들을 지정하지 않으면 Material3 의 기본값(라벤더 계열)이 그대로 쓰인다.
 * NavigationBar 는 surfaceContainer, 바텀시트는 surfaceContainerLow,
 * 카드 테두리는 outlineVariant 를 쓰기 때문에 앱 전체에 보라 기가 섞여 보였다.
 */
val SlateContainerLowestLight = Color(0xFFFFFFFF)
val SlateContainerLowLight    = Color(0xFFFBFCFE)
val SlateContainerLight       = Color(0xFFF4F7FA)
val SlateContainerHighLight   = Color(0xFFEDF1F6)
val SlateContainerHighestLight= Color(0xFFE6EBF1)

val SlateContainerLowestDark  = Color(0xFF070C16)
val SlateContainerLowDark     = Color(0xFF0D1424)
val SlateContainerDark        = Color(0xFF121B2E)
val SlateContainerHighDark    = Color(0xFF1A2438)
val SlateContainerHighestDark = Color(0xFF232F45)

// 상태색(정비 상태 표시용) - 필요할 때 직접 사용해도 좋음
val StatusNormal = Color(0xFF16A34A)
val StatusSoon   = Color(0xFFF59E0B)
val StatusOverdue= Color(0xFFDC2626)

val Notice = Color(0xFF278C4C)