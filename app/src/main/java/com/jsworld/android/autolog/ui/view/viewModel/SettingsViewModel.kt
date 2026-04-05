package com.jsworld.android.autolog.ui.view.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.room.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPrefsRepository: UserPrefsRepository
) : AndroidViewModel(application) {

    val appVersion: String = run {
        val context = getApplication<Application>()
        runCatching {
            val pm = context.packageManager
            val pkg = pm.getPackageInfo(context.packageName, 0)
            pkg.versionName ?: ""
        }.getOrDefault("")
    }

    val weeklyMileageNotificationEnabled: Flow<Boolean> =
        userPrefsRepository.observeWeeklyMileageNotificationEnabled()

    fun setWeeklyMileageNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setWeeklyMileageNotificationEnabled(enabled)
        }
    }
}