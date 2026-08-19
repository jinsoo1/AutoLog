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
    val progressRatio: Float? = null,
    /**
     * 기록이 하나라도 있는지. 기록이 없으면 0km/오늘 기준으로 계산돼
     * 즉시 초과로 보이는데, 푸시 알림은 이런 항목을 걸러야 한다
     * (항목만 켜둔 사용자에게 초과 알림을 쏟아내면 안 된다).
     */
    val hasHistory: Boolean = true,
    /**
     * 남은 거리(km)·남은 일수. 주기가 없는 쪽은 null, 음수면 이미 지난 것.
     *
     * [remainingText] 는 사람이 읽는 문장이라 계산에 쓸 수 없어서 숫자로도 함께 내보낸다
     * (리포트의 정비 시기 예측이 월평균 주행거리와 나눠 쓴다).
     */
    val remainingKm: Int? = null,
    val remainingDays: Long? = null
)
