package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.MaintenanceAlertNotifiedState
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.shouldNotifyMaintenanceAlert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 알림 스팸 방지 규칙.
 *
 * "상태가 바뀔 때 1회"가 깨지면 매일 같은 알림이 가고,
 * 사용자는 앱 알림을 통째로 차단한다. 여기가 이 기능의 생명선이다.
 */
class MaintenanceAlertDecisionTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000L * day

    @Test
    fun `처음 임박해지면 알린다`() {
        assertTrue(shouldNotifyMaintenanceAlert(MaintenanceStatus.SOON, null, 0, now))
    }

    @Test
    fun `같은 상태가 이어지면 침묵한다`() {
        val prev = MaintenanceAlertNotifiedState("SOON", now - day)
        assertFalse(shouldNotifyMaintenanceAlert(MaintenanceStatus.SOON, prev, 0, now))

        val prevOverdue = MaintenanceAlertNotifiedState("OVERDUE", now - day)
        assertFalse(shouldNotifyMaintenanceAlert(MaintenanceStatus.OVERDUE, prevOverdue, 0, now))
    }

    @Test
    fun `임박에서 초과로 전이하면 다시 알린다`() {
        val prev = MaintenanceAlertNotifiedState("SOON", now - day)
        assertTrue(shouldNotifyMaintenanceAlert(MaintenanceStatus.OVERDUE, prev, 0, now))
    }

    @Test
    fun `초과 리마인드 - 주기가 지나면 다시, 안 지났으면 침묵`() {
        val prev = MaintenanceAlertNotifiedState("OVERDUE", now - 7 * day)
        assertTrue(shouldNotifyMaintenanceAlert(MaintenanceStatus.OVERDUE, prev, 7, now))

        val recent = MaintenanceAlertNotifiedState("OVERDUE", now - 6 * day)
        assertFalse(shouldNotifyMaintenanceAlert(MaintenanceStatus.OVERDUE, recent, 7, now))
    }

    @Test
    fun `리마인드는 초과에만 적용된다 - 임박은 주기가 지나도 침묵`() {
        val prev = MaintenanceAlertNotifiedState("SOON", now - 30 * day)
        assertFalse(shouldNotifyMaintenanceAlert(MaintenanceStatus.SOON, prev, 7, now))
    }

    @Test
    fun `리마인드 안 함이면 초과가 아무리 오래돼도 침묵`() {
        val prev = MaintenanceAlertNotifiedState("OVERDUE", now - 365 * day)
        assertFalse(shouldNotifyMaintenanceAlert(MaintenanceStatus.OVERDUE, prev, 0, now))
    }
}
