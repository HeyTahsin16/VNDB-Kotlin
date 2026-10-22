package com.vndblite.dcj.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe `@Serializable` routes for Jetpack Navigation Compose (A4, Item 5a).
 */
@Serializable
data object HomeRoute

@Serializable
data class SearchRoute(
    val initialQuery: String = "",
    val initialTagId: String? = null,
    val initialTagName: String? = null,
    val initialSortFieldCode: String? = null,
    val initialReverseSort: Boolean = true,
    val initialDevStatusCode: Int? = null,
    val initialReleaseWindowKey: String = ""
)

@Serializable
data class CharacterSearchRoute(
    val initialQuery: String = "",
    val initialTraitId: String? = null,
    val initialTraitName: String? = null,
    val initialTraitGroupName: String? = null
)

@Serializable
data object CollectionRoute

@Serializable
data class VnDetailRoute(
    val vnId: String
)

@Serializable
data object OthersRoute

@Serializable
data object SettingsRoute

@Serializable
data object AuthSyncRoute

@Serializable
data object RandomVnRoute

@Serializable
data object AboutRoute
