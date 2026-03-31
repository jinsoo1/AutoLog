package com.jsworld.android.autolog.ui.data.item

data class Car(
    val id: Long,
    val name: String,
    val plate: String,
    val year: String?,
    val mileage: Int,
    val fuelType: String?,
    val notes: String?,
    val isPrimary: Boolean = false,
    val lastMileageUpdatedAt: Long? = null
)