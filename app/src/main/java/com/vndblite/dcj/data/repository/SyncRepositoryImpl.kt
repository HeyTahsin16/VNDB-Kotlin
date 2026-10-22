package com.vndblite.dcj.data.repository

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.data.local.secure.TokenStore
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.repository.SyncRepository
import com.vndblite.dcj.sync.SyncProgress
import com.vndblite.dcj.sync.SyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class SyncRepositoryImpl(
    private val syncService: SyncService,
    private val settingsStore: SettingsStore,
    private val tokenStore: TokenStore
) : SyncRepository {

    override val progressFlow: StateFlow<SyncProgress>
        get() = syncService.progressFlow

    override val activeAccountFlow: Flow<UserAccount?>
        get() = syncService.activeAccountFlow

    override val isAccountSyncedFlow: Flow<Boolean>
        get() = syncService.isAccountSyncedFlow

    override suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit
    ): AppResult<Pair<UserAccount, SyncReport>> =
        syncService.authenticateAndSync(rawToken = rawToken, onProgress = onProgress)

    override suspend fun syncCurrentAccount(
        onProgress: (SyncProgress) -> Unit
    ): AppResult<SyncReport> =
        syncService.syncCurrentAccount(onProgress = onProgress)

    override suspend fun hasStoredToken(): Boolean {
        val activeAccountId = settingsStore.getActiveAccountId()?.trim()?.takeIf { it.isNotEmpty() }
            ?: return false
        return tokenStore.hasToken(activeAccountId)
    }

    override suspend fun logout(clearLocalCollection: Boolean) {
        syncService.logout(clearLocalCollection = clearLocalCollection)
    }
}
