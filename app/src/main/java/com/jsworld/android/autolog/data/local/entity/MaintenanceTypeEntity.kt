package com.jsworld.android.autolog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "maintenance_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class MaintenanceTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val defaultIntervalKm: Int?,
    val defaultIntervalMonths: Int?,

    /**
     * 세차·관리 항목인지. 정비 시스템(정비 탭·홈 긴급·알림)에서 완전히 분리되어
     * 세차 허브에서만 다뤄진다.
     *
     * 이름으로 판정하면 "실내 클리닝"처럼 키워드가 없는 항목이 정비로 잡히므로
     * 플래그로 둔다. 기존 데이터는 마이그레이션에서 이름으로 한 번 백필한다.
     */
    val isCare: Boolean = false
)