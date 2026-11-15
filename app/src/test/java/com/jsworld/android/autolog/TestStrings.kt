package com.jsworld.android.autolog

import com.jsworld.android.autolog.core.ui.UiText
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 단위 테스트에서 [UiText] 를 **한국어 문장으로 굳히는** 도우미.
 *
 * 도메인 문장 빌더가 리소스 id 를 돌려주게 바뀌었지만, 테스트는 여전히
 * "세차 3회마다 · 마지막 7월 10일"처럼 사람이 읽는 문장을 검증해야 한다 —
 * 문구 자체가 제품이기 때문이다. Android 런타임 없이 기본 strings.xml 을 직접 읽어
 * R.string 의 id → 이름 → 문구로 풀고, 자바 String.format 으로 인자를 채운다.
 */
object TestStrings {

    private val idToName: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    private val nameToValue: Map<String, String> by lazy {
        val candidates = listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("strings.xml not found from ${File(".").absolutePath}")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        buildMap {
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val name = node.attributes.getNamedItem("name").nodeValue
                put(name, node.textContent.unescapeAndroid())
            }
        }
    }

    private fun String.unescapeAndroid(): String =
        replace("\\n", "\n").replace("\\'", "'").replace("\\\"", "\"")

    fun UiText.ko(): String = when (this) {
        is UiText.Raw -> value
        is UiText.Res -> {
            val name = idToName[id] ?: error("unknown string id $id")
            val pattern = nameToValue[name] ?: error("no value for R.string.$name")
            if (args.isEmpty()) pattern
            else String.format(
                Locale.KOREA,
                pattern,
                *args.map { if (it is UiText) it.ko() else it }.toTypedArray()
            )
        }
        is UiText.Joined -> parts.joinToString(separator) { it.ko() }
    }
}
