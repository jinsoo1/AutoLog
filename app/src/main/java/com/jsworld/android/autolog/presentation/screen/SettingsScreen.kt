package com.jsworld.android.autolog.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.presentation.state.BackupUiEvent
import com.jsworld.android.autolog.presentation.scheduler.WeeklyMileageWorkScheduler
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.presentation.viewModel.SettingsViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNoticeClick: () -> Unit,
    onExcelExportClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val backupUiState by viewModel.backupUiState
        .collectAsStateWithLifecycle()

    val notificationEnabled by viewModel.weeklyMileageNotificationEnabled
        .collectAsStateWithLifecycle(initialValue = false)

    // 복원 대상 선택(목록) 다이얼로그 표시 여부
    var showRestorePicker by remember { mutableStateOf(false) }
    // 복원 확인 대상(폴더 목록 또는 SAF에서 선택된 파일)
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    /**
     * JSON 백업 파일 선택
     */
    val restoreFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            pendingRestoreUri = uri
        }

    val enableWeeklyNotification: () -> Unit = remember(context) {
        {
            AutoLogNotificationHelper.createChannels(context)
            WeeklyMileageWorkScheduler.rescheduleNext(context)
            viewModel.setWeeklyMileageNotificationEnabled(true)

            Toast.makeText(
                context,
                "매주 일요일 오후 8시에 알림을 보내드릴게요.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val disableWeeklyNotification: () -> Unit = remember(context) {
        {
            WeeklyMileageWorkScheduler.cancel(context)
            viewModel.setWeeklyMileageNotificationEnabled(false)

            Toast.makeText(
                context,
                "주간 알림이 꺼졌습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    enableWeeklyNotification()
                } else {
                    viewModel.setWeeklyMileageNotificationEnabled(false)

                    Toast.makeText(
                        context,
                        "알림 권한이 허용되지 않아 알림을 켤 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

    val onNotificationToggleChange: (Boolean) -> Unit = { checked ->
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    enableWeeklyNotification()
                } else {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            } else {
                enableWeeklyNotification()
            }
        } else {
            disableWeeklyNotification()
        }
    }

    /**
     * 백업 및 복원 완료 이벤트 처리
     */
    LaunchedEffect(Unit) {
        viewModel.backupEvent.collect { event ->
            when (event) {
                is BackupUiEvent.ExportSuccess -> {
                    val msg = event.location
                        ?.let { "백업을 저장했습니다.\n$it" }
                        ?: "백업 파일을 저장했습니다."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }

                is BackupUiEvent.ExportFailure -> {
                    Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                BackupUiEvent.RestoreSuccess -> {
                    Toast.makeText(
                        context,
                        "백업 데이터를 복원했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 복원된 차량 정보를 기준으로 알림 작업 재등록
                    if (notificationEnabled) {
                        WeeklyMileageWorkScheduler.rescheduleNext(context)
                    }

                    // 사용하는 위젯 갱신 함수가 있다면 여기에 추가
                    // CarStatusWidget().updateAll(context)
                }

                is BackupUiEvent.RestoreFailure -> {
                    Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "설정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 72.dp
                )
            ) {
                item {
                    SettingsSectionTitle("앱 정보")
                }

                item {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Campaign,
                        title = "공지사항",
                        subtitle = "업데이트 및 안내사항 확인",
                        onClick = onNoticeClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionTitle("알림")
                }

                item {
                    SettingsSwitchMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = "주간 주행거리 알림",
                        subtitle = "매주 1회 주행거리 업데이트 여부를 알려드립니다",
                        checked = notificationEnabled,
                        onCheckedChange = onNotificationToggleChange
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionTitle("데이터 관리")
                }

                item {
                    SettingsMenuItem(
                        icon = Icons.Outlined.TableView,
                        title = "엑셀 내보내기",
                        subtitle = "차량을 선택하여 정비내역을 엑셀 파일로 저장",
                        enabled = !backupUiState.isExporting &&
                                !backupUiState.isRestoring,
                        onClick = onExcelExportClick
                    )
                }

                /**
                 * JSON 백업
                 */
                item {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Backup,
                        title = if (backupUiState.isExporting) {
                            "백업 파일 생성 중"
                        } else {
                            "백업 파일 만들기"
                        },
                        subtitle = if (backupUiState.isExporting) {
                            "차량과 정비 기록을 저장하고 있습니다."
                        } else {
                            "다운로드 > AutoLog 폴더에 저장돼요 (복원이 쉬워요)"
                        },
                        enabled = !backupUiState.isExporting &&
                                !backupUiState.isRestoring,
                        onClick = {
                            viewModel.exportToFolder()
                        }
                    )
                }

                /**
                 * JSON 복원
                 */
                item {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Restore,
                        title = if (backupUiState.isRestoring) {
                            "백업 복원 중"
                        } else {
                            "백업 파일 복원"
                        },
                        subtitle = if (backupUiState.isRestoring) {
                            "백업 데이터를 적용하고 있습니다."
                        } else {
                            "AutoLog 폴더의 백업 목록에서 복원"
                        },
                        enabled = !backupUiState.isRestoring &&
                                !backupUiState.isExporting,
                        onClick = {
                            viewModel.refreshBackups()
                            showRestorePicker = true
                        }
                    )
                }
            }

            Text(
                text = "v${viewModel.appVersion}",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = 14.dp
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    /**
     * 복원 대상 선택: AutoLog 폴더의 백업 목록 + 다른 파일에서 복원(SAF)
     */
    if (showRestorePicker) {
        AlertDialog(
            onDismissRequest = { showRestorePicker = false },
            title = { Text("백업에서 복원") },
            text = {
                Column {
                    when {
                        backupUiState.isLoadingBackups -> {
                            Text(
                                text = "불러오는 중…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        backupUiState.backups.isEmpty() -> {
                            Text(
                                text = "이 목록에 표시할 백업이 없습니다.\n\n" +
                                        "앱을 다시 설치했거나 기기를 바꾼 경우, 예전 백업은 " +
                                        "목록에 나타나지 않을 수 있어요. 하지만 파일은 " +
                                        "'다운로드 > AutoLog' 폴더에 그대로 남아 있습니다.\n\n" +
                                        "'다른 파일에서 복원'을 눌러 그 파일을 직접 선택하면 복원할 수 있어요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                backupUiState.backups.forEach { info ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    showRestorePicker = false
                                                    pendingRestoreUri = info.uri
                                                }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = formatBackupDate(info.dateMillis),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = info.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { shareBackup(context, info.uri) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "백업 공유"
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestorePicker = false
                        restoreFileLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/json",
                                "text/plain",
                                "application/octet-stream"
                            )
                        )
                    }
                ) {
                    Text("다른 파일에서 복원")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePicker = false }) {
                    Text("닫기")
                }
            }
        )
    }

    /**
     * 복원 전 경고(전체 교체) 확인 다이얼로그
     */
    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                if (!backupUiState.isRestoring) pendingRestoreUri = null
            },
            title = {
                Text("⚠️ 백업 복원 주의")
            },
            text = {
                Column {
                    Text(
                        text = "현재 저장된 모든 데이터(차량 · 정비 기록 · 주행거리 기록)가 " +
                                "삭제됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "선택한 백업 파일의 데이터로 완전히 교체되며, 이 작업은 " +
                                "되돌릴 수 없습니다.\n\n복원을 계속하시겠습니까?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !backupUiState.isRestoring,
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.restoreBackup(uri)
                    }
                ) {
                    Text(
                        text = "삭제하고 복원",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !backupUiState.isRestoring,
                    onClick = { pendingRestoreUri = null }
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (enabled) 0.7f else 0.35f
                )
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.5f
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    }
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else 0.5f
                        )
                    )
                }
            }

            if (!badgeText.isNullOrBlank()) {
                SettingsBadge(text = badgeText)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
fun SettingsSwitchMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
fun SettingsBadge(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun shareBackup(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "백업 공유"))
}

private fun formatBackupDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat(
        "yyyy.MM.dd HH:mm",
        java.util.Locale.getDefault()
    )
    return formatter.format(java.util.Date(millis))
}