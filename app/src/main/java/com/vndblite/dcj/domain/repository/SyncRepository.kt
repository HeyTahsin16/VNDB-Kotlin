package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.map
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.sync.SyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class LogoutCollectionOption {
    KEEP_LOCAL_COLLECTION,
    CLEAR_LOCAL_COLLECTION
}

interface SyncRepository {
    val progressFlow: StateFlow<SyncProgress>
    val activeAccountFlow: Flow<UserAccount?>
    val isAccountSyncedFlow: Flow<Boolean>

    val syncProgress: StateFlow<SyncProgress>
        get() = progressFlow

    fun observeActiveAccount(): Flow<UserAccount?> = activeAccountFlow

    suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit = {}
    ): AppResult<Pair<UserAccount, SyncReport>>

    suspend fun authenticateAndSaveToken(rawToken: String): AppResult<UserAccount> =
        authenticateAndSync(rawToken).map { it.first }

    suspend fun syncCurrentAccount(
        onProgress: (SyncProgress) -> Unit = {}
    ): AppResult<SyncReport>

    suspend fun syncCollection(): AppResult<SyncReport> = syncCurrentAccount()

    suspend fun hasStoredToken(): Boolean

    suspend fun logout(clearLocalCollection: Boolean)

    suspend fun logout(option: LogoutCollectionOption) =
        logout(clearLocalCollection = option == LogoutCollectionOption.CLEAR_LOCAL_COLLECTION)
}
