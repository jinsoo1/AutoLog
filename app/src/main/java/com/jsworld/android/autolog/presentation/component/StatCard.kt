package com.jsworld.android.autolog.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 탭 상단의 요약 카드 — 홈의 '주행거리·이번 달 지출', 주유의 '이번 달 주유비·평균 단가'.
 *
 * 두 탭이 따로 카드를 갖고 있어서 **테두리 유무와 캡션 줄** 때문에 무게와 높이가
 * 달랐다(홈은 테두리 + 캡션, 주유는 둘 다 없음). 탭을 옮기면 같은 자리의 카드가
 * 다른 카드처럼 보였다. 하나로 묶고 최소 높이를 맞춰 그 어긋남을 없앤다.
 *
 * 높이는 **강제하지 않는다.** 캡션 없는 카드에 최소 높이를 물리면 아래에 빈 자리가
 * 남아 레이아웃이 깨진 것처럼 보인다. 높이는 캡션이 맞추는 것이지 여백이 맞추는 게 아니다.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    /** 주유/충전처럼 종류를 색으로 구분해야 할 때만 */
    accentDot: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (accentDot != null) {
                    Surface(color = accentDot, shape = CircleShape) {
                        Spacer(Modifier.size(7.dp))
                    }
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (caption != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
