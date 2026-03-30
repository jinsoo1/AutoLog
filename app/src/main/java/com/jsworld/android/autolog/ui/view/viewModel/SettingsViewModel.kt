package com.jsworld.android.autolog.ui.view.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.jsworld.android.autolog.ui.view.util.getAppVersionName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val appVersion = context.getAppVersionName()

}