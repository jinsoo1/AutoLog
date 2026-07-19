package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.MaintenanceType
import kotlinx.coroutines.flow.Flow

interface MaintenanceTypeRepository {
    fun getTypes(): Flow<List<MaintenanceType>>
    suspend fun addType(name: String, intervalKm: Int? = null, intervalMonths: Int? = null)
    suspend fun updateType(type: MaintenanceType)
    suspend fun deleteType(type: MaintenanceType)
    fun observeAllTypes(): Flow<List<MaintenanceType>>
    suspend fun ensureDefaultTypes()
    fun observeType(typeId: Long): Flow<MaintenanceType?>
}
