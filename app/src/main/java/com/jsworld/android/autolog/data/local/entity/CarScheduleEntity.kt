package com.jsworld.android.autolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 날짜 기반 일정 — 정기검사·보험 만기·자동차세처럼 주행거리와 무관하게
 * 날짜로만 오는 일들. 정비(주기)·세차(경과일)와 결이 달라 전용 테이블로 둔다.
 *
 * 프리셋(검사/보험/세금)은 **추가 화면의 템플릿일 뿐** 저장 구조는 전부 같은 행이다 —
 * 자동차세도 "12월 16일, 6개월마다"라는 반복 일정로 표현되므로 특수 로직이 없다.
 */
@Entity(
    tableName = "car_schedules",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class CarScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val carId: Long,

    /** ScheduleType 의 name. 프리셋 구분·아이콘용이고 동작은 전 타입 동일 */
    val type: String,

    /** 표시 이름 — 프리셋도 저장해 둔다(나중에 프리셋 이름이 바뀌어도 기존 일정은 유지) */
    val title: String,

    /** 다음 도래일 yyyy-MM-dd */
    val dueDate: String,

    /**
     * 반복 주기(개월). 완료 처리하면 dueDate 에 더해 다음 회차로 넘어간다.
     * null = 반복 없음(완료하면 삭제).
     */
    val repeatMonths: Int? = null,

    /** 보험사 이름, 검사소 등 자유 메모 */
    val memo: String? = null
)
