package com.jsworld.android.autolog.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.getAppVersionName(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * Compose 의 LocalContext 는 Activity 가 아니라 래퍼일 수 있다.
 * 권한 재요청 가능 여부(shouldShowRequestPermissionRationale)를 물으려면 Activity 가 필요하다.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
