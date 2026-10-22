package com.vndblite.dcj.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.VndbTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.action_confirm),
    dismissLabel: String = stringResource(R.string.action_cancel),
    isDestructive: Boolean = false,
    confirmTestTag: String = "confirm_dialog_confirm_button",
    dismissTestTag: String = "confirm_dialog_dismiss_button",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val confirmButtonColor = if (isDestructive) ColorTokens.DestructiveRed else themeSpec.accentColor
    val confirmTextColor = if (isDestructive) ColorTokens.PureWhite else themeSpec.onAccentColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeSpec.surfaceColor,
        titleContentColor = themeSpec.contentColor,
        textContentColor = themeSpec.contentColor,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = themeSpec.accentColor
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor.copy(alpha = 0.9f)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmButtonColor,
                    contentColor = confirmTextColor
                ),
                modifier = Modifier.testTag(confirmTestTag)
            ) {
                Text(text = confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = themeSpec.contentColor),
                modifier = Modifier.testTag(dismissTestTag)
            ) {
                Text(text = dismissLabel)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        ConfirmDialog(
            title = "Remove from Collection?",
            message = "Remove \"Steins;Gate\" from your collection?",
            isDestructive = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
