package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceTypeDao {

    @Query("SELECT * FROM maintenance_types ORDER BY name ASC")
    fun getAllTypes(): Flow<List<MaintenanceTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertType(type: MaintenanceTypeEntity): Long

    @Query("SELECT * FROM maintenance_types WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): MaintenanceTypeEntity?

    @Update
    suspend fun updateType(type: MaintenanceTypeEntity)

    @Delete
    suspend fun deleteType(type: MaintenanceTypeEntity)

    @Query("SELECT * FROM maintenance_types WHERE id IN (:ids)")
    suspend fun getTypesByIds(ids: List<Long>): List<MaintenanceTypeEntity>

    @Query("SELECT * FROM maintenance_types ORDER BY name")
    fun observeAll(): Flow<List<MaintenanceTypeEntity>>

    @Query("SELECT COUNT(*) FROM maintenance_types")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(list: List<MaintenanceTypeEntity>)

    @Query("SELECT * FROM maintenance_types WHERE id = :typeId")
    fun observeByTypeId(typeId: Long): Flow<MaintenanceTypeEntity?>



}
