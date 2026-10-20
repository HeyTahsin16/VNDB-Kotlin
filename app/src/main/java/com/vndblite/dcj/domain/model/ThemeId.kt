package com.vndblite.dcj.domain.model

/**
 * Pure domain identifier for the 11 visual novel themes in VNDB Kotlin (A6, A8, D1, F7).
 * Colors, surface luminance, and background asset paths live in `ui/theme/`.
 */
enum class ThemeId(
    val code: String,
    val displayName: String
) {
    AGL("agl", "Angel Serenade"),
    AIR("air", "AIR"),
    EV17("ev17", "Ever17"),
    FATE("fate", "Fate/Stay Night"),
    CARNVL("carnvl", "Gekkou No Carnevale"),
    HGRSHI("hgrshi", "Higurashi No Naku Koro Ni"),
    TSUKHME("tsukhme", "Tsukihime"),
    TOHO("toho", "Touhou"),
    SEINRKN("seinrkn", "Seinarukana"),
    LILBUSTS("lilbusts", "Little Busters!"),
    SAYA("saya", "Saya No Uta");

    companion object {
        val DEFAULT: ThemeId = AGL

        fun fromCode(code: String?): ThemeId {
            if (code.isNullOrBlank()) return DEFAULT
            val trimmed = code.trim()
            return entries.firstOrNull {
                it.code.equals(trimmed, ignoreCase = true) ||
                    it.name.equals(trimmed, ignoreCase = true)
            } ?: DEFAULT
        }
    }
}
