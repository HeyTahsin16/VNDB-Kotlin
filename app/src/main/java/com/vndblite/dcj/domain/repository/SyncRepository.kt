package com.vndblite.dcj.domain.repository

import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.sync.SyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    val progressFlow: StateFlow<SyncProgress>
    val activeAccountFlow: Flow<UserAccount?>
    val isAccountSyncedFlow: Flow<Boolean>

    suspend fun authenticateAndSync(
        rawToken: String,
        onProgress: (SyncProgress) -> Unit = {}
    ): AppResult<Pair<UserAccount, SyncReport>>

    suspend fun syncCurrentAccount(
        onProgress: (SyncProgress) -> Unit = {}
    ): AppResult<SyncReport>

    suspend fun hasStoredToken(): Boolean

    suspend fun logout(clearLocalCollection: Boolean)
}
