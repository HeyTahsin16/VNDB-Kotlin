package com.vndblite.dcj.domain.model

/**
 * Represents a verified VNDB user account returned by `/kana/authinfo`.
 * The personal access token itself is kept inside `SecureTokenStorage` and never exposed in UI state.
 */
data class UserAccount(
    val id: String,
    val username: String,
    val permissions: Set<String> = emptySet()
) {
    val canReadList: Boolean
        get() = "listread" in permissions

    val canWriteList: Boolean
        get() = "listwrite" in permissions

    val hasFullListPermissions: Boolean
        get() = canReadList && canWriteList

    val missingListPermissions: Set<String>
        get() = buildSet {
            if (!canReadList) add("listread")
            if (!canWriteList) add("listwrite")
        }
}
