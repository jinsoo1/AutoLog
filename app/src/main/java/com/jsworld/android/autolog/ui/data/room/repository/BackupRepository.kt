package com.jsworld.android.autolog.ui.data.room.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.jsworld.android.autolog.ui.data.backup.AutoLogBackup
import com.jsworld.android.autolog.ui.data.backup.toBackup
import com.jsworld.android.autolog.ui.data.backup.toEntity
import com.jsworld.android.autolog.ui.data.room.dao.BackupDao
import com.jsworld.android.autolog.ui.data.room.database.AutoLogDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AutoLogDatabase,
    private val backupDao: BackupDao
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    /**
     * 현재 저장된 모든 데이터를 JSON 파일로 내보낸다.
     */
    suspend fun exportBackup(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backup = createBackup()

                val jsonText = json.encodeToString(backup)

                context.contentResolver
                    .openOutputStream(uri, "wt")
                    ?.bufferedWriter(Charsets.UTF_8)
                    ?.use { writer ->
                        writer.write(jsonText)
                    }
                    ?: throw IllegalStateException(
                        "백업 파일을 열 수 없습니다."
                    )
            }
        }

    /**
     * 하나의 트랜잭션 안에서 데이터를 조회한다.
     *
     * 백업 도중 데이터가 수정돼 서로 다른 시점의 데이터가
     * 섞이는 것을 방지한다.
     */
    private suspend fun createBackup(): AutoLogBackup =
        database.withTransaction {
            AutoLogBackup(
                databaseVersion = DATABASE_VERSION,
                createdAt = System.currentTimeMillis(),
                cars = backupDao.getAllCars().map { it.toBackup() },
                maintenanceTypes = backupDao
                    .getAllMaintenanceTypes()
                    .map { it.toBackup() },
                maintenanceSettings = backupDao
                    .getAllMaintenanceSettings()
                    .map { it.toBackup() },
                maintenanceHistories = backupDao
                    .getAllMaintenanceHistories()
                    .map { it.toBackup() },
                mileageHistories = backupDao
                    .getAllMileageHistories()
                    .map { it.toBackup() }
            )
        }

    private suspend fun readBackup(
        uri: Uri
    ): AutoLogBackup =
        withContext(Dispatchers.IO) {

            val jsonText = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: error("백업 파일을 열 수 없습니다.")

            json.decodeFromString<AutoLogBackup>(jsonText)
        }

    private fun validateBackup(
        backup: AutoLogBackup
    ) {

        require(
            backup.backupVersion == AutoLogBackup.CURRENT_BACKUP_VERSION
        ) {
            "지원하지 않는 백업 파일입니다."
        }

        val carIds =
            backup.cars.map { it.id }.toSet()

        val typeIds =
            backup.maintenanceTypes.map { it.id }.toSet()

        val settingIds =
            backup.maintenanceSettings.map { it.id }.toSet()

        backup.maintenanceSettings.forEach {

            require(it.carId in carIds)

            require(it.maintenanceTypeId in typeIds)
        }

        backup.maintenanceHistories.forEach {

            require(it.settingId in settingIds)
        }

        backup.mileageHistories.forEach {

            require(it.carId in carIds)
        }
    }

    suspend fun restoreBackup(
        uri: Uri
    ): Result<Unit> =
        withContext(Dispatchers.IO) {

            runCatching {

                val backup = readBackup(uri)

                validateBackup(backup)

                database.withTransaction {

                    //
                    // 삭제
                    //

                    backupDao.deleteAllMaintenanceHistories()
                    backupDao.deleteAllMileageHistories()
                    backupDao.deleteAllMaintenanceSettings()
                    backupDao.deleteAllMaintenanceTypes()
                    backupDao.deleteAllCars()

                    //
                    // 삽입
                    //

                    backupDao.insertCars(
                        backup.cars.map { it.toEntity() }
                    )

                    backupDao.insertMaintenanceTypes(
                        backup.maintenanceTypes.map {
                            it.toEntity()
                        }
                    )

                    backupDao.insertMaintenanceSettings(
                        backup.maintenanceSettings.map {
                            it.toEntity()
                        }
                    )

                    backupDao.insertMaintenanceHistories(
                        backup.maintenanceHistories.map {
                            it.toEntity()
                        }
                    )

                    backupDao.insertMileageHistories(
                        backup.mileageHistories.map {
                            it.toEntity()
                        }
                    )
                }
            }
        }

    companion object {
        /**
         * 실제 AutoLogDatabase의 현재 버전과 동일하게 맞춘다.
         */
        const val DATABASE_VERSION = 2
    }
}