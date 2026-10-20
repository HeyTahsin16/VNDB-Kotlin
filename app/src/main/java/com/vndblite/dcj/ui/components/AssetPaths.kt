package com.vndblite.dcj.ui.components

import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ThemeId

/**
 * Single typed registry for bundled assets in `app/src/main/assets/` loaded via Coil
 * using `file:///android_asset/...` URIs (U4, R8).
 */
object AssetPaths {
    const val ASSET_URI_PREFIX: String = "file:///android_asset/"

    const val APP_ICON: String = "${ASSET_URI_PREFIX}images/icon/app_icon.png"
    const val GITHUB_ICON: String = "${ASSET_URI_PREFIX}images/contact/github.png"
    const val UNKNOWN_FLAG: String = "${ASSET_URI_PREFIX}images/flags/xx.png"
    const val OTHER_PLATFORM_ICON: String = "${ASSET_URI_PREFIX}images/os/oth.png"

    fun themeBackground(themeId: ThemeId): String {
        val fileName = when (themeId) {
            ThemeId.AGL -> "bgAngel.png"
            ThemeId.AIR -> "bgAIR.png"
            ThemeId.EV17 -> "bgEver17.png"
            ThemeId.FATE -> "bgFate.png"
            ThemeId.CARNVL -> "bgCarnevale.png"
            ThemeId.HGRSHI -> "bgHigurashi.png"
            ThemeId.TSUKHME -> "bgTsukihime.png"
            ThemeId.TOHO -> "bgTouhou.png"
            ThemeId.SEINRKN -> "bgSeinaru.png"
            ThemeId.LILBUSTS -> "bgLittleBusters.png"
            ThemeId.SAYA -> "bgSaya.png"
        }
        return "${ASSET_URI_PREFIX}images/bg/$fileName"
    }

    fun statusIcon(status: CollectionStatus): String =
        "${ASSET_URI_PREFIX}images/status/${status.storageKey}.png"

    fun languageFlag(language: Language?): String =
        languageFlagByCode(language?.code)

    fun languageFlagByCode(rawCode: String?): String {
        val normalized = rawCode?.trim().orEmpty()
        val matched = Language.fromCode(normalized)
        return if (matched != null) {
            "${ASSET_URI_PREFIX}images/flags/${matched.code}.png"
        } else {
            UNKNOWN_FLAG
        }
    }

    fun platformIcon(platform: Platform?): String =
        platformIconByCode(platform?.code)

    fun platformIconByCode(rawCode: String?): String {
        val normalized = rawCode?.trim()?.lowercase().orEmpty()
        return if (normalized in BUNDLED_PLATFORM_ICON_CODES) {
            "${ASSET_URI_PREFIX}images/os/$normalized.png"
        } else {
            OTHER_PLATFORM_ICON
        }
    }

    fun genrePresetImage(genreKey: String): String =
        "${ASSET_URI_PREFIX}images/search/${genreKey.trim().lowercase()}.png"

    fun tutorialImage(stepNumber: Int): String {
        val step = stepNumber.coerceIn(1, 3)
        return "${ASSET_URI_PREFIX}images/tutorial/tutorial$step.png"
    }

    /**
     * Converts either a typed `file:///android_asset/...` URI or a relative asset path
     * into a canonical Coil `file:///android_asset/...` URI (U4).
     */
    fun toCoilAssetUri(pathOrUri: String): String {
        val trimmed = pathOrUri.trim()
        if (trimmed.startsWith(ASSET_URI_PREFIX)) return trimmed
        val relative = trimmed.removePrefix("assets/").removePrefix("/")
        return "$ASSET_URI_PREFIX$relative"
    }

    private val BUNDLED_PLATFORM_ICON_CODES: Set<String> = setOf(
        "and", "bdp", "drc", "dvd", "fm7", "fm8", "fmt", "ios",
        "lin", "mac", "n3d", "nds", "nes", "oth", "p88", "p98",
        "ps1", "ps2", "ps3", "ps4", "ps5", "psp", "psv", "swi",
        "vnd", "web", "wii", "win", "xb1", "xb3", "xbo", "xxs"
    )
}
