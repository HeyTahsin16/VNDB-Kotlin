package com.vndblite.dcj.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.repository.LogoutCollectionOption
import com.vndblite.dcj.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthSyncViewModel(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthSyncUiState())
    val uiState: StateFlow<AuthSyncUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            syncRepository.observeActiveAccount().collect { account ->
                _uiState.update { it.copy(activeAccount = account) }
            }
        }
        viewModelScope.launch {
            syncRepository.syncProgress.collect { progress ->
                _uiState.update { it.copy(syncProgress = progress) }
            }
        }
    }

    fun onAction(action: AuthSyncAction) {
        when (action) {
            is AuthSyncAction.UpdateTokenInput -> {
                // Never persist raw token input into SavedStateHandle bundle
                _uiState.update { it.copy(tokenInput = action.input, error = null) }
            }
            AuthSyncAction.ToggleTokenVisibility -> {
                _uiState.update { it.copy(isTokenVisible = !it.isTokenVisible) }
            }
            AuthSyncAction.VerifyAndSaveToken -> verifyAndSaveToken()
            AuthSyncAction.SyncNow -> syncNow()
            AuthSyncAction.RequestLogout -> {
                _uiState.update { it.copy(showLogoutDialog = true) }
            }
            AuthSyncAction.DismissLogoutDialog -> {
                _uiState.update { it.copy(showLogoutDialog = false) }
            }
            is AuthSyncAction.ConfirmLogout -> {
                _uiState.update { it.copy(showLogoutDialog = false) }
                viewModelScope.launch {
                    syncRepository.logout(action.option)
                    _uiState.update {
                        it.copy(
                            tokenInput = "",
                            lastSyncReport = null,
                            error = null
                        )
                    }
                }
            }
        }
    }

    private fun verifyAndSaveToken() {
        val rawToken = _uiState.value.tokenInput.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, error = null, lastSyncReport = null) }
            when (val res = syncRepository.authenticateAndSaveToken(rawToken)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            tokenInput = "",
                            isVerifying = false,
                            activeAccount = res.data,
                            error = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            error = res.error
                        )
                    }
                }
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, lastSyncReport = null) }
            when (val res = syncRepository.syncCollection()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(lastSyncReport = res.data, error = null) }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(error = res.error) }
                }
            }
        }
    }

    companion object {
        fun factory(syncRepository: SyncRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthSyncViewModel(syncRepository) as T
                }
            }
    }
}
