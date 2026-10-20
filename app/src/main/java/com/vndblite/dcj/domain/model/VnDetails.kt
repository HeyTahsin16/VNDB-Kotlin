package com.vndblite.dcj.domain.model

/**
 * Extended metadata, screenshots, tags, releases summary, external links, and relations
 * for a visual novel's detail screen. Replaces `VnDataPhase02` and `VnDataPhase03` (A6, A7, A8, N2, N6).
 */
data class VnDetails(
    val id: String,
    val developers: List<Producer> = emptyList(),
    val devStatus: DevStatus? = null,
    val lengthMinutes: Int? = null,
    val aliases: List<String> = emptyList(),
    val minAges: List<Int> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val screenshots: List<VnImage> = emptyList(),
    val languages: List<String> = emptyList(),
    val machineTranslatedLanguages: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val externalLinks: List<String> = emptyList(),
    val relations: List<VnRelation> = emptyList(),
    val releasesTruncated: Boolean = false
)
