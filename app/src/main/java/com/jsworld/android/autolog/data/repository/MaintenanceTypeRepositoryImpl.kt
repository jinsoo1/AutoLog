package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.domain.repository.MaintenanceTypeRepository

import com.jsworld.android.autolog.data.repository.DefaultMaintenanceItems
import com.jsworld.android.autolog.domain.model.MaintenanceType
import com.jsworld.android.autolog.data.local.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.mapper.toDomain
import com.jsworld.android.autolog.data.mapper.toEntity
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MaintenanceTypeRepositoryImpl @Inject constructor(
    private val maintenanceTypeDao: MaintenanceTypeDao
) : MaintenanceTypeRepository {

    /** 전체 정비 타입 가져오기 (엔진오일, 타이어 등) */
    override fun getTypes(): Flow<List<MaintenanceType>> =
        maintenanceTypeDao.getAllTypes()
            .map { list -> list.map { it.toDomain() } }

    /** 새 정비 타입 추가 */
    override suspend fun addType(
        name: String,
        intervalKm: Int?,
        intervalMonths: Int?
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
    override suspend fun updateType(type: MaintenanceType) {
        maintenanceTypeDao.updateType(type.toEntity())
    }

    /** 타입 삭제 */
    override suspend fun deleteType(type: MaintenanceType) {
        maintenanceTypeDao.deleteType(type.toEntity())
    }

    override fun observeAllTypes(): Flow<List<MaintenanceType>> =
        maintenanceTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureDefaultTypes() {
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

    override fun observeType(typeId: Long): Flow<MaintenanceType?> =
        maintenanceTypeDao.observeByTypeId(typeId).map { it?.toDomain() }


}