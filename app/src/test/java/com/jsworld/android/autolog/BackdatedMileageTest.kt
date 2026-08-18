package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.suggestBackdatedMileage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 과거 날짜 기록의 주행거리 제안 — 제안값은 정확하지 않아도 되지만
 * 이웃 기록과의 순서(단조 증가)는 절대 깨면 안 된다.
 */
class BackdatedMileageTest {

    @Test
    fun `직전 기록이 있으면 +1`() {
        assertEquals(38_501, suggestBackdatedMileage(prevMileage = 38_500, nextMileage = null))
    }

    @Test
    fun `직전이 없으면 다음 기록 -1`() {
        assertEquals(41_254, suggestBackdatedMileage(prevMileage = null, nextMileage = 41_255))
    }

    @Test
    fun `앞뒤 다 있으면 그 사이 - 직전+1이 다음을 넘지 않는다`() {
        assertEquals(38_501, suggestBackdatedMileage(prevMileage = 38_500, nextMileage = 41_255))
        // 앞뒤가 같은 값이면 +1이 다음을 넘으므로 다음 값으로 잘린다
        assertEquals(38_500, suggestBackdatedMileage(prevMileage = 38_500, nextMileage = 38_500))
    }

    @Test
    fun `이웃이 없으면 지어내지 않는다`() {
        assertNull(suggestBackdatedMileage(prevMileage = null, nextMileage = null))
    }

    @Test
    fun `다음 기록이 0이어도 음수가 되지 않는다`() {
        assertEquals(0, suggestBackdatedMileage(prevMileage = null, nextMileage = 0))
    }
}
