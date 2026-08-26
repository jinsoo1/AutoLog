package com.jsworld.android.autolog.presentation.model

import androidx.compose.ui.graphics.Color
import com.jsworld.android.autolog.domain.model.SymptomRiskLevel
import com.jsworld.android.autolog.presentation.theme.StatusNormal
import com.jsworld.android.autolog.presentation.theme.StatusOverdue
import com.jsworld.android.autolog.presentation.theme.StatusRestrict
import com.jsworld.android.autolog.presentation.theme.StatusSoon

/** 주행위험도 단계별 상태색 — 정비 임박/초과와 같은 상태색 계열을 쓴다 */
fun SymptomRiskLevel.riskColor(): Color = when (this) {
    SymptomRiskLevel.CHECK -> StatusNormal
    SymptomRiskLevel.CAUTION -> StatusSoon
    SymptomRiskLevel.MINIMIZE -> StatusRestrict
    SymptomRiskLevel.STOP -> StatusOverdue
}
