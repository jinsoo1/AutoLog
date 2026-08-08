package com.jsworld.android.autolog.domain.model

enum class MaintenanceStatus { NORMAL, SOON, OVERDUE }

data class MaintenanceUiModel(
    /** 이 항목을 눌렀을 때 바로 기록/상세로 갈 수 있도록 함께 넘긴다. */
    val settingId: Long,
    val name: String,
    val status: MaintenanceStatus,
    val remainingText: String,
    /**
     * 주기 소진율(0~1). 주기가 km·개월 둘 다 있으면 더 많이 소진된 쪽을 쓴다.
     * 주기가 없으면 null.
     */
    val progressRatio: Float? = null
)
