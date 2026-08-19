package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.CarSchedule
import kotlinx.coroutines.flow.Flow

interface CarScheduleRepository {
    fun observeByCar(carId: Long): Flow<List<CarSchedule>>

    /** 알림 워커용 — 전 차량 */
    suspend fun getAll(): List<CarSchedule>

    suspend fun add(schedule: CarSchedule): Long
    suspend fun update(schedule: CarSchedule)
    suspend fun delete(id: Long)

    /**
     * 완료 처리 — 반복 일정이면 다음 회차로 넘기고, 아니면 삭제한다.
     * @return 다음 도래일. 삭제됐으면 null
     */
    suspend fun markDone(id: Long): String?
}
