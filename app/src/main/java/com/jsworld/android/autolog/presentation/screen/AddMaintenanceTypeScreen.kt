package com.jsworld.android.autolog.presentation.screen

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jsworld.android.autolog.presentation.component.ThousandsSeparatorTransformation
import com.jsworld.android.autolog.presentation.viewModel.AddMaintenanceTypeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceTypeScreen(
    carId: Long,
    viewModel: AddMaintenanceTypeViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    var name by rememberSaveable { mutableStateOf("") }
    var defaultKmText by rememberSaveable { mutableStateOf("") }
    var defaultMonthsText by rememberSaveable { mutableStateOf("") }

    var useCarOverride by rememberSaveable { mutableStateOf(false) }
    var carKmText by rememberSaveable { mutableStateOf("") }
    var carMonthsText by rememberSaveable { mutableStateOf("") }

    fun parseInt(s: String): Int? = s.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    val defaultKm = parseInt(defaultKmText)
    val defaultMonths = parseInt(defaultMonthsText)
    val carKm = parseInt(carKmText)
    val carMonths = parseInt(carMonthsText)

    val canSave =
        name.trim().isNotEmpty() &&
                (defaultKm != null || defaultMonths != null) &&
                (!useCarOverride || (carKm != null || carMonths != null))

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is AddMaintenanceTypeViewModel.UiEvent.Snackbar ->
                    snackbarHostState.showSnackbar(e.message)
                AddMaintenanceTypeViewModel.UiEvent.Done -> onDone()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.at_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            viewModel.save(
                                carId = carId,
                                name = name,
                                defaultKm = defaultKm,
                                defaultMonths = defaultMonths,
                                useCarOverride = useCarOverride,
                                carKm = carKm,
                                carMonths = carMonths
                            )
                        }
                    ) { Text(stringResource(R.string.common_save)) }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()              // 키보드 올라오면 위로
                    .navigationBarsPadding()   // 하단 내비 영역까지 안전
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 입력 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.car_section_basic), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.at_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = defaultKmText,
                            onValueChange = { defaultKmText = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.at_default_km)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsSeparatorTransformation,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defaultMonthsText,
                            onValueChange = { defaultMonthsText = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.at_default_months)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.at_override_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.at_override_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = useCarOverride, onCheckedChange = { useCarOverride = it })
                    }

                    if (useCarOverride) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = carKmText,
                                onValueChange = { carKmText = it.filter(Char::isDigit) },
                                label = { Text(stringResource(R.string.at_car_km)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = ThousandsSeparatorTransformation,
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = carMonthsText,
                                onValueChange = { carMonthsText = it.filter(Char::isDigit) },
                                label = { Text(stringResource(R.string.at_car_months)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 팁 카드(요청하신 “추가 팁”)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.at_tips_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.at_tips_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
