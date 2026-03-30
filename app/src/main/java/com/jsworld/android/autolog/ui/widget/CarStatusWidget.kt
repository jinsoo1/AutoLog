package com.jsworld.android.autolog.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.cornerRadius
import kotlin.jvm.java
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.jsworld.android.autolog.ui.data.item.MaintenanceStatus
import com.jsworld.android.autolog.ui.view.activity.MainActivity
import dagger.hilt.android.EntryPointAccessors
import java.text.NumberFormat
import kotlin.math.roundToInt
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.ui.view.util.Constant.ACTION_OPEN_CAR_DETAIL
import com.jsworld.android.autolog.ui.view.util.Constant.EXTRA_CAR_ID


private object WColors {
    val Canvas = ColorProvider(Color(0xFFF6F7F9.toInt()))
    val Surface = ColorProvider(Color(0xFFFFFFFF.toInt()))
    val Divider = ColorProvider(Color(0xFFE6E8EB.toInt()))
    val TextSecondary = ColorProvider(Color(0xFF6B7280.toInt()))
    val ChipBg = ColorProvider(Color(0xFFF3F4F6.toInt()))

    fun statusColor(status: MaintenanceStatus) = when (status) {
        MaintenanceStatus.OVERDUE -> ColorProvider(Color(0xFFDF2D2D.toInt()))
        MaintenanceStatus.SOON -> ColorProvider(Color(0xFFFFA000.toInt()))
        MaintenanceStatus.NORMAL -> ColorProvider(Color(0xFF2E7D32.toInt()))
    }

    // 행 배경 “아주 약하게” 틴트
    fun rowTint(status: MaintenanceStatus) = when (status) {
        MaintenanceStatus.OVERDUE -> ColorProvider(Color(0xFFFFF1F1.toInt()))
        MaintenanceStatus.SOON -> ColorProvider(Color(0xFFFFF7E6.toInt()))
        MaintenanceStatus.NORMAL -> ColorProvider(Color(0xFFF5FFF7.toInt()))
    }
}

// 위젯 인스턴스(GlanceId)별 저장되는 key
val KEY_CAR_ID = longPreferencesKey("car_id")

class CarStatusWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val carId = prefs[KEY_CAR_ID] ?: -1L

            if (carId <= 0L) {
                Text("차량을 선택해주세요")
                return@provideContent
            }

            val ep = remember {
                EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            }

            // carId가 바뀌면 다시 로드되도록 produceState 사용
            val ui by produceState<CarWidgetUi?>(initialValue = null, key1 = carId) {
                value = runCatching {
                    ep.carWidgetRepository().getCarWidgetUiOnce(carId, maxRows = 4)
                }.getOrNull()
            }

            if (ui == null) {
                Text("위젯 데이터를 불러오지 못했어요")
                return@provideContent
            }

            WidgetRoot(
                context = context,
                carId = carId,
                ui = ui
            )
        }
    }
}

@Composable
private fun WidgetRoot(
    context: Context,
    carId: Long,
    ui: CarWidgetUi?
) {
    if (carId <= 0L) return
    if (ui == null) { Text("위젯 데이터를 불러오지 못했어요"); return }

    val clickIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_CAR_DETAIL
        putExtra(EXTRA_CAR_ID, carId)

        // 중요: PendingIntent 재사용 방지용 (carId별 유니크)
        data = Uri.parse("autolog://widget/car/$carId")

        // 기존 태스크가 있으면 재사용 + 새 인텐트 전달
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    // 위젯이 좁아질 때 좌측 폭만 살짝 줄여서 “1/3 느낌” 유지 (LocalSize 활용) :contentReference[oaicite:3]{index=3}
    val isNarrow = LocalSize.current.width < 280.dp
    val leftWidth = if (isNarrow) 104.dp else 120.dp

    Scaffold(
        backgroundColor = WColors.Surface,
        content = {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(clickIntent))
                    .padding(12.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxSize()) {

                    // LEFT
                    LeftPanel(
                        ui = ui,
                        modifier = GlanceModifier
                            .width(leftWidth)
                            .fillMaxHeight()
                            .padding(end = 10.dp)
                    )

                    // DIVIDER (세로 구분선)
                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(WColors.Divider)
                    ){}

                    // RIGHT
                    RightPanel(
                        ui = ui,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(start = 10.dp)
                    )
                }
            }
        }
    )
}
@Composable
private fun LeftPanel(ui: CarWidgetUi, modifier: GlanceModifier) {
    Column(modifier = modifier) {

        Text(ui.carName, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(GlanceModifier.height(2.dp))
        Text(ui.plate, style = TextStyle(fontSize = 11.sp, color = WColors.TextSecondary), maxLines = 1)

        Spacer(GlanceModifier.height(12.dp))

        Text("주행거리", style = TextStyle(fontSize = 11.sp, color = WColors.TextSecondary))
        Text("${ui.mileage.formatComma()} km", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)

        Spacer(GlanceModifier.height(12.dp))

        val badgeText = if (ui.dangerCount > 0) "위험 ${ui.dangerCount}" else "정상"
        val badgeColor = WColors.statusColor(ui.overallStatus)

        Box(
            modifier = GlanceModifier
                .background(WColors.ChipBg)
                .cornerRadius(999.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                badgeText,
                style = TextStyle(fontSize = 11.sp, color = badgeColor, fontWeight = FontWeight.Medium)
            )
        }
    }
}


@Composable
private fun RightPanel(ui: CarWidgetUi, modifier: GlanceModifier) {

    // 위젯이 낮거나(런처에서 작은 높이), 항목이 많으면(4개) 컴팩트 모드
    val size = LocalSize.current
    val compact = size.height < 170.dp || size.width < 250.dp || ui.rows.size >= 4

    val headerTitleSize = if (compact) 12.sp else 13.sp
    val headerMetaSize  = if (compact) 10.sp else 11.sp

    val headerTopGap    = if (compact) 4.dp else 8.dp
    val headerBottomGap = if (compact) 6.dp else 10.dp

    val rowGap          = if (compact) 4.dp else 8.dp

    Column(modifier = modifier.fillMaxHeight()) {

        // 헤더
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "정비 주기",
                style = TextStyle(fontSize = headerTitleSize, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                if (ui.rows.isEmpty()) "항목 없음" else "위험 TOP ${ui.rows.size}",
                style = TextStyle(fontSize = headerMetaSize, color = WColors.TextSecondary),
                maxLines = 1
            )
        }

        Spacer(GlanceModifier.height(headerTopGap))

        // 구분선 (Box 대신 Spacer 추천)
//        Spacer(
//            GlanceModifier
//                .fillMaxWidth()
//                .height(1.dp)
//                .background(WColors.Divider)
//        )

        Spacer(GlanceModifier.height(headerBottomGap))

        if (ui.rows.isEmpty()) {
            Text("정비 항목이 없어요", style = TextStyle(fontSize = 12.sp, color = WColors.TextSecondary))
            Spacer(GlanceModifier.height(2.dp))
            Text("앱에서 정비 항목을 추가해보세요", style = TextStyle(fontSize = 11.sp, color = WColors.TextSecondary))
        } else {
            ui.rows.forEachIndexed { idx, row ->
                MaintenanceRow(row, compact = compact)
                if (idx != ui.rows.lastIndex) Spacer(GlanceModifier.height(rowGap))
            }
        }
    }
}

@Composable
private fun MaintenanceRow(row: MaintenanceProgressRow, compact: Boolean) {
    val width = LocalSize.current.width

    val ultraNarrow = width < 220.dp
    val narrow = width < 250.dp

    val nameSize = when {
        width < 190.dp -> 10.sp
        width < 240.dp -> 11.sp
        else -> 12.sp
    }

    val metaSize = when {
        width < 190.dp -> 9.sp
        width < 240.dp -> 10.sp
        else -> 11.sp
    }

    val outerPadH = if (compact) 6.dp else 8.dp
    val outerPadV = if (compact) 6.dp else 8.dp

    val dotSize = if (compact) 5.dp else 6.dp
    val dotRadius = dotSize / 2

    val lineGap = if (compact) 4.dp else 6.dp
    val barHeight = if (compact) 6.dp else 8.dp
    val corner = if (compact) 10.dp else 12.dp

    val nameWidth = when {
        width < 180.dp -> 40.dp
        width < 200.dp -> 52.dp
        width < 220.dp -> 64.dp
        width < 240.dp -> 76.dp
        width < 260.dp -> 88.dp
        else -> 100.dp
    }

    val maxNameChars = when {
        width < 170.dp -> 4
        width < 190.dp -> 5
        width < 210.dp -> 6
        width < 230.dp -> 7
        width < 250.dp -> 8
        width < 270.dp -> 9
        else -> 10
    }

    val displayName = row.name.ellipsize(maxNameChars)

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WColors.rowTint(row.status))
            .cornerRadius(corner)
            .padding(horizontal = outerPadH, vertical = outerPadV)
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(dotSize)
                        .cornerRadius(dotRadius)
                        .background(WColors.statusColor(row.status))
                ) {}

                Spacer(GlanceModifier.width(5.dp))

                Box(
                    modifier = GlanceModifier.width(nameWidth),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = displayName,
                        style = TextStyle(
                            fontSize = nameSize,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }

                Spacer(GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = row.remainText,
                        style = TextStyle(
                            fontSize = metaSize,
                            color = WColors.TextSecondary
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(GlanceModifier.height(lineGap))

            RemoteProgressBar(
                progress = row.progress,
                status = row.status,
                height = barHeight
            )
        }
    }
}

@Composable
private fun RemoteProgressBar(
    progress: Float,
    status: MaintenanceStatus,
    height: Dp = 8.dp
) {
    val context = LocalContext.current
    val layoutRes = when (status) {
        MaintenanceStatus.OVERDUE -> R.layout.widget_progressbar_overdue
        MaintenanceStatus.SOON -> R.layout.widget_progressbar_soon
        MaintenanceStatus.NORMAL -> R.layout.widget_progressbar_normal
    }
    val p = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    val rv = RemoteViews(context.packageName, layoutRes).apply {
        setProgressBar(R.id.pb, 100, p, false)
    }
    AndroidRemoteViews(
        remoteViews = rv,
        modifier = GlanceModifier.fillMaxWidth().height(height)
    )
}

private fun String.ellipsize(maxChars: Int): String {
    if (maxChars <= 0) return ""
    if (length <= maxChars) return this
    if (maxChars == 1) return "…"
    return take(maxChars - 1) + "…"
}

private fun Int.formatComma(): String =
    NumberFormat.getIntegerInstance().format(this)
