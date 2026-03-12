package com.jsworld.android.autolog.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.jvm.java

class CarStatusWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // ✅ 기본은 취소
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        val ep = EntryPointAccessors.fromApplication(this, WidgetEntryPoint::class.java)

        setContent {
            val cars by ep.carRepository().getAllCars().collectAsState(initial = emptyList())

            CarWidgetPickerScreen(
                cars = cars,
                onPick = { car ->
                    lifecycleScope.launch {
                        runCatching {
                            val manager = GlanceAppWidgetManager(this@CarStatusWidgetConfigActivity)
                            val glanceId = manager.getGlanceIdBy(appWidgetId)

                            // ✅⭐ 가장 중요: definition 명시!
                            updateAppWidgetState(
                                context = this@CarStatusWidgetConfigActivity,
                                definition = PreferencesGlanceStateDefinition,
                                glanceId = glanceId
                            ) { prefs: Preferences ->
                                val mutable = prefs.toMutablePreferences()
                                mutable[KEY_CAR_ID] = car.id   // ✅ 이제 set 연산자 됨
                                mutable                      // ✅ Preferences 리턴(중요!)
                            }

                            // ✅ 위젯 즉시 갱신
                            CarStatusWidget().update(this@CarStatusWidgetConfigActivity, glanceId)

                            WidgetDailyUpdateScheduler.schedule(this@CarStatusWidgetConfigActivity)

                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            )
                            finish()

                        }.onFailure {
                            // 여기서 예외나면 "위젯을 추가할 수 없습니다" 또는 빈 위젯이 됩니다.
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    }
                },
                onCancel = {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            )
        }
    }
}