package com.vndblite.dcj.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.ConfirmDialog
import com.vndblite.dcj.ui.components.ConfirmDialogSpec
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(LocalAppContainer.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onBack = onBack,
        onAction = viewModel::onAction
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    var showClearCacheConfirm by rememberSaveable { mutableStateOf(false) }
    var showClearCollectionConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back_cd),
                    tint = spec.accentColor
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        state.feedbackMessage?.let { msg ->
            SettingsFeedbackBanner(
                message = msg.asString(),
                onDismiss = { onAction(SettingsAction.DismissFeedback) }
            )
        }

        SettingsThemePickerCard(
            selectedTheme = state.settings.themeId,
            onSelectTheme = { onAction(SettingsAction.SelectTheme(it)) }
        )

        SettingsDisplayAndLayoutCard(
            settings = state.settings,
            onAction = onAction
        )

        SettingsTabOrderCard(
            orderedStatuses = state.settings.collectionStatusTabArrangement,
            onMoveStatus = { status, dir -> onAction(SettingsAction.MoveStatusTab(status, dir)) }
        )

        SettingsMaintenanceCard(
            onRequestClearCache = { showClearCacheConfirm = true },
            onRequestResetSettings = { showResetConfirm = true },
            onRequestClearCollection = { showClearCollectionConfirm = true }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showClearCacheConfirm) {
        ConfirmDialog(
            spec = ConfirmDialogSpec(
                title = UiText.StringResource(R.string.dialog_clear_cache_title),
                message = UiText.StringResource(R.string.dialog_clear_cache_message),
                confirmLabel = UiText.StringResource(R.string.dialog_confirm)
            ),
            onConfirm = {
                showClearCacheConfirm = false
                onAction(SettingsAction.ClearTransientCache)
            },
            onDismiss = { showClearCacheConfirm = false }
        )
    }

    if (showResetConfirm) {
        ConfirmDialog(
            spec = ConfirmDialogSpec(
                title = UiText.StringResource(R.string.dialog_reset_settings_title),
                message = UiText.StringResource(R.string.dialog_reset_settings_message),
                confirmLabel = UiText.StringResource(R.string.action_reset),
                isDestructive = true
            ),
            onConfirm = {
                showResetConfirm = false
                onAction(SettingsAction.ResetSettings)
            },
            onDismiss = { showResetConfirm = false }
        )
    }

    if (showClearCollectionConfirm) {
        ConfirmDialog(
            spec = ConfirmDialogSpec(
                title = UiText.StringResource(R.string.settings_clear_collection_dialog_title),
                message = UiText.StringResource(R.string.settings_clear_collection_dialog_body),
                confirmLabel = UiText.StringResource(R.string.settings_clear_all_confirm),
                isDestructive = true
            ),
            onConfirm = {
                showClearCollectionConfirm = false
                onAction(SettingsAction.ClearLocalCollection)
            },
            onDismiss = { showClearCollectionConfirm = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        SettingsContent(
            state = SettingsUiState(),
            onBack = {},
            onAction = {}
        )
    }
}
