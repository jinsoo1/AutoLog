package com.jsworld.android.autolog.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

/**
 * 주행거리 빠른 수정. 홈 탭의 주행거리 카드에서 띄운다.
 * 가장 최근 정비 기록보다 낮은 값은 모순이므로 막는다.
 */
@Composable
internal fun MileageQuickEditDialog(
    currentMileage: Int,
    minAllowedMileage: Int?,   // nullable
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    // 숫자만 보관하고, 표시용 TextFieldValue 로 콤마 포맷을 유지한다(커서 점프 방지)
    var digits by rememberSaveable(currentMileage) { mutableStateOf(currentMileage.toString()) }
    var field by remember(currentMileage) {
        mutableStateOf(TextFieldValue(currentMileage.formatKm()))
    }
    val parsed = digits.toIntOrNull()

    val isBelowMin = minAllowedMileage != null && parsed != null && parsed < minAllowedMileage
    val canSave = parsed != null && parsed >= 0 && !isBelowMin

    val minText = minAllowedMileage?.formatKm()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주행거리 업데이트", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "현재 값: ${currentMileage.formatKm()} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = field,
                    onValueChange = { input ->
                        val d = input.text.filter(Char::isDigit)
                        digits = d
                        val formatted = d.toIntOrNull()?.formatKm() ?: ""
                        field = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    },
                    label = { Text("새 주행거리(km)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isBelowMin,
                    supportingText = {
                        when {
                            digits.isBlank() -> Text("숫자만 입력해 주세요.")
                            parsed == null -> Text("올바른 숫자를 입력해 주세요.")
                            isBelowMin -> Text(
                                "가장 최근 정비 기록(${minText} km)보다 낮게 설정할 수 없어요.",
                                color = MaterialTheme.colorScheme.error
                            )
                            minAllowedMileage == null -> Text(
                                "정비 기록이 아직 없어서 자유롭게 입력할 수 있어요.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> Text(
                                "정비 기록(최소 ${minText} km) 이상으로 입력해 주세요.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val v = parsed ?: return@Button
                    if (minAllowedMileage != null && v < minAllowedMileage) return@Button // 방어
                    onSave(v)
                }
            ) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
private fun Int.formatKm(): String = NumberFormat.getIntegerInstance().format(this)
