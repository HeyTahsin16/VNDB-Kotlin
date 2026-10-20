package com.vndblite.dcj.domain.model

data class Quote(
    val id: String,
    val text: String,
    val score: Int = 0,
    val vn: VnSummary? = null,
    val characterId: String? = null,
    val characterName: String? = null,
    val characterOriginalName: String? = null
)
