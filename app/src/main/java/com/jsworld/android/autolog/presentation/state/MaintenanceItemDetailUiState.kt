package com.jsworld.android.autolog.presentation.state

import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import com.jsworld.android.autolog.domain.model.MaintenanceStatus

data class MaintenanceItemDetailUiState(
    val loading: Boolean = true,
    val carId: Long = 0L,
    val typeName: String = "정비 항목",

    /** 실제 적용되는 주기(차량 설정값이 없으면 항목 기본값) */
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    /** 차량별로 따로 정한 주기 없이 기본값을 쓰는 중인지 */
    val usingDefaultIntervals: Boolean = true,

    val status: MaintenanceStatus = MaintenanceStatus.NORMAL,
    val remainingText: String = "",
    val progressRatio: Float? = null,

    val lastServiceMileage: Int? = null,
    val nextDueMileage: Int? = null,

    val histories: List<MaintenanceHistory> = emptyList(),

    /** 실제 교체 간격 평균(기록 2건 이상일 때만) */
    val averageIntervalKm: Int? = null,
    /** 비용이 적힌 기록들의 평균 */
    val averageCost: Int? = null
) {
    /**
     * 주기가 전혀 없는 항목 = 일회성 수리.
     * 임박 계산에서 제외되므로 상태 카드 대신 수리 안내를 보여준다.
     */
    val isRepair: Boolean
        get() = !loading && intervalKm == null && intervalMonths == null
}
