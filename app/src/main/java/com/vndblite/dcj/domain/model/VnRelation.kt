package com.vndblite.dcj.domain.model

enum class VnRelationType(val apiCode: String) {
    SEQUEL("seq"),
    PREQUEL("preq"),
    SAME_SETTING("set"),
    ALTERNATIVE_VERSION("alt"),
    SHARES_CHARACTERS("char"),
    SIDE_STORY("side"),
    PARENT_STORY("par"),
    SAME_SERIES("ser"),
    FANDISC("fan"),
    ORIGINAL_GAME("orig"),
    OTHER("oth");

    companion object {
        fun fromApiCode(code: String?): VnRelationType {
            if (code.isNullOrBlank()) return OTHER
            return entries.firstOrNull { it.apiCode.equals(code.trim(), ignoreCase = true) } ?: OTHER
        }
    }
}

data class VnRelation(
    val id: String,
    val relationType: VnRelationType,
    val rawRelationCode: String,
    val isOfficial: Boolean = false,
    val relatedVn: VnSummary? = null
)
