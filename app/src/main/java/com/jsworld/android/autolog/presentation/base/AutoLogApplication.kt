package com.jsworld.android.autolog.presentation.base

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jsworld.android.autolog.core.util.WeeklyNotificationStartupManager
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class AutoLogApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var weeklyNotificationStartupManager: WeeklyNotificationStartupManager

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("AutoLogApp", "Application onCreate")

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            weeklyNotificationStartupManager.restoreIfNeeded(this@AutoLogApplication)
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            android.util.Log.d("AutoLogApp", "Providing WorkManager config")
            return Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setWorkerFactory(workerFactory)
                .build()
        }
}