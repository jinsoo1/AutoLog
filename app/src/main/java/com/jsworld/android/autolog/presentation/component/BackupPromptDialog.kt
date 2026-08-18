package com.jsworld.android.autolog.presentation.component

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jsworld.android.autolog.presentation.viewModel.BackupPromptViewModel

/**
 * 백업 권유 다이얼로그 호스트 — 탭 셸에 놓아두면 조건이 맞을 때 딱 1회 뜬다.
 * 조건·1회 보장은 전부 [BackupPromptViewModel]에 있다.
 */
@Composable
fun BackupPromptHost(viewModel: BackupPromptViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val shouldPrompt by viewModel.shouldPrompt.collectAsState()
    val staleBackup by viewModel.staleBackup.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BackupPromptViewModel.Event.BackupDone ->
                    Toast.makeText(
                        context,
                        "백업 완료 — ${event.path} 에 저장했어요.",
                        Toast.LENGTH_LONG
                    ).show()

                is BackupPromptViewModel.Event.BackupFailed ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (!shouldPrompt) return

    AlertDialog(
        onDismissRequest = { viewModel.dismiss() },
        title = {
            Text(
                if (staleBackup) "백업이 오래됐어요" else "백업 파일을 만들어둘까요?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                if (staleBackup) {
                    "마지막 백업 이후 기록이 많이 쌓였어요. 오토로그의 데이터는 이 휴대폰에만 " +
                        "저장되기 때문에, 백업 파일이 최신이어야 그만큼 되살릴 수 있어요.\n\n" +
                        "지금 백업하면 '다운로드 > AutoLog' 폴더에 새 파일로 저장됩니다."
                } else {
                    "기록이 쌓이기 시작했어요. 오토로그의 데이터는 이 휴대폰에만 저장되기 때문에, " +
                        "휴대폰을 잃어버리거나 앱을 삭제하면 되돌릴 수 없어요.\n\n" +
                        "지금 백업하면 '다운로드 > AutoLog' 폴더에 파일로 저장되고, " +
                        "기기를 바꿀 때 그 파일 하나로 복원할 수 있습니다.\n\n" +
                        "설정 > 데이터 관리에서도 언제든 백업할 수 있어요."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { viewModel.backupNow() }) {
                Text("지금 백업", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismiss() }) {
                Text("나중에")
            }
        }
    )
}
