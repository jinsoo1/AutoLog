package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.dao.CareDao
import com.jsworld.android.autolog.data.local.entity.CareItemEntity
import com.jsworld.android.autolog.data.local.entity.CareRecordEntity
import com.jsworld.android.autolog.data.local.entity.CareRecordRow
import com.jsworld.android.autolog.domain.model.CarePickItem
import com.jsworld.android.autolog.domain.model.CareRecord
import com.jsworld.android.autolog.domain.repository.CareRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CareRepositoryImpl @Inject constructor(
    private val careDao: CareDao
) : CareRepository {

    override fun observeItems(carId: Long): Flow<List<CarePickItem>> =
        careDao.observeItems(carId).map { entities ->
            val existing = entities.map { it.toPickItem() }

            // 아직 DB에 없는 기본 항목도 목록에 보여야 켤 수 있다.
            val missingDefaults = DefaultCareItems.items
                .filterNot { name -> existing.any { it.name == name } }
                .map {
                    CarePickItem(
                        name = it,
                        enabled = false,
                        itemId = null,
                        intervalDays = null,
                        intervalWashCount = null
                    )
                }

            val defaultOrder = DefaultCareItems.items.withIndex().associate { (i, n) -> n to i }
            (existing + missingDefaults).sortedWith(
                compareBy({ defaultOrder[it.name] ?: Int.MAX_VALUE }, { it.name })
            )
        }

    override fun observeRecords(carId: Long): Flow<List<CareRecord>> =
        careDao.observeRecords(carId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun setItemEnabled(carId: Long, name: String, enabled: Boolean) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "항목 이름이 비어있어요" }

        val existing = careDao.findItem(carId, trimmed)
        when {
            existing != null -> careDao.setItemActive(existing.id, enabled)
            enabled -> careDao.insertItem(CareItemEntity(carId = carId, name = trimmed))
            // 없는 항목을 끄는 건 할 일이 없다.
        }
    }

    override suspend fun updateInterval(itemId: Long, months: Int?, washCount: Int?) {
        careDao.updateInterval(itemId, months, washCount)
    }

    override suspend fun addRecord(
        carId: Long,
        itemName: String,
        performedAt: String,
        cost: Int?,
        method: String?,
        place: String?,
        memo: String?
    ) {
        val trimmed = itemName.trim()
        require(trimmed.isNotEmpty()) { "항목 이름이 비어있어요" }

        // 항목을 찾거나 만든다 — 꺼져 있던 항목에 기록하면 다시 켠다
        // (기록이 쌓이는 항목이 꺼져 있으면 허브 목록에서 안 보여 혼란스럽다).
        val item = careDao.findItem(carId, trimmed)
        val itemId = if (item == null) {
            careDao.insertItem(CareItemEntity(carId = carId, name = trimmed))
        } else {
            if (!item.isActive) careDao.setItemActive(item.id, true)
            item.id
        }

        careDao.insertRecord(
            CareRecordEntity(
                careItemId = itemId,
                performedAt = performedAt,
                cost = cost,
                method = method?.trim()?.takeIf { it.isNotBlank() },
                place = place?.trim()?.takeIf { it.isNotBlank() },
                memo = memo?.trim()?.takeIf { it.isNotBlank() }
            )
        )
    }

    override suspend fun getRecord(recordId: Long): CareRecord? {
        val entity = careDao.getRecord(recordId) ?: return null
        return CareRecord(
            id = entity.id,
            careItemId = entity.careItemId,
            itemName = "", // 단건 조회에서는 이름이 필요 없다(수정 시트는 항목 고정)
            performedAt = entity.performedAt,
            cost = entity.cost,
            method = entity.method,
            place = entity.place,
            memo = entity.memo
        )
    }

    override suspend fun updateRecord(
        recordId: Long,
        performedAt: String,
        cost: Int?,
        method: String?,
        place: String?,
        memo: String?
    ) {
        val entity = careDao.getRecord(recordId) ?: return
        careDao.updateRecord(
            entity.copy(
                performedAt = performedAt,
                cost = cost,
                method = method?.trim()?.takeIf { it.isNotBlank() },
                place = place?.trim()?.takeIf { it.isNotBlank() },
                memo = memo?.trim()?.takeIf { it.isNotBlank() }
            )
        )
    }

    override suspend fun deleteRecord(recordId: Long) {
        careDao.deleteRecord(recordId)
    }

    private fun CareItemEntity.toPickItem() = CarePickItem(
        name = name,
        enabled = isActive,
        itemId = id.takeIf { isActive },
        intervalDays = intervalDays,
        intervalWashCount = intervalWashCount
    )

    private fun CareRecordRow.toDomain() = CareRecord(
        id = id,
        careItemId = careItemId,
        itemName = itemName,
        performedAt = performedAt,
        cost = cost,
        method = method,
        place = place,
        memo = memo
    )
}
