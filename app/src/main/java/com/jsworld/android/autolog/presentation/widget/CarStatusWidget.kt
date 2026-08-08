package com.jsworld.android.autolog.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.util.Constant.ACTION_OPEN_CAR_DETAIL
import com.jsworld.android.autolog.core.util.Constant.EXTRA_CAR_ID
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.presentation.activity.MainActivity
import dagger.hilt.android.EntryPointAccessors
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * 위젯 팔레트 — 앱 테마(딥 블루/슬레이트)와 동일한 값을 라이트/다크로 나눠 쓴다.
 * (Glance 의 day/night ColorProvider 는 시스템 다크모드를 따라간다)
 */
private object WColors {
    val Surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF111A2E))
    val TextPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFE5E7EB))
    val TextSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val Divider = ColorProvider(day = Color(0xFFE2E8F0), night = Color(0xFF263349))
    val Accent = ColorProvider(day = Color(0xFF1E3A8A), night = Color(0xFF93C5FD))
    val AccentBg = ColorProvider(day = Color(0xFFDBEAFE), night = Color(0xFF1E3A8A))

    fun statusColor(status: MaintenanceStatus) = when (status) {
        MaintenanceStatus.OVERDUE -> ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFF87171))
        MaintenanceStatus.SOON -> ColorProvider(day = Color(0xFFB45309), night = Color(0xFFFBBF24))
        MaintenanceStatus.NORMAL -> ColorProvider(day = Color(0xFF16A34A), night = Color(0xFF4ADE80))
    }

    /** 정비 행의 옅은 배경 틴트 — 이전 버전의 보기 좋던 요소를 유지한다. */
    fun rowTint(status: MaintenanceStatus) = when (status) {
        MaintenanceStatus.OVERDUE -> ColorProvider(day = Color(0xFFFEF2F2), night = Color(0xFF3B1E1E))
        MaintenanceStatus.SOON -> ColorProvider(day = Color(0xFFFFFBEB), night = Color(0xFF3A2A12))
        MaintenanceStatus.NORMAL -> ColorProvider(day = Color(0xFFF0FDF4), night = Color(0xFF16291D))
    }

    fun statusBg(status: MaintenanceStatus) = when (status) {
        MaintenanceStatus.OVERDUE -> ColorProvider(day = Color(0xFFFEE2E2), night = Color(0xFF7F1D1D))
        MaintenanceStatus.SOON -> ColorProvider(day = Color(0xFFFEF3C7), night = Color(0xFF78350F))
        MaintenanceStatus.NORMAL -> ColorProvider(day = Color(0xFFDCFCE7), night = Color(0xFF14532D))
    }
}

// 위젯 인스턴스(GlanceId)별 저장되는 key
val KEY_CAR_ID = longPreferencesKey("car_id")

class CarStatusWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    /**
     * 크기 버킷. 이걸 지정하지 않으면 SizeMode.Single 이라 LocalSize 가
     * 항상 최소 크기를 돌려줘서 크기 적응 코드가 전부 죽은 코드가 된다.
     */
    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT, SIZE_WIDE, SIZE_WIDE_TALL)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val carId = prefs[KEY_CAR_ID] ?: -1L

            if (carId <= 0L) {
                EmptyMessage("차량을 선택해주세요")
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

            val loaded = ui
            if (loaded == null) {
                EmptyMessage("불러오는 중…")
                return@provideContent
            }

            WidgetRoot(context = context, carId = carId, ui = loaded)
        }
    }

    companion object {
        /** 2x2 부근 — 차량명·주행거리·가장 급한 항목 1개 */
        val SIZE_COMPACT = DpSize(110.dp, 110.dp)

        /** 4x2(기본) — 좌측 요약 + 우측 정비 목록 3개 */
        val SIZE_WIDE = DpSize(250.dp, 110.dp)

        /** 세로로 늘렸을 때 — 목록 4개 */
        val SIZE_WIDE_TALL = DpSize(250.dp, 160.dp)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WColors.Surface)
            .cornerRadius(16.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = TextStyle(fontSize = 12.sp, color = WColors.TextSecondary))
    }
}

@Composable
private fun WidgetRoot(context: Context, carId: Long, ui: CarWidgetUi) {
    val clickIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_CAR_DETAIL
        putExtra(EXTRA_CAR_ID, carId)
        // PendingIntent 재사용 방지용 (carId별 유니크)
        data = Uri.parse("autolog://widget/car/$carId")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    val compact = LocalSize.current.width < CarStatusWidget.SIZE_WIDE.width

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WColors.Surface)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(clickIntent))
            .padding(12.dp)
    ) {
        if (compact) CompactLayout(ui) else WideLayout(ui)
    }
}

/* ───────────────────────── 2x2 ───────────────────────── */

@Composable
private fun CompactLayout(ui: CarWidgetUi) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                ui.carName,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WColors.TextPrimary),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            StatusBadge(ui)
        }

        Spacer(GlanceModifier.height(6.dp))

        Text(
            "${ui.mileage.formatComma()} km",
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WColors.Accent),
            maxLines = 1
        )

        Spacer(GlanceModifier.height(10.dp))

        // 가장 급한 항목 하나만
        val top = ui.rows.firstOrNull()
        if (top != null) {
            Text(
                top.name.ellipsize(9),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WColors.TextPrimary),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(3.dp))
            Text(
                top.remainText,
                style = TextStyle(fontSize = 10.sp, color = WColors.statusColor(top.status)),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(4.dp))
            RemoteProgressBar(top.progress, top.status, height = 6.dp)
        } else {
            Text(
                "정비 항목이 없어요",
                style = TextStyle(fontSize = 11.sp, color = WColors.TextSecondary)
            )
        }
    }
}

/* ───────────────────────── 4x2 ───────────────────────── */

@Composable
private fun WideLayout(ui: CarWidgetUi) {
    Row(modifier = GlanceModifier.fillMaxSize()) {

        // LEFT — 차량 요약
        Column(
            modifier = GlanceModifier
                .width(112.dp)
                .fillMaxHeight()
                .padding(end = 10.dp)
        ) {
            Text(
                ui.carName,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WColors.TextPrimary),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(1.dp))
            Text(
                ui.plate,
                style = TextStyle(fontSize = 10.sp, color = WColors.TextSecondary),
                maxLines = 1
            )

            Spacer(GlanceModifier.height(10.dp))

            Text("주행거리", style = TextStyle(fontSize = 10.sp, color = WColors.TextSecondary))
            Text(
                "${ui.mileage.formatComma()} km",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WColors.Accent),
                maxLines = 1
            )

            Spacer(GlanceModifier.height(12.dp))

            StatusBadge(ui)
        }

        // DIVIDER
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(WColors.Divider)
        ) {}

        // RIGHT — 급한 순 정비 목록.
        // 행 사이는 고정 간격이다. defaultWeight 로 나누면 위젯이 높을 때
        // 항목들이 위아래로 흩어져 목록으로 읽히지 않는다.
        val rowCount =
            if (LocalSize.current.height >= CarStatusWidget.SIZE_WIDE_TALL.height) 4 else 3
        val shown = ui.rows.take(rowCount)

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(start = 12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "정비 주기",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WColors.TextPrimary),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    if (shown.isEmpty()) "항목 없음" else "위험 TOP ${shown.size}",
                    style = TextStyle(fontSize = 10.sp, color = WColors.TextSecondary),
                    maxLines = 1
                )
            }

            Spacer(GlanceModifier.height(7.dp))

            if (shown.isEmpty()) {
                Text("정비 항목이 없어요", style = TextStyle(fontSize = 12.sp, color = WColors.TextSecondary))
                Spacer(GlanceModifier.height(2.dp))
                Text("앱에서 항목을 추가해보세요", style = TextStyle(fontSize = 11.sp, color = WColors.TextSecondary))
            } else {
                shown.forEachIndexed { index, row ->
                    MaintenanceRow(row)
                    if (index != shown.lastIndex) Spacer(GlanceModifier.height(5.dp))
                }
            }
        }
    }
}

/* ───────────────────────── 공용 조각 ───────────────────────── */

@Composable
private fun StatusBadge(ui: CarWidgetUi) {
    val label = if (ui.dangerCount > 0) "위험 ${ui.dangerCount}" else "정상"
    Box(
        modifier = GlanceModifier
            .background(WColors.statusBg(ui.overallStatus))
            .cornerRadius(999.dp)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = 10.sp,
                color = WColors.statusColor(ui.overallStatus),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun MaintenanceRow(row: MaintenanceProgressRow) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WColors.rowTint(row.status))
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(6.dp)
                        .cornerRadius(3.dp)
                        .background(WColors.statusColor(row.status))
                ) {}
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    row.name.ellipsize(8),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WColors.TextPrimary),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    row.remainText,
                    style = TextStyle(fontSize = 10.sp, color = WColors.TextSecondary),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            RemoteProgressBar(row.progress, row.status, height = 6.dp)
        }
    }
}

@Composable
private fun RemoteProgressBar(
    progress: Float,
    status: MaintenanceStatus,
    height: Dp = 6.dp
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
