package com.jsworld.android.autolog.ui.view.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.items
import com.jsworld.android.autolog.ui.data.item.ExcelExportUiState
import com.jsworld.android.autolog.ui.view.viewModel.ExcelExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelExportScreen(
    onBackClick: () -> Unit,
    viewModel: ExcelExportViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val selectedCarId by viewModel.selectedCarId.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            viewModel.exportSelectedCarToUri(uri)
        }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExcelExportUiState.Success -> {
                Toast.makeText(
                    context,
                    "엑셀 파일이 저장되었습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.resetExportState()
            }

            is ExcelExportUiState.Error -> {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.resetExportState()
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "엑셀 내보내기",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Button(
                    onClick = {
                        val fileName = viewModel.createExcelFileName()

                        if (fileName == null) {
                            Toast.makeText(
                                context,
                                "차량을 먼저 선택해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            createExcelLauncher.launch(fileName)
                        }
                    },
                    enabled = selectedCarId != null &&
                            exportState !is ExcelExportUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // 네비바 안전
                        .imePadding()            // 키보드 안전
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (exportState is ExcelExportUiState.Loading) {
                            "엑셀 저장 중..."
                        } else {
                            "선택한 차량 엑셀로 저장"
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (cars.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 차량이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 88.dp
                )
            ) {
                item {
                    Text(
                        text = "엑셀로 저장할 차량을 선택해주세요.",
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(
                    items = cars,
                    key = { car -> car.id }
                ) { car ->
                    ExcelExportCarItem(
                        carName = car.name,
                        plate = car.plate,
                        mileage = car.mileage,
                        selected = selectedCarId == car.id,
                        onClick = {
                            viewModel.selectCar(car.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcelExportCarItem(
    carName: String,
    plate: String,
    mileage: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = carName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$plate · ${mileage}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}