package com.vndblite.dcj.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
internal fun ConnectedAccountCard(
    account: UserAccount,
    isSynced: Boolean,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onRequestLogout: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = semantic.statusFinished,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.auth_connected_heading),
                        style = MaterialTheme.typography.titleLarge,
                        color = spec.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.auth_username_line, account.username, account.id),
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.onSurfaceColor
                    )
                    val statusLabel = if (isSynced) {
                        stringResource(R.string.auth_status_synced)
                    } else {
                        stringResource(R.string.auth_status_pending)
                    }
                    Text(
                        text = stringResource(R.string.auth_status_line, statusLabel),
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.onSurfaceColor.copy(alpha = 0.75f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = spec.accentColor,
                        contentColor = semantic.onAccentButton
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("auth_sync_now_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = semantic.onAccentButton,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_sync_in_progress), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.auth_sync_now), fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onRequestLogout,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.danger),
                    border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("auth_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.auth_sign_out))
                }
            }
        }
    }
}

@Composable
internal fun TokenSignInCard(
    tokenInput: String,
    showToken: Boolean,
    isSyncing: Boolean,
    onTokenChange: (String) -> Unit,
    onToggleShowToken: () -> Unit,
    onSubmit: () -> Unit,
    onOpenVndbInBrowser: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_token_card_heading),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.auth_token_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor.copy(alpha = 0.85f)
            )
            OutlinedTextField(
                value = tokenInput,
                onValueChange = onTokenChange,
                label = { Text(stringResource(R.string.auth_token_label)) },
                placeholder = { Text(stringResource(R.string.auth_token_placeholder)) },
                singleLine = true,
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, tint = spec.accentColor)
                },
                trailingIcon = {
                    IconButton(onClick = onToggleShowToken) {
                        Icon(
                            imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(
                                if (showToken) R.string.auth_token_hide_cd else R.string.auth_token_show_cd
                            ),
                            tint = spec.onSurfaceColor
                        )
                    }
                },
                colors = vndbOutlinedTextFieldColors(spec),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_token_input")
            )
            Button(
                onClick = onSubmit,
                enabled = !isSyncing && tokenInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = spec.accentColor,
                    contentColor = semantic.onAccentButton
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_submit_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = semantic.onAccentButton,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sync_stage_authenticating), fontWeight = FontWeight.Bold)
                } else {
                    Text(stringResource(R.string.auth_sign_in_and_sync), fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = onOpenVndbInBrowser,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = spec.accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.auth_open_vndb_button), color = spec.accentColor)
            }
        }
    }
}

@Composable
internal fun SyncStatusBanner(
    message: String,
    isRunning: Boolean,
    isError: Boolean,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    val bannerColor = if (isError) semantic.danger else spec.accentColor
    Surface(
        color = bannerColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    color = spec.accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = bannerColor
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (canRetry) {
                OutlinedButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = spec.accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.dialog_retry), color = spec.accentColor)
                }
            }
        }
    }
}

@Composable
internal fun AccountLogoutDialog(
    onKeepLocal: () -> Unit,
    onClearLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = spec.surfaceColor,
        titleContentColor = spec.accentColor,
        textContentColor = spec.onSurfaceColor,
        title = {
            Text(
                text = stringResource(R.string.dialog_logout_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_logout_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onKeepLocal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = spec.accentColor,
                        contentColor = semantic.onAccentButton
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.dialog_logout_keep_local), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onClearLocal,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.danger),
                    border = BorderStroke(1.dp, semantic.danger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.dialog_logout_clear_local), fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.dialog_cancel), color = spec.onSurfaceColor)
                }
            }
        }
    )
}
