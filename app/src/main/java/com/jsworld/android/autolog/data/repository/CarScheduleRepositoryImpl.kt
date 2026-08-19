package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.dao.CarScheduleDao
import com.jsworld.android.autolog.data.local.entity.CarScheduleEntity
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.ScheduleType
import com.jsworld.android.autolog.domain.model.nextDueDateAfterDone
import com.jsworld.android.autolog.domain.repository.CarScheduleRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CarScheduleRepositoryImpl @Inject constructor(
    private val dao: CarScheduleDao
) : CarScheduleRepository {

    override fun observeByCar(carId: Long): Flow<List<CarSchedule>> =
        dao.observeByCar(carId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<CarSchedule> =
        dao.getAllForBackup().map { it.toDomain() }

    override suspend fun add(schedule: CarSchedule): Long =
        dao.insert(schedule.toEntity())

    override suspend fun update(schedule: CarSchedule) =
        dao.update(schedule.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun markDone(id: Long): String? {
        val schedule = dao.getById(id)?.toDomain() ?: return null
        val next = nextDueDateAfterDone(schedule, LocalDate.now())
        return if (next == null) {
            dao.deleteById(id)
            null
        } else {
            dao.updateDueDate(id, next.toString())
            next.toString()
        }
    }

    private fun CarScheduleEntity.toDomain() = CarSchedule(
        id = id,
        carId = carId,
        type = ScheduleType.from(type),
        title = title,
        dueDate = dueDate,
        repeatMonths = repeatMonths,
        memo = memo
    )

    private fun CarSchedule.toEntity() = CarScheduleEntity(
        id = id,
        carId = carId,
        type = type.name,
        title = title,
        dueDate = dueDate,
        repeatMonths = repeatMonths,
        memo = memo
    )
}
