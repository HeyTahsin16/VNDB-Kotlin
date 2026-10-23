package com.vndblite.dcj.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.domain.repository.LogoutCollectionOption
import com.vndblite.dcj.sync.SyncStage
import com.vndblite.dcj.ui.format.toUiText
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
fun AuthSyncScreen(
    uiState: AuthSyncUiState,
    onAction: (AuthSyncAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_sync_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = themeSpec.accentColor
                    )
                }
                Text(
                    text = stringResource(R.string.user_authentication),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeSpec.accentColor
                )
            }
        }

        val activeAccount = uiState.activeAccount
        if (activeAccount != null) {
            item {
                ConnectedAccountCard(
                    uiState = uiState,
                    onAction = onAction
                )
            }
        } else {
            item {
                TokenSetupCard(
                    uiState = uiState,
                    onAction = onAction,
                    onOpenVndbTokensPage = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://vndb.org/u/tokens"))
                            )
                        } catch (e: Exception) {
                            AppLog.w("AuthSyncScreen", "Failed to open VNDB tokens page", e)
                        }
                    }
                )
            }
        }

        if (uiState.syncProgress.isSyncing) {
            item {
                SyncProgressCard(uiState = uiState)
            }
        }

        uiState.lastSyncReport?.let { report ->
            item {
                SyncReportCard(
                    report = report,
                    onRetryFailed = { onAction(AuthSyncAction.SyncNow) }
                )
            }
        }

        uiState.error?.let { error ->
            item {
                Surface(
                    color = ColorTokens.DestructiveRed.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ColorTokens.DestructiveRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error.toUiText().asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeSpec.contentColor
                        )
                    }
                }
            }
        }
    }

    if (uiState.showLogoutDialog) {
        LogoutChoiceDialog(
            onKeepLocal = { onAction(AuthSyncAction.ConfirmLogout(LogoutCollectionOption.KEEP_LOCAL_COLLECTION)) },
            onClearLocal = { onAction(AuthSyncAction.ConfirmLogout(LogoutCollectionOption.CLEAR_LOCAL_COLLECTION)) },
            onDismiss = { onAction(AuthSyncAction.DismissLogoutDialog) }
        )
    }
}

@Composable
private fun ConnectedAccountCard(
    uiState: AuthSyncUiState,
    onAction: (AuthSyncAction) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val account = uiState.activeAccount ?: return

    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ColorTokens.SuccessGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auth_connected_as, account.username, account.id),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeSpec.contentColor
                )
            }

            if (account.hasFullListPermissions) {
                Text(
                    text = stringResource(R.string.auth_permissions_ok),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.accentColor
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.auth_permissions_missing,
                        account.missingListPermissions.joinToString(", ")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.DestructiveRed
                )
            }

            Button(
                onClick = { onAction(AuthSyncAction.SyncNow) },
                enabled = !uiState.syncProgress.isSyncing && account.hasFullListPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeSpec.accentColor,
                    contentColor = themeSpec.onAccentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_sync_now_button")
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auth_sync_now), fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onAction(AuthSyncAction.RequestLogout) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTokens.DestructiveRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_logout_button")
            ) {
                Text(stringResource(R.string.auth_logout))
            }
        }
    }
}

@Composable
private fun TokenSetupCard(
    uiState: AuthSyncUiState,
    onAction: (AuthSyncAction) -> Unit,
    onOpenVndbTokensPage: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_token_help),
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor
            )
            OutlinedButton(
                onClick = onOpenVndbTokensPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.auth_open_vndb_tokens),
                    color = themeSpec.accentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = themeSpec.accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            OutlinedTextField(
                value = uiState.tokenInput,
                onValueChange = { onAction(AuthSyncAction.UpdateTokenInput(it)) },
                label = { Text(stringResource(R.string.auth_token_label)) },
                placeholder = { Text(stringResource(R.string.auth_token_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onAction(AuthSyncAction.ToggleTokenVisibility) }) {
                        Icon(
                            imageVector = if (uiState.isTokenVisible) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (uiState.isTokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                colors = vndbOutlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_token_input")
            )
            Button(
                onClick = { onAction(AuthSyncAction.VerifyAndSaveToken) },
                enabled = uiState.tokenInput.isNotBlank() && !uiState.isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeSpec.accentColor,
                    contentColor = themeSpec.onAccentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_verify_save_button")
            ) {
                Text(
                    text = if (uiState.isVerifying) {
                        stringResource(R.string.label_loading)
                    } else {
                        stringResource(R.string.auth_verify_and_save)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SyncProgressCard(uiState: AuthSyncUiState) {
    val themeSpec = LocalThemeSpec.current
    val progress = uiState.syncProgress
    val stageMessage = when (progress.stage) {
        SyncStage.AUTHENTICATING -> stringResource(R.string.sync_stage_validating)
        SyncStage.FETCHING_REMOTE_LIST, SyncStage.PLANNING -> stringResource(R.string.sync_stage_fetching)
        SyncStage.PUSHING_LOCAL_CHANGES -> stringResource(
            R.string.sync_stage_pushing,
            progress.completedItems,
            progress.totalItems
        )
        SyncStage.DELETING_REMOTE -> stringResource(
            R.string.sync_stage_deleting,
            progress.completedItems,
            progress.totalItems
        )
        SyncStage.APPLYING_LOCAL_UPDATES,
        SyncStage.CACHING_SUMMARIES,
        SyncStage.IDLE,
        SyncStage.COMPLETED,
        SyncStage.FAILED -> stringResource(R.string.sync_stage_applying)
    }

    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stageMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                color = themeSpec.accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SyncReportCard(
    report: com.vndblite.dcj.domain.model.SyncReport,
    onRetryFailed: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val isSuccess = report.isCompleteSuccess
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sync_report_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val summaryText = if (isSuccess) {
                stringResource(
                    R.string.sync_report_complete,
                    report.pushed,
                    report.pulled,
                    report.deletedRemote,
                    report.deletedLocal
                )
            } else {
                stringResource(
                    R.string.sync_report_partial,
                    report.pushed,
                    report.pulled,
                    report.failed.size
                )
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSuccess) themeSpec.contentColor else ColorTokens.WarningAmber
            )
            if (!isSuccess) {
                Button(
                    onClick = onRetryFailed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeSpec.accentColor,
                        contentColor = themeSpec.onAccentColor
                    ),
                    modifier = Modifier.testTag("sync_retry_failed_button")
                ) {
                    Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LogoutChoiceDialog(
    onKeepLocal: () -> Unit,
    onClearLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeSpec.surfaceColor,
        titleContentColor = themeSpec.contentColor,
        textContentColor = themeSpec.contentColor,
        title = {
            Text(
                text = stringResource(R.string.auth_logout_title),
                style = MaterialTheme.typography.titleLarge,
                color = themeSpec.accentColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.auth_logout_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeSpec.contentColor
                )
                Button(
                    onClick = onKeepLocal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeSpec.accentColor,
                        contentColor = themeSpec.onAccentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_keep_local_button")
                ) {
                    Text(stringResource(R.string.auth_logout_keep_local), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClearLocal,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTokens.DestructiveRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_clear_local_button")
                ) {
                    Text(stringResource(R.string.auth_logout_clear_local), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = themeSpec.contentColor)
            }
        }
    )
}
