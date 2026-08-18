package com.jsworld.android.autolog.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.data.repository.BackupRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 백업 권유 다이얼로그 — 자동 백업 대신 "잃으면 아까운 만큼 쌓인 시점"마다 권한다.
 *
 * 규칙은 하나다: **기준점보다 기록이 [REPROMPT_GROWTH]건 더 쌓이면 권유.**
 * 기준점은 마지막 권유 또는 마지막 백업 시점의 기록 수 — 백업하면 기준점이
 * 리셋되므로 꾸준히 백업하는 사람은 다시 보지 않고, 백업 후 방치하면
 * 몇 달에 한 번씩 다시 권한다. (첫 권유만 [PROMPT_RECORD_THRESHOLD]건 문턱)
 */
@HiltViewModel
class BackupPromptViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    sealed interface Event {
        data class BackupDone(val path: String) : Event
        data class BackupFailed(val message: String) : Event
    }

    private val _shouldPrompt = MutableStateFlow(false)
    val shouldPrompt: StateFlow<Boolean> = _shouldPrompt.asStateFlow()

    /** 첫 권유가 아니라 "백업이 오래됨" 권유인지 — 다이얼로그 문구가 달라진다 */
    private val _staleBackup = MutableStateFlow(false)
    val staleBackup: StateFlow<Boolean> = _staleBackup.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    /** 이번에 권유했을 때의 기록 수 — 닫을 때 저장해 다음 기준점으로 쓴다 */
    private var promptedAtCount = 0

    init {
        viewModelScope.launch {
            runCatching {
                val hasBackedUp = userPrefsRepository.observeLastBackupAt().first() > 0L
                val baseline = userPrefsRepository.observeBackupPromptRecordCount().first()
                val records = backupRepository.countAllRecords()

                // 이 기능이 생기기 전(또는 설정 경로로) 백업한 사용자 — 그 시점의
                // 기록 수를 모르므로 지금을 기준점으로 삼고 이번엔 권하지 않는다.
                if (hasBackedUp && baseline == 0) {
                    userPrefsRepository.setBackupPromptRecordCount(records)
                    return@launch
                }

                promptedAtCount = records
                _staleBackup.value = hasBackedUp
                _shouldPrompt.value = shouldPrompt(records, baseline)
            }
        }
    }

    /** '나중에'/바깥 탭 — 기준점만 갱신. 기록이 또 +30건 쌓이면 다시 권한다 */
    fun dismiss() {
        _shouldPrompt.value = false
        viewModelScope.launch {
            userPrefsRepository.setBackupPromptRecordCount(promptedAtCount)
        }
    }

    /** '지금 백업' — 설정의 빠른 백업과 같은 경로(다운로드/AutoLog 폴더) */
    fun backupNow() {
        _shouldPrompt.value = false
        viewModelScope.launch {
            backupRepository.exportToAutoLogFolder()
                .onSuccess { path ->
                    userPrefsRepository.setLastBackupAt(System.currentTimeMillis())
                    userPrefsRepository.setBackupPromptRecordCount(promptedAtCount)
                    _events.send(Event.BackupDone(path))
                }
                .onFailure { e ->
                    // 실패하면 기준점을 갱신하지 않는다 — 다음 실행 때 다시 권한다.
                    _events.send(Event.BackupFailed(e.message ?: "백업 파일 생성에 실패했습니다."))
                }
        }
    }

    companion object {
        /**
         * 첫 권유 문턱. 기록 1~2건에 권하면 "백업할 것도 없는데"가 되고,
         * 너무 늦으면 그 전에 잃는다.
         */
        const val PROMPT_RECORD_THRESHOLD = 3

        /** 기준점에서 다시 권하기까지 더 쌓여야 하는 기록 수 — 몇 달치 사용량 */
        const val REPROMPT_GROWTH = 30

        /**
         * 순수 판정 — 테스트로 지킨다.
         * @param baseline 마지막 권유·백업 때의 기록 수 (0 = 처음)
         */
        fun shouldPrompt(records: Int, baseline: Int): Boolean {
            if (records < PROMPT_RECORD_THRESHOLD) return false
            return baseline == 0 || records >= baseline + REPROMPT_GROWTH
        }
    }
}
