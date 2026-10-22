package com.vndblite.dcj.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.format.openExternalUrl
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(LocalAppContainer.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AuthContent(
        state = state,
        onBack = onBack,
        onAction = viewModel::onAction,
        onOpenVndbInBrowser = { openExternalUrl(context, AppInfo.VNDB_URL) }
    )
}

@Composable
fun AuthContent(
    state: AuthUiState,
    onBack: () -> Unit,
    onAction: (AuthAction) -> Unit,
    onOpenVndbInBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var showToken by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

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
                text = stringResource(R.string.auth_screen_title),
                style = MaterialTheme.typography.displayMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.activeAccount != null) {
            ConnectedAccountCard(
                account = state.activeAccount,
                isSynced = state.isAccountSynced,
                isSyncing = state.progress.isRunning,
                onSyncNow = { onAction(AuthAction.SyncNow) },
                onRequestLogout = { showLogoutDialog = true }
            )
        } else {
            TokenSignInCard(
                tokenInput = tokenInput,
                showToken = showToken,
                isSyncing = state.progress.isRunning,
                onTokenChange = { tokenInput = it },
                onToggleShowToken = { showToken = !showToken },
                onSubmit = { onAction(AuthAction.AuthenticateAndSync(tokenInput)) },
                onOpenVndbInBrowser = onOpenVndbInBrowser
            )
        }

        state.statusText?.let { statusUiText ->
            SyncStatusBanner(
                message = statusUiText.asString(),
                isRunning = state.progress.isRunning,
                isError = state.isErrorStatus,
                canRetry = state.canRetrySync,
                onRetry = { onAction(AuthAction.SyncNow) }
            )
        }
    }

    if (showLogoutDialog) {
        AccountLogoutDialog(
            onKeepLocal = {
                showLogoutDialog = false
                tokenInput = ""
                onAction(AuthAction.Logout(clearLocalCollection = false))
            },
            onClearLocal = {
                showLogoutDialog = false
                tokenInput = ""
                onAction(AuthAction.Logout(clearLocalCollection = true))
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        AuthContent(
            state = AuthUiState(),
            onBack = {},
            onAction = {},
            onOpenVndbInBrowser = {}
        )
    }
}
