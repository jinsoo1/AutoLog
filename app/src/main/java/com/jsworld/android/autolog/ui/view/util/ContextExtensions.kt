package com.jsworld.android.autolog.ui.view.util

import android.content.Context

fun Context.getAppVersionName(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}