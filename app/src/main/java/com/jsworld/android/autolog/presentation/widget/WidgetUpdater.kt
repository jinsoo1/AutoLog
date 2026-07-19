package com.jsworld.android.autolog.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        scope.launch {
            requests
                .debounce(500)
                .collect {
                    CarStatusWidget().updateAll(context)
                }
        }
    }

    fun requestUpdate() {
        requests.tryEmit(Unit)
    }
}