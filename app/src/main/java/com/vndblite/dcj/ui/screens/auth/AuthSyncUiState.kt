package com.vndblite.dcj.ui.screens.auth

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.repository.LogoutCollectionOption
import com.vndblite.dcj.sync.SyncProgress

data class AuthSyncUiState(
    val tokenInput: String = "",
    val isTokenVisible: Boolean = false,
    val activeAccount: UserAccount? = null,
    val isVerifying: Boolean = false,
    val syncProgress: SyncProgress = SyncProgress(),
    val lastSyncReport: SyncReport? = null,
    val error: ApiError? = null,
    val showLogoutDialog: Boolean = false
)

sealed interface AuthSyncAction {
    data class UpdateTokenInput(val input: String) : AuthSyncAction
    data object ToggleTokenVisibility : AuthSyncAction
    data object VerifyAndSaveToken : AuthSyncAction
    data object SyncNow : AuthSyncAction
    data object RequestLogout : AuthSyncAction
    data object DismissLogoutDialog : AuthSyncAction
    data class ConfirmLogout(val option: LogoutCollectionOption) : AuthSyncAction
}
