package com.jsworld.android.autolog.presentation.component

import androidx.compose.ui.res.stringResource
import com.jsworld.android.autolog.R
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
                        context.getString(R.string.backup_prompt_done, event.path),
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
                stringResource(if (staleBackup) R.string.backup_prompt_title_stale else R.string.backup_prompt_title_first),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(
                    if (staleBackup) R.string.backup_prompt_body_stale else R.string.backup_prompt_body_first
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { viewModel.backupNow() }) {
                Text(stringResource(R.string.backup_prompt_now), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismiss() }) {
                Text(stringResource(R.string.common_later))
            }
        }
    )
}
