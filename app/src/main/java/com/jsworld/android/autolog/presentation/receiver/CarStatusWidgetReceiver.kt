package com.jsworld.android.autolog.presentation.receiver

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.jsworld.android.autolog.presentation.widget.CarStatusWidget
import com.jsworld.android.autolog.presentation.widget.WidgetDailyUpdateScheduler


class CarStatusWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = CarStatusWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetDailyUpdateScheduler.schedule(context) // 하루 갱신 스케줄 시작
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetDailyUpdateScheduler.cancel(context)
    }
}