package com.vndblite.dcj.domain.model

/**
 * Logical operator for combining VNDB search filter clauses (A7).
 */
enum class AndOr(val apiToken: String) {
    AND("and"),
    OR("or");

    fun toggle(): AndOr = if (this == AND) OR else AND
    fun toggled(): AndOr = toggle()

    companion object {
        fun fromToken(token: String?): AndOr =
            if (token.equals("or", ignoreCase = true)) OR else AND
    }
}

/**
 * Release window filter for visual novel queries (A7, N8).
 */
enum class ReleaseWindow(val storageKey: String) {
    ALL(""),
    LATEST("latest"),
    UPCOMING("upcoming");

    companion object {
        val ANY: ReleaseWindow
            get() = ALL

        fun fromStorageKey(key: String?): ReleaseWindow =
            entries.firstOrNull { it.storageKey.equals(key?.trim().orEmpty(), ignoreCase = true) } ?: ALL
    }
}

/**
 * Pure domain filter state for remote VN search (A6, A7, A8).
 */
data class VnSearchFilters(
    val searchQuery: String = "",
    val andOr: AndOr = AndOr.AND,
    val languages: List<Language> = emptyList(),
    val devStatuses: List<DevStatus> = emptyList(),
    val originalLanguages: List<Language> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val developers: List<Producer> = emptyList(),
    val characterTraits: List<Trait> = emptyList(),
    val traitAndOr: AndOr = AndOr.AND,
    val characterRoles: List<CharacterRole> = emptyList(),
    val characterRoleAndOr: AndOr = AndOr.OR,
    val characterSexes: List<CharacterSex> = emptyList(),
    val characterSexAndOr: AndOr = AndOr.OR,
    val releaseWindow: ReleaseWindow = ReleaseWindow.ALL
) {
    val hasActiveFilters: Boolean
        get() = languages.isNotEmpty() ||
            devStatuses.isNotEmpty() ||
            originalLanguages.isNotEmpty() ||
            platforms.isNotEmpty() ||
            tags.isNotEmpty() ||
            developers.isNotEmpty() ||
            characterTraits.isNotEmpty() ||
            characterRoles.isNotEmpty() ||
            characterSexes.isNotEmpty() ||
            releaseWindow != ReleaseWindow.ALL
}

/**
 * Pure domain filter state for the Character Trait Explorer (A6, A7, A8).
 */
data class CharacterSearchFilters(
    val searchQuery: String = "",
    val traits: List<Trait> = emptyList(),
    val traitAndOr: AndOr = AndOr.AND,
    val roles: List<CharacterRole> = emptyList(),
    val roleAndOr: AndOr = AndOr.OR,
    val sexes: List<CharacterSex> = emptyList(),
    val sexAndOr: AndOr = AndOr.OR
) {
    val hasActiveFilters: Boolean
        get() = traits.isNotEmpty() || roles.isNotEmpty() || sexes.isNotEmpty()
}

/**
 * Pure domain filter state for filtering the local user collection (A6, A7, A8).
 */
data class CollectionFilters(
    val searchQuery: String = "",
    val status: CollectionStatus? = null,
    val minAge: Int? = null,
    val languages: List<Language> = emptyList(),
    val devStatuses: List<DevStatus> = emptyList(),
    val originalLanguages: List<Language> = emptyList(),
    val platforms: List<Platform> = emptyList()
) {
    val hasActiveMetadataFilters: Boolean
        get() = (minAge != null && minAge > 0) ||
            languages.isNotEmpty() ||
            devStatuses.isNotEmpty() ||
            originalLanguages.isNotEmpty() ||
            platforms.isNotEmpty()
}
