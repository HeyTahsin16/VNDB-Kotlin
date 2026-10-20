package com.vndblite.dcj.domain.model

import com.vndblite.dcj.core.ApiError

data class SyncFailure(
    val vnId: String,
    val operation: String,
    val error: ApiError
)

/**
 * Structured report returned by userlist synchronization (`SyncEngine`),
 * replacing raw boolean returns and silent failures (Y4, A10).
 */
data class SyncReport(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val deletedLocal: Int = 0,
    val deletedRemote: Int = 0,
    val failed: List<SyncFailure> = emptyList()
) {
    val isCompleteSuccess: Boolean
        get() = failed.isEmpty()

    val hasPartialFailures: Boolean
        get() = failed.isNotEmpty() && (pushed + pulled + deletedLocal + deletedRemote) > 0
}
