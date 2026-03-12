package com.jsworld.android.autolog.ui.data.room.repository

import com.jsworld.android.autolog.ui.data.default.DefaultMaintenanceItems
import com.jsworld.android.autolog.ui.data.item.MaintenanceType
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.ui.data.room.mapper.toDomain
import com.jsworld.android.autolog.ui.data.room.mapper.toEntity
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MaintenanceTypeRepository @Inject constructor(
    private val maintenanceTypeDao: MaintenanceTypeDao
) {

    /** 전체 정비 타입 가져오기 (엔진오일, 타이어 등) */
    fun getTypes(): Flow<List<MaintenanceType>> =
        maintenanceTypeDao.getAllTypes()
            .map { list -> list.map { it.toDomain() } }

    /** 새 정비 타입 추가 */
    suspend fun addType(
        name: String,
        intervalKm: Int? = null,
        intervalMonths: Int? = null
    ) {
        maintenanceTypeDao.insertType(
            MaintenanceTypeEntity(
                name = name,
                defaultIntervalKm = intervalKm,
                defaultIntervalMonths = intervalMonths
            )
        )
    }

    /** 타입 수정 */
    suspend fun updateType(type: MaintenanceType) {
        maintenanceTypeDao.updateType(type.toEntity())
    }

    /** 타입 삭제 */
    suspend fun deleteType(type: MaintenanceType) {
        maintenanceTypeDao.deleteType(type.toEntity())
    }

    fun observeAllTypes(): Flow<List<MaintenanceType>> =
        maintenanceTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun ensureDefaultTypes() {
        if (maintenanceTypeDao.count() > 0) return

        val defaults = DefaultMaintenanceItems.items.map { (name, pair) ->
            MaintenanceTypeEntity(
                name = name,
                defaultIntervalKm = pair.first,
                defaultIntervalMonths = pair.second
            )
        }
        maintenanceTypeDao.insertAll(defaults)
    }

    fun observeType(typeId: Long): Flow<MaintenanceType?> =
        maintenanceTypeDao.observeByTypeId(typeId).map { it?.toDomain() }


}