package com.vndblite.dcj.ui.screens.random

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.components.VnGridCardDisplayState
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
fun RandomVnDialog(
    state: RandomVnUiState,
    onAction: (RandomVnAction) -> Unit,
    onOpenDetail: (VnSummary) -> Unit
) {
    if (!state.isVisible) return
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors

    AlertDialog(
        onDismissRequest = { onAction(RandomVnAction.Dismiss) },
        containerColor = spec.surfaceColor,
        titleContentColor = spec.onSurfaceColor,
        textContentColor = spec.onSurfaceColor,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    tint = spec.accentColor
                )
                Text(
                    text = stringResource(R.string.random_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            RandomVnDialogBody(
                state = state,
                onAction = onAction,
                onOpenDetail = onOpenDetail
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAction(RandomVnAction.RollAgain) },
                    enabled = !state.isLoading,
                    modifier = Modifier.testTag("random_vn_roll_again")
                ) {
                    Text(
                        text = stringResource(R.string.random_dialog_roll_again),
                        color = spec.accentColor
                    )
                }
                if (state.vn != null && !state.isLoading) {
                    Button(
                        onClick = { onOpenDetail(state.vn) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.accentColor,
                            contentColor = semantic.onAccentButton
                        ),
                        modifier = Modifier.testTag("random_vn_view_detail")
                    ) {
                        Text(
                            text = stringResource(R.string.random_dialog_view_details),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(RandomVnAction.Dismiss) }) {
                Text(
                    text = stringResource(R.string.action_close),
                    color = spec.onSurfaceColor.copy(alpha = 0.8f)
                )
            }
        }
    )
}

@Composable
private fun RandomVnDialogBody(
    state: RandomVnUiState,
    onAction: (RandomVnAction) -> Unit,
    onOpenDetail: (VnSummary) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = spec.accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.random_dialog_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.onSurfaceColor.copy(alpha = 0.85f)
                    )
                }
            }
            state.error != null -> {
                Text(
                    text = state.error.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = semantic.danger,
                    textAlign = TextAlign.Center
                )
            }
            state.vn != null -> {
                val vn = state.vn
                Box(modifier = Modifier.width(210.dp)) {
                    VnGridCard(
                        vn = vn,
                        onClick = { onOpenDetail(vn) },
                        displayState = VnGridCardDisplayState(
                            subtitleSortField = SortField.RATING,
                            collectionEntry = state.collectionEntries[vn.id],
                            blurExplicitCover = state.showCoverCensor,
                            isNsfwRevealed = vn.id in state.nsfwRevealedIds
                        ),
                        onToggleNsfwReveal = {
                            onAction(RandomVnAction.ToggleNsfwReveal(vn.id))
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RandomVnDialogPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        RandomVnDialog(
            state = RandomVnUiState(
                isVisible = true,
                vn = PreviewData.sampleVnSummary
            ),
            onAction = {},
            onOpenDetail = {}
        )
    }
}
