package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

/**
 * Reusable confirmation dialog using semantic theme tokens and >= 48.dp touch targets (F9, U1, U2, U6).
 */
@Composable
fun VndbConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.dialog_confirm),
    dismissLabel: String = stringResource(R.string.dialog_cancel),
    isDestructive: Boolean = false,
    confirmTestTag: String = "confirm_dialog_confirm_button",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val semanticColors = VndbThemeTokens.colors
    val confirmContainer = if (isDestructive) semanticColors.danger else MaterialTheme.colorScheme.primary
    val confirmContent = if (isDestructive) semanticColors.onDanger else semanticColors.onAccentButton

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmContainer,
                    contentColor = confirmContent
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(confirmTestTag)
            ) {
                Text(text = confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_dialog_dismiss_button")
            ) {
                Text(text = dismissLabel)
            }
        }
    )
}

/**
 * Confirmation dialog shown before removing a single collection entry, explicitly informing
 * the user when the deletion will also be synced to their VNDB.org list on the next sync (F9).
 */
@Composable
fun RemoveSingleEntryConfirmDialog(
    vnTitle: String,
    isAccountSignedIn: Boolean,
    onConfirmRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = if (isAccountSignedIn) {
        stringResource(R.string.dialog_remove_single_message_synced, vnTitle)
    } else {
        stringResource(R.string.dialog_remove_single_message_local, vnTitle)
    }
    VndbConfirmDialog(
        title = stringResource(R.string.dialog_remove_single_title),
        message = message,
        confirmLabel = stringResource(R.string.collection_remove_button),
        isDestructive = true,
        confirmTestTag = "confirm_remove_single_entry_button",
        onConfirm = onConfirmRemove,
        onDismiss = onDismiss
    )
}

/**
 * Confirmation dialog shown before batch-deleting multiple collection entries (F9, Section 7).
 */
@Composable
fun RemoveBatchEntriesConfirmDialog(
    selectedCount: Int,
    isAccountSignedIn: Boolean,
    onConfirmRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = if (isAccountSignedIn) {
        pluralStringResource(R.plurals.dialog_remove_batch_message_synced, selectedCount, selectedCount)
    } else {
        pluralStringResource(R.plurals.dialog_remove_batch_message_local, selectedCount, selectedCount)
    }
    VndbConfirmDialog(
        title = stringResource(R.string.dialog_remove_batch_title),
        message = message,
        confirmLabel = stringResource(R.string.collection_batch_delete),
        isDestructive = true,
        confirmTestTag = "confirm_remove_batch_entries_button",
        onConfirm = onConfirmRemove,
        onDismiss = onDismiss
    )
}

/**
 * Sign-out confirmation dialog asking whether to keep or clear the local collection on device (Y6).
 */
@Composable
fun LogoutAccountConfirmDialog(
    onKeepLocalCollection: () -> Unit,
    onClearLocalCollection: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = stringResource(R.string.dialog_logout_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            LogoutDialogOptionBody(
                onKeepLocalCollection = onKeepLocalCollection,
                onClearLocalCollection = onClearLocalCollection
            )
        },
        confirmButton = {},
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("logout_cancel_button")
                ) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}

@Composable
private fun LogoutDialogOptionBody(
    onKeepLocalCollection: () -> Unit,
    onClearLocalCollection: () -> Unit
) {
    val semanticColors = VndbThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.dialog_logout_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(
            onClick = onKeepLocalCollection,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = semanticColors.onAccentButton
            ),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("logout_keep_local_button")
        ) {
            Text(
                text = stringResource(R.string.dialog_logout_keep_local),
                fontWeight = FontWeight.Bold
            )
        }
        OutlinedButton(
            onClick = onClearLocalCollection,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = semanticColors.danger
            ),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("logout_clear_local_button")
        ) {
            Text(
                text = stringResource(R.string.dialog_logout_clear_local),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(name = "RemoveSingleEntryConfirmDialog - Synced", showBackground = true)
@Composable
private fun RemoveSingleEntryConfirmDialogPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        RemoveSingleEntryConfirmDialog(
            vnTitle = "Ever17 -the out of infinity-",
            isAccountSignedIn = true,
            onConfirmRemove = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "LogoutAccountConfirmDialog", showBackground = true)
@Composable
private fun LogoutAccountConfirmDialogPreview() {
    VndbTheme(themeId = ThemeId.EV17) {
        LogoutAccountConfirmDialog(
            onKeepLocalCollection = {},
            onClearLocalCollection = {},
            onDismiss = {}
        )
    }
}
