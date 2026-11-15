package com.jsworld.android.autolog.core.util

import android.content.Context
import java.io.InputStream

/**
 * 콘텍츠 JSON(증상 가이드·공지)을 현재 언어 판으로 연다.
 *
 * `symptom_guide.json` 을 요청하면 표시 언어가 일본어일 때 `symptom_guide_ja.json` 이
 * assets 에 있으면 그것을, 없으면 원본을 돌려준다. 번역 파일을 **넣기만 하면** 붙도록
 * 만든 훅이다 — 문장이 많은 콘텐츠는 코드와 다른 속도로 번역되기 때문이다.
 */
fun Context.openLocalizedAsset(fileName: String): InputStream {
    val language = resources.configuration.locales[0]?.language
        ?: return assets.open(fileName)
    val dot = fileName.lastIndexOf('.')
    val localized =
        if (dot > 0) fileName.substring(0, dot) + "_" + language + fileName.substring(dot)
        else "${fileName}_$language"
    return runCatching { assets.open(localized) }.getOrElse { assets.open(fileName) }
}

/** 원본 콘텐츠 JSON 이 쓰인 언어 — 이 언어에서는 번역 파일이 없어도 그대로 쓸 수 있다 */
private const val CONTENT_SOURCE_LANGUAGE = "ko"

/**
 * 현재 표시 언어로 읽을 수 있는 콘텐츠인가.
 *
 * 원문 언어(한국어)이거나 `_ja.json` 같은 번역 파일이 assets 에 있으면 true.
 * 증상 가이드 진입점을 숨기거나 공지에 "한국어로만 제공" 안내를 붙일지 정하는 기준이다 —
 * 조건을 "일본어 모드"가 아니라 **파일 존재**로 두어, 번역 파일을 넣기만 하면 UI 가 저절로 켜진다.
 */
fun Context.isAssetAvailableInCurrentLanguage(fileName: String): Boolean {
    val language = resources.configuration.locales[0]?.language ?: return true
    if (language == CONTENT_SOURCE_LANGUAGE) return true
    val dot = fileName.lastIndexOf('.')
    val localized =
        if (dot > 0) fileName.substring(0, dot) + "_" + language + fileName.substring(dot)
        else "${fileName}_$language"
    return runCatching { assets.open(localized).close(); true }.getOrDefault(false)
}
