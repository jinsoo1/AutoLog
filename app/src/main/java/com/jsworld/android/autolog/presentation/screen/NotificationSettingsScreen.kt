package com.jsworld.android.autolog.presentation.screen

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.asString
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jsworld.android.autolog.presentation.state.BackupUiEvent
import com.jsworld.android.autolog.presentation.state.RestorePreviewUiState
import com.jsworld.android.autolog.BuildConfig
import com.jsworld.android.autolog.presentation.scheduler.MaintenanceAlertScheduler
import com.jsworld.android.autolog.presentation.scheduler.MonthlyReportScheduler
import com.jsworld.android.autolog.presentation.scheduler.WeeklyMileageWorkScheduler
import androidx.core.app.ActivityCompat
import com.jsworld.android.autolog.presentation.component.TabContentTopPadding
import com.jsworld.android.autolog.presentation.component.TabTopBar
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.core.util.findActivity
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
import com.jsworld.android.autolog.presentation.viewModel.SettingsViewModel

/**
 * 알림 설정 — 설정 화면에서 떼어낸 화면.
 *
 * 설정 1,610줄 중 3분의 1이 알림이었다. 토글 5개에 하위 항목과 디버그 항목이 붙고,
 * 권한 처리(종류별 런처 3개 · 화면 복귀 시 재확인 · 거부 시 되돌리기)까지 본문 상단에
 * 얹혀 있어서 설정 화면을 읽을 수 없게 만들고 있었다.
 *
 * 권한 로직은 알림 말고 쓰는 곳이 없으므로 이 화면이 통째로 갖는다.
 * 상태는 SettingsViewModel 을 그대로 쓴다 — 알림 설정만 따로 떼어낼 이유가 없다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val notificationEnabled by viewModel.weeklyMileageNotificationEnabled
        .collectAsStateWithLifecycle(initialValue = false)

    val alertPrefs by viewModel.maintenanceAlertPrefs
        .collectAsStateWithLifecycle(initialValue = MaintenanceAlertPrefs())

    val monthlyReportEnabled by viewModel.monthlyReportNotificationEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    val scheduleAlertEnabled by viewModel.scheduleAlertEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    val seasonalCareAlertEnabled by viewModel.seasonalCareAlertEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    var showAlertHourDialog by remember { mutableStateOf(false) }
    var showAlertRemindDialog by remember { mutableStateOf(false) }

    // 앱에서 알림을 켰는데도 시스템이 막고 있으면 사용자는 "안 온다"고만 느낀다 —
    // 시스템 설정에서 바꾸고 돌아올 수 있으니 화면에 돌아올 때마다 다시 확인한다.
    var notificationBlock by remember {
        mutableStateOf(AutoLogNotificationHelper.NotificationBlock.NONE)
    }
    // 기본 켜짐인 알림(일정·리포트)은 권한이 없으면 "켜져 있는데 안 오는" 상태가 된다.
    // 스위치는 사용자의 의사(ON)를 그대로 두되, 아직 보낼 수 없다는 사실을 그 자리에서 밝힌다.
    var notificationsAllowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        onPauseOrDispose { }
    }

    val defaultOnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            notificationsAllowed = granted
            if (granted) {
                AutoLogNotificationHelper.createChannels(context)
                return@rememberLauncherForActivityResult
            }

            // 두 번 거부하면 시스템이 요청 창을 아예 띄우지 않는다 —
            // 눌러도 아무 일도 안 일어난 것처럼 보이므로, 그때는 설정 화면으로 보낸다.
            // (거부 직후 rationale 이 false 면 "다시 물어볼 수 없는 상태"다)
            val canAskAgain = context.findActivity()?.let { activity ->
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } ?: false

            if (!canAskAgain) {
                runCatching {
                    context.startActivity(
                        AutoLogNotificationHelper.notificationSettingsIntent(context)
                    )
                }.onFailure {
                    Toast.makeText(
                        context,
                        context.getString(R.string.notif_enable_in_system),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    )

    // 월간 리포트는 기본 켜짐이라 여기 넣으면 권한 없는 모든 사용자에게 경고가 뜬다 —
    // 배너는 사용자가 직접 켠 기능이 막혔을 때만.
    val anyNotificationOn = alertPrefs.enabled || notificationEnabled
    LifecycleResumeEffect(anyNotificationOn) {
        notificationBlock = if (anyNotificationOn) {
            AutoLogNotificationHelper.checkBlocked(
                context,
                listOf(
                    AutoLogNotificationHelper.MAINT_SOON_CHANNEL_ID,
                    AutoLogNotificationHelper.MAINT_OVERDUE_CHANNEL_ID,
                    AutoLogNotificationHelper.WEEKLY_MILEAGE_CHANNEL_ID,
                    AutoLogNotificationHelper.MONTHLY_REPORT_CHANNEL_ID
                )
            )
        } else {
            AutoLogNotificationHelper.NotificationBlock.NONE
        }
        onPauseOrDispose { }
    }

    // 복원 대상 선택(목록) 다이얼로그 표시 여부

    val enableWeeklyNotification: () -> Unit = remember(context) {
        {
            AutoLogNotificationHelper.createChannels(context)
            WeeklyMileageWorkScheduler.rescheduleNext(context)
            viewModel.setWeeklyMileageNotificationEnabled(true)

            Toast.makeText(
                context,
                context.getString(R.string.notif_weekly_on),
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
                context.getString(R.string.notif_weekly_off),
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
                        context.getString(R.string.notif_permission_denied),
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
     * 정비 임박/초과 알림 — 마스터 스위치를 켜는 순간 채널 생성 + 일일 검사 예약.
     * (remember 로 감싸지 않는다 — alertPrefs.hour 최신값을 잡아야 해서)
     */
    val maintOnToast = stringResource(R.string.notif_maint_on, formatAlertHour(alertPrefs.hour))
    val enableMaintenanceAlert: () -> Unit = {
        AutoLogNotificationHelper.createChannels(context)
        MaintenanceAlertScheduler.reschedule(context, alertPrefs.hour)
        viewModel.setMaintenanceAlertEnabled(true)
        Toast.makeText(
            context,
            maintOnToast,
            Toast.LENGTH_SHORT
        ).show()
    }

    val disableMaintenanceAlert: () -> Unit = {
        MaintenanceAlertScheduler.cancel(context)
        viewModel.setMaintenanceAlertEnabled(false)
        Toast.makeText(context, context.getString(R.string.notif_maint_off), Toast.LENGTH_SHORT).show()
    }

    val maintenanceAlertPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    enableMaintenanceAlert()
                } else {
                    viewModel.setMaintenanceAlertEnabled(false)
                    Toast.makeText(
                        context,
                        context.getString(R.string.notif_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

    val onMaintenanceAlertToggleChange: (Boolean) -> Unit = { checked ->
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    enableMaintenanceAlert()
                } else {
                    maintenanceAlertPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            } else {
                enableMaintenanceAlert()
            }
        } else {
            disableMaintenanceAlert()
        }
    }

    /**
     * 월간 리포트 알림 — 기본 켜짐이라 대개는 끄는 쪽만 쓰인다.
     * 다시 켤 때는 다른 알림처럼 권한을 확인하고 예약을 새로 건다.
     */
    val enableMonthlyReport: () -> Unit = {
        AutoLogNotificationHelper.createChannels(context)
        MonthlyReportScheduler.reschedule(context)
        viewModel.setMonthlyReportNotificationEnabled(true)
        Toast.makeText(context, context.getString(R.string.notif_monthly_on), Toast.LENGTH_SHORT).show()
    }

    val monthlyReportPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    enableMonthlyReport()
                } else {
                    viewModel.setMonthlyReportNotificationEnabled(false)
                    Toast.makeText(
                        context,
                        context.getString(R.string.notif_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

    val onMonthlyReportToggleChange: (Boolean) -> Unit = { checked ->
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    enableMonthlyReport()
                } else {
                    monthlyReportPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            } else {
                enableMonthlyReport()
            }
        } else {
            MonthlyReportScheduler.cancel(context)
            viewModel.setMonthlyReportNotificationEnabled(false)
            Toast.makeText(context, context.getString(R.string.notif_monthly_off), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_notification),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionTitle(stringResource(R.string.settings_section_notification))
                }

                if (notificationBlock != AutoLogNotificationHelper.NotificationBlock.NONE) {
                    item {
                        NotificationBlockedCard(
                            block = notificationBlock,
                            onOpenSettings = {
                                val channelId =
                                    if (notificationBlock ==
                                        AutoLogNotificationHelper.NotificationBlock.CHANNEL_BLOCKED
                                    ) {
                                        AutoLogNotificationHelper.MAINT_OVERDUE_CHANNEL_ID
                                    } else {
                                        null
                                    }
                                runCatching {
                                    context.startActivity(
                                        AutoLogNotificationHelper
                                            .notificationSettingsIntent(context, channelId)
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.notif_open_system_failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }
                }

                item {
                    SettingsSwitchMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = stringResource(R.string.notif_weekly_title),
                        subtitle = stringResource(R.string.notif_weekly_sub),
                        checked = notificationEnabled,
                        onCheckedChange = onNotificationToggleChange
                    )
                }

                item {
                    SettingsSwitchMenuItem(
                        icon = Icons.Outlined.NotificationsActive,
                        title = stringResource(R.string.notif_maint_title),
                        subtitle = stringResource(R.string.notif_maint_sub),
                        checked = alertPrefs.enabled,
                        onCheckedChange = onMaintenanceAlertToggleChange
                    )
                }

                if (alertPrefs.enabled) {
                    item {
                        SettingsSwitchMenuItem(
                            icon = Icons.Outlined.Notifications,
                            title = stringResource(R.string.notif_soon_title),
                            subtitle = stringResource(R.string.notif_soon_sub),
                            checked = alertPrefs.soonEnabled,
                            indented = true,
                            onCheckedChange = viewModel::setMaintenanceAlertSoonEnabled
                        )
                    }

                    item {
                        SettingsSwitchMenuItem(
                            icon = Icons.Outlined.NotificationImportant,
                            title = stringResource(R.string.notif_overdue_title),
                            subtitle = stringResource(R.string.notif_overdue_sub),
                            checked = alertPrefs.overdueEnabled,
                            indented = true,
                            onCheckedChange = viewModel::setMaintenanceAlertOverdueEnabled
                        )
                    }

                    item {
                        SettingsMenuItem(
                            icon = Icons.Outlined.Schedule,
                            title = stringResource(R.string.notif_time_title),
                            subtitle = stringResource(R.string.notif_time_sub, formatAlertHour(alertPrefs.hour)),
                            indented = true,
                            onClick = { showAlertHourDialog = true }
                        )
                    }

                    item {
                        SettingsMenuItem(
                            icon = Icons.Outlined.Repeat,
                            title = stringResource(R.string.notif_remind_title),
                            subtitle = if (alertPrefs.remindDays == 0) stringResource(R.string.notif_remind_none_sub)
                            else stringResource(R.string.notif_remind_days_sub, alertPrefs.remindDays),
                            indented = true,
                            onClick = { showAlertRemindDialog = true }
                        )
                    }

                    // 알림 테스트 — 디버그 빌드 전용. 릴리즈에는 나타나지 않는다.
                    if (BuildConfig.DEBUG) {
                        item {
                            SettingsMenuItem(
                                icon = Icons.Outlined.BugReport,
                                title = stringResource(R.string.notif_test_title),
                                subtitle = stringResource(R.string.notif_test_sub),
                                indented = true,
                                onClick = {
                                    MaintenanceAlertScheduler.enqueueTest(context)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.notif_test_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                item {
                    SettingsSwitchMenuItem(
                        icon = Icons.Outlined.CalendarMonth,
                        title = stringResource(R.string.notif_schedule_title),
                        subtitle = stringResource(R.string.notif_schedule_sub),
                        checked = scheduleAlertEnabled,
                        onCheckedChange = { checked ->
                            if (checked) AutoLogNotificationHelper.createChannels(context)
                            viewModel.setScheduleAlertEnabled(checked)
                        }
                    )
                }

                if (scheduleAlertEnabled && !notificationsAllowed) {
                    item {
                        PermissionNeededRow(
                            onClick = {
                                defaultOnPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        )
                    }
                }

                item {
                    SettingsSwitchMenuItem(
                        icon = Icons.Outlined.WbSunny,
                        title = stringResource(R.string.notif_seasonal_title),
                        subtitle = stringResource(R.string.notif_seasonal_sub),
                        checked = seasonalCareAlertEnabled,
                        onCheckedChange = { checked ->
                            if (checked) AutoLogNotificationHelper.createChannels(context)
                            viewModel.setSeasonalCareAlertEnabled(checked)
                        }
                    )
                }

                if (seasonalCareAlertEnabled && !notificationsAllowed) {
                    item {
                        PermissionNeededRow(
                            onClick = {
                                defaultOnPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        )
                    }
                }

                // 계절 알림 테스트 — 계절마다 1회뿐이라 기다려서는 확인할 수 없다.
                // 디버그 빌드 전용. 릴리즈에는 나타나지 않는다.
                if (BuildConfig.DEBUG && seasonalCareAlertEnabled) {
                    item {
                        SettingsMenuItem(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(R.string.notif_seasonal_test_title),
                            subtitle = stringResource(R.string.notif_seasonal_test_sub),
                            indented = true,
                            onClick = {
                                AutoLogNotificationHelper.createChannels(context)
                                // forceTest — '이번 계절에 보냈나' 기록을 건드리지 않는다.
                                viewModel.sendSeasonalTestNotification()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.notif_seasonal_test_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                item {
                    SettingsSwitchMenuItem(
                        icon = Icons.Outlined.BarChart,
                        title = stringResource(R.string.notif_monthly_title),
                        subtitle = stringResource(R.string.notif_monthly_sub),
                        checked = monthlyReportEnabled,
                        onCheckedChange = onMonthlyReportToggleChange
                    )
                }

                if (monthlyReportEnabled && !notificationsAllowed) {
                    item {
                        PermissionNeededRow(
                            onClick = {
                                defaultOnPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        )
                    }
                }

                // 리포트 알림 테스트 — 디버그 빌드 전용. 릴리즈에는 나타나지 않는다.
                if (BuildConfig.DEBUG && monthlyReportEnabled) {
                    item {
                        SettingsMenuItem(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(R.string.notif_monthly_test_title),
                            subtitle = stringResource(R.string.notif_monthly_test_sub),
                            indented = true,
                            onClick = {
                                MonthlyReportScheduler.enqueueTest(context)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.notif_test_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
        }
    }

    if (showAlertHourDialog) {
        AlertDialog(
            onDismissRequest = { showAlertHourDialog = false },
            title = { Text(stringResource(R.string.notif_time_title)) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    (6..22).forEach { hour ->
                        val selected = hour == alertPrefs.hour
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setMaintenanceAlertHour(hour)
                                    MaintenanceAlertScheduler.reschedule(context, hour)
                                    showAlertHourDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                formatAlertHour(hour),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAlertHourDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    /**
     * 초과 리마인드 주기 선택
     */
    if (showAlertRemindDialog) {
        AlertDialog(
            onDismissRequest = { showAlertRemindDialog = false },
            title = { Text(stringResource(R.string.notif_remind_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.notif_remind_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    MaintenanceAlertPrefs.REMIND_OPTIONS.forEach { days ->
                        val selected = days == alertPrefs.remindDays
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setMaintenanceAlertRemindDays(days)
                                    showAlertRemindDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                MaintenanceAlertPrefs.remindLabel(days).asString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAlertRemindDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}
