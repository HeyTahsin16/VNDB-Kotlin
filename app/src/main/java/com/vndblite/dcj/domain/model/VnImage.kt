package com.vndblite.dcj.domain.model

data class ImageDimensions(
    val width: Int,
    val height: Int
)

/**
 * Pure domain representation of a VNDB cover, character portrait, or screenshot (N6, A8).
 */
data class VnImage(
    val url: String,
    val thumbnailUrl: String? = null,
    val dims: ImageDimensions? = null,
    val sexual: Double = 0.0,
    val violence: Double = 0.0
) {
    val isExplicit: Boolean
        get() = sexual >= EXPLICIT_SEXUAL_THRESHOLD || violence >= EXPLICIT_VIOLENCE_THRESHOLD

    val previewUrl: String
        get() = thumbnailUrl?.takeIf { it.isNotBlank() } ?: url

    companion object {
        const val EXPLICIT_SEXUAL_THRESHOLD: Double = 0.25
        const val EXPLICIT_VIOLENCE_THRESHOLD: Double = 0.50
    }
}
