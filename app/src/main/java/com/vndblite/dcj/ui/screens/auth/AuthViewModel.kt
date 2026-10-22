package com.vndblite.dcj.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.sync.SyncService
import com.vndblite.dcj.sync.SyncStage
import com.vndblite.dcj.ui.format.toStatusUiText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val syncService: SyncService
) : ViewModel() {

    val uiState: StateFlow<AuthUiState> = combine(
        syncService.activeAccountFlow,
        syncService.isAccountSyncedFlow,
        syncService.progressFlow
    ) { account, isSynced, progress ->
        val report = progress.lastReport
        val hasFailures = progress.stage == SyncStage.FAILED ||
            progress.lastError != null ||
            (report != null && !report.isCompleteSuccess)
        AuthUiState(
            activeAccount = account,
            isAccountSynced = isSynced,
            progress = progress,
            statusText = progress.toStatusUiText(),
            isErrorStatus = hasFailures,
            canRetrySync = !progress.isRunning && account != null && hasFailures
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState()
    )

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.AuthenticateAndSync -> {
                viewModelScope.launch {
                    syncService.authenticateAndSync(action.token)
                }
            }
            AuthAction.SyncNow -> {
                viewModelScope.launch {
                    syncService.syncCurrentAccount()
                }
            }
            is AuthAction.Logout -> {
                viewModelScope.launch {
                    syncService.logout(clearLocalCollection = action.clearLocalCollection)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(syncService = container.syncService)
            }
        }
    }
}
