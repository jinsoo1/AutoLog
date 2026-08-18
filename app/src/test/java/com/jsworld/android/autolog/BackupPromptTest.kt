package com.jsworld.android.autolog

import com.jsworld.android.autolog.presentation.viewModel.BackupPromptViewModel.Companion.shouldPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 백업 권유 판정 — 틀리면 "조용히 안 뜨는" 버그라 여기서 잠근다.
 * 규칙: 기록 3건부터 첫 권유 / 이후는 기준점(마지막 권유·백업 때의 기록 수)에서
 * +30건 쌓일 때마다 다시 권유. 백업하면 기준점이 리셋되므로 꾸준히 백업하면 안 뜬다.
 */
class BackupPromptTest {

    @Test
    fun `기록 3건부터 첫 권유`() {
        assertFalse(shouldPrompt(records = 2, baseline = 0))
        assertTrue(shouldPrompt(records = 3, baseline = 0))
    }

    @Test
    fun `나중에를 눌러도 기록이 30건 더 쌓이면 다시 권한다`() {
        // 3건에서 거절 → 32건까지는 침묵
        assertFalse(shouldPrompt(records = 32, baseline = 3))
        // 33건(3+30)부터 한 번 더
        assertTrue(shouldPrompt(records = 33, baseline = 3))
        // 다시 거절하면 다음 기준은 63건
        assertFalse(shouldPrompt(records = 62, baseline = 33))
        assertTrue(shouldPrompt(records = 63, baseline = 33))
    }

    @Test
    fun `백업 직후에는 뜨지 않고, 그 뒤 30건 쌓이면 백업이 오래됐다고 권한다`() {
        // 40건에서 백업 → 기준점 40
        assertFalse(shouldPrompt(records = 40, baseline = 40))
        assertFalse(shouldPrompt(records = 69, baseline = 40))
        assertTrue(shouldPrompt(records = 70, baseline = 40))
    }

    @Test
    fun `복원 등으로 기록이 줄어도 기준점보다 적으면 침묵`() {
        assertFalse(shouldPrompt(records = 10, baseline = 40))
    }
}
