package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.Symptom

interface SymptomGuideRepository {
    suspend fun loadSymptoms(): List<Symptom>
}
