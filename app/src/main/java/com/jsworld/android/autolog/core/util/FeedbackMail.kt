package com.jsworld.android.autolog.core.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.jsworld.android.autolog.R

/** 개선 의견을 받는 주소 — 스토어 리뷰 답글에 안내한 주소와 같아야 한다 */
const val FEEDBACK_EMAIL = "wow91820@gmail.com"

/**
 * 메일 앱으로 '개선 및 의견' 메일을 연다.
 *
 * 본문 아래에 앱 버전·기기·Android 버전을 미리 적어둔다 — 문제 제보가 오면
 * 되묻지 않고 바로 재현해볼 수 있게. 사용자가 지워도 된다고 함께 적는다.
 * 메일 앱이 없으면 주소를 클립보드에 복사하고 알려준다.
 */
fun Context.openFeedbackMail() {
    val body = getString(
        R.string.feedback_mail_body,
        getAppVersionName(),
        "${Build.MANUFACTURER} ${Build.MODEL}",
        Build.VERSION.RELEASE
    )
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_mail_subject))
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("email", FEEDBACK_EMAIL))
        Toast.makeText(this, getString(R.string.feedback_no_mail_app, FEEDBACK_EMAIL), Toast.LENGTH_LONG).show()
    }
}
