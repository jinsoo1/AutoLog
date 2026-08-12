package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.domain.model.CareRecord
import kotlinx.coroutines.flow.Flow

/**
 * 세차·관리 — 정비와 완전히 분리된 자체 저장소.
 * 항목은 차량별(care_items)이고, 주기는 "세차 N회마다" 또는 "N개월마다"를 가진다.
 */
interface CareRepository {

    /** 허브의 항목 목록 — DB에 있는 항목 + 아직 안 켠 기본 항목(끔 상태) */
    fun observeItems(carId: Long): Flow<List<CarePickItem>>

    /** 이 차량의 세차·관리 기록 전부(최신순) */
    fun observeRecords(carId: Long): Flow<List<CareRecord>>

    suspend fun setItemEnabled(carId: Long, name: String, enabled: Boolean)

    /** 주기 설정. 둘 다 null 이면 기록 전용 */
    suspend fun updateInterval(itemId: Long, months: Int?, washCount: Int?)

    /** 기록 저장 — 항목은 이름으로 찾거나 만든다(켜지지 않았어도 기록하면 켜진다) */
    suspend fun addRecord(
        carId: Long,
        itemName: String,
        performedAt: String,
        cost: Int?,
        method: String?,
        place: String?,
        memo: String?
    )

    suspend fun getRecord(recordId: Long): CareRecord?

    suspend fun updateRecord(
        recordId: Long,
        performedAt: String,
        cost: Int?,
        method: String?,
        place: String?,
        memo: String?
    )

    suspend fun deleteRecord(recordId: Long)
}
