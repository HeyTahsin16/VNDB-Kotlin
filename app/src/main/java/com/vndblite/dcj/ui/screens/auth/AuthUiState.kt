package com.vndblite.dcj.ui.screens.auth

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.sync.SyncProgress

@Immutable
data class AuthUiState(
    val activeAccount: UserAccount? = null,
    val isAccountSynced: Boolean = false,
    val progress: SyncProgress = SyncProgress(),
    val statusText: UiText? = null,
    val isErrorStatus: Boolean = false,
    val canRetrySync: Boolean = false
)

sealed interface AuthAction {
    data class AuthenticateAndSync(val token: String) : AuthAction
    data object SyncNow : AuthAction
    data class Logout(val clearLocalCollection: Boolean) : AuthAction
}
