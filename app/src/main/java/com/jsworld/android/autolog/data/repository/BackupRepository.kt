package com.jsworld.android.autolog.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.jsworld.android.autolog.data.repository.AutoLogBackup
import com.jsworld.android.autolog.data.repository.toBackup
import com.jsworld.android.autolog.data.repository.toEntity
import com.jsworld.android.autolog.data.local.dao.BackupDao
import com.jsworld.android.autolog.data.local.db.AutoLogDatabase
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
                careItems = backupDao.getAllCareItems().map { it.toBackup() },
                careRecords = backupDao.getAllCareRecords().map { it.toBackup() },
                mileageHistories = backupDao
                    .getAllMileageHistories()
                    .map { it.toBackup() },
                fuelRecords = backupDao
                    .getAllFuelRecords()
                    .map { it.toBackup() },
                schedules = backupDao.getAllSchedules().map { it.toBackup() }
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

        backup.fuelRecords.forEach {

            require(it.carId in carIds)
        }

        val careItemIds = backup.careItems.map { it.id }.toSet()
        backup.careItems.forEach { require(it.carId in carIds) }
        backup.careRecords.forEach { require(it.careItemId in careItemIds) }

        backup.schedules.forEach { require(it.carId in carIds) }
    }

    suspend fun restoreBackup(
        uri: Uri
    ): Result<Unit> =
        withContext(Dispatchers.IO) {

            runCatching {

                // v4 이전 백업은 세차가 정비 목록에 섞여 있다 — 새 구조로 옮긴다.
                val backup = readBackup(uri).withLegacyCareConverted()

                validateBackup(backup)

                database.withTransaction {

                    //
                    // 삭제
                    //

                    backupDao.deleteAllSchedules()
                    backupDao.deleteAllCareRecords()
                    backupDao.deleteAllCareItems()
                    backupDao.deleteAllFuelRecords()
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

                    backupDao.insertFuelRecords(
                        backup.fuelRecords.map {
                            it.toEntity()
                        }
                    )

                    backupDao.insertCareItems(
                        backup.careItems.map { it.toEntity() }
                    )

                    backupDao.insertCareRecords(
                        backup.careRecords.map { it.toEntity() }
                    )

                    backupDao.insertSchedules(
                        backup.schedules.map { it.toEntity() }
                    )
                }
            }
        }

    /**
     * Download/AutoLog 폴더에 JSON 백업을 저장한다. (사용자가 저장 위치를 직접 고르지 않아도 됨)
     * @return 저장된 상대 경로 (예: "Download/AutoLog/AutoLog_Backup_...json")
     */
    suspend fun exportToAutoLogFolder(): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backup = createBackup()
                val jsonText = json.encodeToString(backup)

                val fileName = buildBackupFileName()
                val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_DIR_NAME"
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val itemUri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("백업 파일을 만들 수 없습니다.")

                try {
                    resolver.openOutputStream(itemUri)
                        ?.bufferedWriter(Charsets.UTF_8)
                        ?.use { it.write(jsonText) }
                        ?: throw IllegalStateException("백업 파일을 열 수 없습니다.")

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                } catch (t: Throwable) {
                    // 쓰기 도중 실패하면 보이지 않는 미완성(pending) 파일이 남으므로 정리한다.
                    runCatching { resolver.delete(itemUri, null, null) }
                    throw t
                }

                "$relativePath/$fileName"
            }
        }

    /**
     * Download/AutoLog 폴더에서 이 앱이 만든 백업 파일 목록을 최신순으로 가져온다.
     */
    suspend fun listAutoLogBackups(): List<BackupFileInfo> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.DATE_MODIFIED
            )
            val selection =
                "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND " +
                        "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%$BACKUP_DIR_NAME/%", "AutoLog_Backup%")
            val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"

            val result = mutableListOf<BackupFileInfo>()
            resolver.query(collection, projection, selection, args, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    result.add(
                        BackupFileInfo(
                            uri = uri,
                            displayName = cursor.getString(nameCol),
                            dateMillis = cursor.getLong(dateCol) * 1000L
                        )
                    )
                }
            }
            result
        }

    /**
     * 복원하기 **전에** 백업 파일의 내용을 훑어본다(적용하지 않음).
     *
     * 복원은 전체 교체라서, 예컨대 주유 기록이 없던 예전 백업을 복원하면
     * 지금 쌓아둔 주유 기록이 사라진다. 사용자가 그걸 모르고 누르지 않도록
     * 확인 다이얼로그에서 미리 보여주기 위한 조회다.
     */
    suspend fun peekBackup(uri: Uri): Result<BackupPreview> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backup = readBackup(uri)

                BackupPreview(
                    createdAt = backup.createdAt,
                    backupVersion = backup.backupVersion,
                    databaseVersion = backup.databaseVersion,
                    carCount = backup.cars.size,
                    maintenanceHistoryCount = backup.maintenanceHistories.size,
                    fuelRecordCount = backup.fuelRecords.size,
                    currentFuelRecordCount = backupDao.countFuelRecords()
                )
            }
        }

    /**
     * 전체 기록 수(정비+주유+세차) — 백업 권유 다이얼로그의 기준점.
     * 백업 성공 시점의 값을 저장해 두고, 그보다 한참 더 쌓이면 다시 권한다.
     */
    suspend fun countAllRecords(): Int =
        withContext(Dispatchers.IO) {
            backupDao.countMaintenanceHistories() +
                backupDao.countFuelRecords() +
                backupDao.countCareRecords()
        }

    private fun buildBackupFileName(): String {
        val formatter = java.text.SimpleDateFormat(
            "yyyy-MM-dd_HHmmss",
            java.util.Locale.getDefault()
        )
        return "AutoLog_Backup_${formatter.format(java.util.Date())}.json"
    }

    companion object {
        /**
         * 실제 AutoLogDatabase의 현재 버전과 동일하게 맞춘다.
         */
        const val DATABASE_VERSION = 5

        /** Download 하위 백업 폴더 이름 */
        const val BACKUP_DIR_NAME = "AutoLog"
    }
}

/**
 * 복원 전에 보여줄 백업 요약.
 *
 * [fuelRecordCount] 가 0 인데 [currentFuelRecordCount] 가 0 보다 크면,
 * 복원하면 현재 주유 기록이 사라진다는 뜻이다.
 */
data class BackupPreview(
    val createdAt: Long,
    val backupVersion: Int,
    val databaseVersion: Int,
    val carCount: Int,
    val maintenanceHistoryCount: Int,
    val fuelRecordCount: Int,
    val currentFuelRecordCount: Int
) {
    /** 복원 시 현재 주유 기록만 사라지는 상황인지 */
    val losesFuelRecords: Boolean
        get() = fuelRecordCount == 0 && currentFuelRecordCount > 0
}

/** Download/AutoLog 폴더의 백업 파일 정보 */
data class BackupFileInfo(
    val uri: Uri,
    val displayName: String,
    val dateMillis: Long
)