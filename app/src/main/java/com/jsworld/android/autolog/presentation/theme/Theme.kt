package com.jsworld.android.autolog.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
    primary = AutoBlueDark,
    onPrimary = Color(0xFF0B1220),

    primaryContainer = Color(0xFF1E3A8A),      // 진한 블루(컨테이너)
    onPrimaryContainer = Color(0xFFDBEAFE),

    secondary = AutoSlateDark,
    onSecondary = Color(0xFF0B1220),

    secondaryContainer = Color(0xFF1F2937),
    onSecondaryContainer = Color(0xFFCBD5E1),

    tertiary = AutoCyanDark,
    onTertiary = Color(0xFF06202B),

    tertiaryContainer = Color(0xFF164E63),
    onTertiaryContainer = Color(0xFFCFFAFE),

    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE5E7EB),

    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE5E7EB),

    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFCBD5E1),

    outline = Color(0xFF334155),

    error = Color(0xFFF87171),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = AutoBlueLight,
    onPrimary = Color.White,

    primaryContainer = Color(0xFFDBEAFE),      // 연한 블루(컨테이너)
    onPrimaryContainer = Color(0xFF0B1220),

    secondary = AutoSlateLight,
    onSecondary = Color.White,

    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),

    tertiary = AutoCyanLight,
    onTertiary = Color(0xFF06202B),

    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF083344),

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),

    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),

    outline = Color(0xFFCBD5E1),

    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun AutoLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 앱 고유 색을 쓰려면 기본 false 추천
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}