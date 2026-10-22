package com.vndblite.dcj.ui.components

import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ThemeId

/**
 * Single source of truth for all bundled asset paths under `app/src/main/assets/` (D8, Item 1, Item 5a).
 */
object AssetPaths {
    const val APP_ICON: String = "images/icon/app_icon.png"
    const val GITHUB_ICON: String = "images/contact/github.png"
    const val FLAG_FALLBACK: String = "images/flags/xx.png"
    const val PLATFORM_FALLBACK: String = "images/os/oth.png"

    val TUTORIAL_IMAGES: List<String> = listOf(
        "images/tutorial/tutorial1.png",
        "images/tutorial/tutorial2.png",
        "images/tutorial/tutorial3.png"
    )

    fun themeBackgroundPath(themeId: ThemeId): String = when (themeId) {
        ThemeId.AGL -> "images/bg/bgAngel.png"
        ThemeId.AIR -> "images/bg/bgAIR.png"
        ThemeId.EV17 -> "images/bg/bgEver17.png"
        ThemeId.FATE -> "images/bg/bgFate.png"
        ThemeId.CARNVL -> "images/bg/bgCarnevale.png"
        ThemeId.HGRSHI -> "images/bg/bgHigurashi.png"
        ThemeId.TSUKHME -> "images/bg/bgTsukihime.png"
        ThemeId.TOHO -> "images/bg/bgTouhou.png"
        ThemeId.SEINRKN -> "images/bg/bgSeinaru.png"
        ThemeId.LILBUSTS -> "images/bg/bgLittleBusters.png"
        ThemeId.SAYA -> "images/bg/bgSaya.png"
    }

    fun languageFlagPath(language: Language?): String =
        if (language != null) "images/flags/${language.code}.png" else FLAG_FALLBACK

    fun languageFlagPath(code: String?): String =
        languageFlagPath(Language.fromCode(code))

    fun platformIconPath(platform: Platform?): String =
        if (platform != null && platform.hasDedicatedAsset) {
            "images/os/${platform.code}.png"
        } else {
            PLATFORM_FALLBACK
        }

    fun platformIconPath(code: String?): String =
        platformIconPath(Platform.fromCode(code))

    fun collectionStatusIconPath(status: CollectionStatus): String =
        "images/status/${status.storageKey}.png"

    fun genreArtPath(preset: GenrePreset): String = when (preset) {
        GenrePreset.ACTION -> "images/search/action.png"
        GenrePreset.COMEDY -> "images/search/comedy.png"
        GenrePreset.DRAMA -> "images/search/drama.png"
        GenrePreset.FANTASY -> "images/search/fantasy.png"
        GenrePreset.HORROR -> "images/search/horror.png"
        GenrePreset.MYSTERY -> "images/search/mystery.png"
        GenrePreset.ROMANCE -> "images/search/romance.png"
        GenrePreset.SCIFI -> "images/search/scifi.png"
    }

    fun allReferencedAssetPaths(): Set<String> = buildSet {
        add(APP_ICON)
        add(GITHUB_ICON)
        add(FLAG_FALLBACK)
        add(PLATFORM_FALLBACK)
        addAll(TUTORIAL_IMAGES)
        ThemeId.entries.forEach { add(themeBackgroundPath(it)) }
        Language.entries.forEach { add(languageFlagPath(it)) }
        Platform.entries.forEach { add(platformIconPath(it)) }
        CollectionStatus.entries.forEach { add(collectionStatusIconPath(it)) }
        GenrePreset.entries.forEach { add(genreArtPath(it)) }
    }
}
