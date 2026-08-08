package com.jsworld.android.autolog.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyFuelCost
import java.text.NumberFormat

/**
 * 월별 지출 막대 그래프.
 *
 * Compose 에 기본 차트가 없고 막대 하나뿐이라 Canvas 로 직접 그린다(의존성·용량 0).
 *
 * 플러그인 하이브리드처럼 주유와 충전을 함께 하는 차량은 **한 막대에 쌓아** 그린다.
 * 월 총 에너지비를 한눈에 보면서 구성비도 같이 읽을 수 있기 때문이다.
 * 한 종류만 쓰는 차량은 단색 막대라 기존과 똑같아 보인다.
 */
@Composable
fun MonthlyFuelCostChart(
    data: List<MonthlyFuelCost>,
    modifier: Modifier = Modifier,
    maxMonths: Int = 6
) {
    // 종류별로 쪼개져 들어오므로 월 단위로 다시 묶는다(월 오름차순 유지).
    val months = remember(data, maxMonths) {
        data.groupBy { it.month }
            .toSortedMap()
            .entries
            .map { (month, items) -> month to items }
            .takeLast(maxMonths)
    }
    if (months.isEmpty()) return

    val unitsPresent = remember(data) {
        data.filter { it.totalAmount > 0 }.map { it.unit }.distinct()
    }
    val isStacked = unitsPresent.size > 1

    val fuelColor = MaterialTheme.colorScheme.primary
    val chargeColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun colorFor(unit: FuelUnit) = if (unit.isElectric) chargeColor else fuelColor

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )

    // 막대 높이 기준은 "월 합계"다(쌓은 막대의 전체 길이).
    val maxTotal = months.maxOf { (_, items) -> items.sumOf { it.totalAmount } }
        .coerceAtLeast(1)

    // 마지막 달만 금액을 적는다. 전부 적으면 좁아서 겹친다.
    val lastMonthTotal = months.last().second.sumOf { it.totalAmount }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            val labelHeight = 16.dp.toPx()
            val valueHeight = 14.dp.toPx()
            val plotTop = valueHeight
            val plotBottom = size.height - labelHeight
            val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)

            repeat(3) { index ->
                val y = plotTop + plotHeight * (index + 1) / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            val slotWidth = size.width / months.size
            val barWidth = (slotWidth * 0.46f).coerceAtMost(26.dp.toPx())
            val corner = 4.dp.toPx()

            months.forEachIndexed { index, (_, items) ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                val isLast = index == months.lastIndex

                // 아래부터 주유 → 충전 순으로 쌓는다.
                val segments = items
                    .filter { it.totalAmount > 0 }
                    .sortedBy { if (it.unit.isElectric) 1 else 0 }

                val total = segments.sumOf { it.totalAmount }
                if (total <= 0) return@forEachIndexed

                val totalHeight = (plotHeight * total / maxTotal.toFloat()).coerceAtLeast(3f)
                var cursorBottom = plotBottom

                segments.forEachIndexed { segIndex, segment ->
                    val ratio = segment.totalAmount.toFloat() / total
                    val segHeight = totalHeight * ratio
                    val segTop = cursorBottom - segHeight

                    // 맨 위 조각만 위쪽을 둥글게 — 쌓은 막대가 하나로 보이게.
                    val isTopSegment = segIndex == segments.lastIndex
                    val baseColor = colorFor(segment.unit)
                    val color = if (isLast) baseColor else baseColor.copy(alpha = 0.3f)

                    if (isTopSegment) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, segTop),
                            size = Size(barWidth, segHeight.coerceAtLeast(corner)),
                            cornerRadius = CornerRadius(corner, corner)
                        )
                        // 아래 모서리를 덮어 직각으로 되돌린다(조각이 이어져 보이도록).
                        if (segments.size > 1 && segHeight > corner) {
                            drawRect(
                                color = color,
                                topLeft = Offset(left, cursorBottom - corner),
                                size = Size(barWidth, corner)
                            )
                        }
                    } else {
                        drawRect(
                            color = color,
                            topLeft = Offset(left, segTop),
                            size = Size(barWidth, segHeight)
                        )
                    }

                    cursorBottom = segTop
                }

                val monthLabel = months[index].first.monthLabel()
                val measured = textMeasurer.measure(monthLabel, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        centerX - measured.size.width / 2f,
                        plotBottom + 3.dp.toPx()
                    )
                )

                if (isLast && lastMonthTotal > 0) {
                    val valueText = NumberFormat.getIntegerInstance().format(lastMonthTotal)
                    val measuredValue = textMeasurer.measure(valueText, valueStyle)
                    drawText(
                        textLayoutResult = measuredValue,
                        topLeft = Offset(
                            (centerX - measuredValue.size.width / 2f)
                                .coerceIn(0f, size.width - measuredValue.size.width),
                            (plotBottom - totalHeight - measuredValue.size.height - 2.dp.toPx())
                                .coerceAtLeast(0f)
                        )
                    )
                }
            }
        }

        // 두 종류가 섞여 있을 때만 범례를 붙인다.
        if (isStacked) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                unitsPresent.sortedBy { if (it.isElectric) 1 else 0 }.forEach { unit ->
                    LegendDot(color = colorFor(unit), label = unit.actionLabel)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = CircleShape) {
            Spacer(Modifier.size(8.dp))
        }
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** "2026-08" → "8월" */
private fun String.monthLabel(): String {
    val month = substringAfter('-', "").trimStart('0')
    return if (month.isBlank()) this else "${month}월"
}
