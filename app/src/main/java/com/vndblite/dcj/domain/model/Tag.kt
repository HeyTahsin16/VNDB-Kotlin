package com.vndblite.dcj.domain.model

data class Tag(
    val id: String,
    val name: String,
    val rating: Double = 0.0,
    val spoilerLevel: Int = 0
)

/**
 * Predefined genre tag shortcuts shown on the Search screen (A6, A8).
 */
enum class GenrePreset(val tagId: String, val defaultName: String) {
    ACTION("g12", "Action"),
    COMEDY("g104", "Comedy"),
    DRAMA("g147", "Drama"),
    FANTASY("g2", "Fantasy"),
    HORROR("g7", "Horror"),
    MYSTERY("g19", "Mystery"),
    ROMANCE("g96", "Romance"),
    SCIFI("g105", "Sci-fi");

    fun toTag(): Tag = Tag(id = tagId, name = defaultName)
}
