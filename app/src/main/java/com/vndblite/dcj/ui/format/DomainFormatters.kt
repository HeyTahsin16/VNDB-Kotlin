package com.vndblite.dcj.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnRelationType
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.theme.ThemeSpecs
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import java.util.Locale

/**
 * Presentation mappings and localized display formatters for pure domain models (Section 5, A8, A9, A11).
 * Keeps `Color`, asset paths, and English display strings out of `domain/model/`.
 */

@StringRes
fun CollectionStatus.labelResId(): Int = when (this) {
    CollectionStatus.PLAYING -> R.string.status_playing
    CollectionStatus.FINISHED -> R.string.status_finished
    CollectionStatus.STALLED -> R.string.status_stalled
    CollectionStatus.DROPPED -> R.string.status_dropped
    CollectionStatus.WISHLIST -> R.string.status_wishlist
    CollectionStatus.BLACKLIST -> R.string.status_blacklist
}

@Composable
fun CollectionStatus.displayLabel(): String = stringResource(labelResId())

fun CollectionStatus.iconAssetUri(): String = AssetPaths.statusIcon(this)

@Composable
fun CollectionStatus.badgeColor(): Color = VndbThemeTokens.colors.statusColor(this)

@StringRes
fun DevStatus.labelResId(): Int = when (this) {
    DevStatus.FINISHED -> R.string.dev_status_finished
    DevStatus.IN_DEVELOPMENT -> R.string.dev_status_in_development
    DevStatus.CANCELLED -> R.string.dev_status_cancelled
}

@Composable
fun DevStatus?.displayLabel(): String =
    if (this != null) stringResource(labelResId()) else stringResource(R.string.dev_status_unknown)

@Composable
fun DevStatus.badgeColor(): Color = VndbThemeTokens.colors.devStatusColor(this)

@StringRes
fun CharacterRole.labelResId(): Int = when (this) {
    CharacterRole.MAIN -> R.string.character_role_main
    CharacterRole.PRIMARY -> R.string.character_role_primary
    CharacterRole.SIDE -> R.string.character_role_side
    CharacterRole.APPEARS -> R.string.character_role_appears
}

@Composable
fun CharacterRole.displayLabel(): String = stringResource(labelResId())

@StringRes
fun CharacterSex?.labelResId(): Int = when (this) {
    CharacterSex.MALE -> R.string.character_sex_male
    CharacterSex.FEMALE -> R.string.character_sex_female
    CharacterSex.BOTH -> R.string.character_sex_both
    CharacterSex.SEXLESS -> R.string.character_sex_sexless
    null -> R.string.character_sex_unknown
}

@Composable
fun CharacterSex?.displayLabel(): String = stringResource(labelResId())

@StringRes
fun VnLengthBucket?.labelResId(): Int = when (this) {
    VnLengthBucket.VERY_SHORT -> R.string.length_very_short
    VnLengthBucket.SHORT -> R.string.length_short
    VnLengthBucket.MEDIUM -> R.string.length_medium
    VnLengthBucket.LONG -> R.string.length_long
    VnLengthBucket.VERY_LONG -> R.string.length_very_long
    null -> R.string.length_unknown
}

@Composable
fun VnLengthBucket?.displayLabel(): String = stringResource(labelResId())

fun formatVnLengthUiText(
    lengthMinutes: Int?,
    lengthBucket: VnLengthBucket?
): UiText {
    val validMinutes = lengthMinutes?.takeIf { it > 0 }
    if (validMinutes != null) {
        val hours = validMinutes / 60
        val mins = validMinutes % 60
        val durationText = when {
            hours > 0 && mins > 0 -> UiText.StringResource(R.string.length_hours_minutes, hours, mins)
            hours > 0 -> UiText.StringResource(R.string.length_hours_only, hours)
            else -> UiText.StringResource(R.string.length_minutes_only, mins)
        }
        return if (lengthBucket != null) {
            UiText.StringResource(
                R.string.length_with_bucket,
                durationText,
                UiText.StringResource(lengthBucket.labelResId())
            )
        } else {
            durationText
        }
    }
    return UiText.StringResource(lengthBucket.labelResId())
}

@StringRes
fun VnRelationType.labelResId(): Int = when (this) {
    VnRelationType.SEQUEL -> R.string.relation_seq
    VnRelationType.PREQUEL -> R.string.relation_preq
    VnRelationType.SAME_SETTING -> R.string.relation_set
    VnRelationType.ALTERNATIVE_VERSION -> R.string.relation_alt
    VnRelationType.SHARES_CHARACTERS -> R.string.relation_char
    VnRelationType.SIDE_STORY -> R.string.relation_side
    VnRelationType.PARENT_STORY -> R.string.relation_par
    VnRelationType.SAME_SERIES -> R.string.relation_ser
    VnRelationType.FANDISC -> R.string.relation_fan
    VnRelationType.ORIGINAL_GAME -> R.string.relation_orig
    VnRelationType.OTHER -> R.string.relation_other
}

@Composable
fun VnRelationType.displayLabel(): String = stringResource(labelResId())

fun ReleaseDate?.toUiText(): UiText = when (this) {
    null -> UiText.StringResource(R.string.release_date_unknown)
    ReleaseDate.Tba -> UiText.StringResource(R.string.release_date_tba)
    is ReleaseDate.Exact -> UiText.DynamicString(date.toString())
    is ReleaseDate.YearMonthOnly -> UiText.DynamicString(toApiString())
    is ReleaseDate.YearOnly -> UiText.DynamicString(toApiString())
}

@Composable
fun ReleaseDate?.displayText(): String = toUiText().asString()

@StringRes
fun ReleaseWindow.labelResId(): Int = when (this) {
    ReleaseWindow.ALL -> R.string.release_window_all
    ReleaseWindow.LATEST -> R.string.release_window_latest
    ReleaseWindow.UPCOMING -> R.string.release_window_upcoming
}

@StringRes
fun AndOr.labelResId(): Int = when (this) {
    AndOr.AND -> R.string.filter_logic_and
    AndOr.OR -> R.string.filter_logic_or
}

@StringRes
fun SortField.labelResId(): Int = when (this) {
    SortField.TITLE -> R.string.sort_title
    SortField.RATING -> R.string.sort_rating
    SortField.VOTE_COUNT -> R.string.sort_vote_count
    SortField.RELEASED -> R.string.sort_release_date
    SortField.LENGTH_MINUTES -> R.string.sort_length
    SortField.ADDED -> R.string.sort_added_date
    SortField.STARTED -> R.string.sort_started_date
    SortField.FINISHED -> R.string.sort_finished_date
    SortField.LAST_MODIFIED -> R.string.sort_last_modified
    SortField.USER_VOTE -> R.string.sort_user_vote
    SortField.SEARCH_RANK -> R.string.sort_search_rank
    SortField.ID -> R.string.sort_id
    SortField.COLLECTION -> R.string.home_section_my_collection
}

@Composable
fun SortField.displayLabel(): String = stringResource(labelResId())

@StringRes
fun HomeSection.titleResId(): Int = when (this) {
    HomeSection.COLLECTION -> R.string.home_section_my_collection
    HomeSection.TOP_RATED -> R.string.home_section_top_rated
    HomeSection.TOP_VOTED -> R.string.home_section_most_voted
    HomeSection.POPULAR_ONGOING -> R.string.home_section_popular_ongoing
    HomeSection.LATEST_RELEASES -> R.string.home_section_latest_releases
    HomeSection.UPCOMING_RELEASES -> R.string.home_section_upcoming_releases
}

@StringRes
fun ThemeId.displayNameResId(): Int = ThemeSpecs.forId(this).displayNameResId

@StringRes
fun Language.labelResId(): Int = when (this) {
    Language.AR -> R.string.lang_ar
    Language.BG -> R.string.lang_bg
    Language.CA -> R.string.lang_ca
    Language.CS -> R.string.lang_cs
    Language.DE -> R.string.lang_de
    Language.EL -> R.string.lang_el
    Language.EN -> R.string.lang_en
    Language.ES -> R.string.lang_es
    Language.FI -> R.string.lang_fi
    Language.FR -> R.string.lang_fr
    Language.HR -> R.string.lang_hr
    Language.HU -> R.string.lang_hu
    Language.ID -> R.string.lang_id
    Language.IT -> R.string.lang_it
    Language.JA -> R.string.lang_ja
    Language.KO -> R.string.lang_ko
    Language.MS -> R.string.lang_ms
    Language.NL -> R.string.lang_nl
    Language.NO -> R.string.lang_no
    Language.PL -> R.string.lang_pl
    Language.PT_BR -> R.string.lang_pt_br
    Language.PT_PT -> R.string.lang_pt_pt
    Language.RO -> R.string.lang_ro
    Language.RU -> R.string.lang_ru
    Language.SK -> R.string.lang_sk
    Language.SR -> R.string.lang_sr
    Language.SV -> R.string.lang_sv
    Language.TA -> R.string.lang_ta
    Language.TH -> R.string.lang_th
    Language.TR -> R.string.lang_tr
    Language.UK -> R.string.lang_uk
    Language.VI -> R.string.lang_vi
    Language.ZH_HANS -> R.string.lang_zh_hans
    Language.ZH_HANT -> R.string.lang_zh_hant
}

fun Language?.flagAssetUri(): String = AssetPaths.languageFlag(this)

@StringRes
fun Platform.labelResId(): Int = when (this) {
    Platform.WIN -> R.string.platform_win
    Platform.LIN -> R.string.platform_lin
    Platform.MAC -> R.string.platform_mac
    Platform.WEB -> R.string.platform_web
    Platform.AND -> R.string.platform_and
    Platform.IOS -> R.string.platform_ios
    Platform.DVD -> R.string.platform_dvd
    Platform.BDP -> R.string.platform_bdp
    Platform.DOS -> R.string.platform_dos
    Platform.P88 -> R.string.platform_p88
    Platform.P98 -> R.string.platform_p98
    Platform.FMT -> R.string.platform_fmt
    Platform.FM7 -> R.string.platform_fm7
    Platform.FM8 -> R.string.platform_fm8
    Platform.MSX -> R.string.platform_msx
    Platform.X68 -> R.string.platform_x68
    Platform.NES -> R.string.platform_nes
    Platform.SFC -> R.string.platform_sfc
    Platform.GBA -> R.string.platform_gba
    Platform.NDS -> R.string.platform_nds
    Platform.N3D -> R.string.platform_n3d
    Platform.WII -> R.string.platform_wii
    Platform.WIU -> R.string.platform_wiu
    Platform.SWI -> R.string.platform_swi
    Platform.PS1 -> R.string.platform_ps1
    Platform.PS2 -> R.string.platform_ps2
    Platform.PS3 -> R.string.platform_ps3
    Platform.PS4 -> R.string.platform_ps4
    Platform.PS5 -> R.string.platform_ps5
    Platform.PSP -> R.string.platform_psp
    Platform.PSV -> R.string.platform_psv
    Platform.DRC -> R.string.platform_drc
    Platform.SAT -> R.string.platform_sat
    Platform.VND -> R.string.platform_vnd
    Platform.XB1 -> R.string.platform_xb1
    Platform.XB3 -> R.string.platform_xb3
    Platform.XBO -> R.string.platform_xbo
    Platform.XXS -> R.string.platform_xxs
    Platform.TDO -> R.string.platform_tdo
    Platform.GBC -> R.string.platform_gbc
    Platform.SW2 -> R.string.platform_sw2
    Platform.PCE -> R.string.platform_pce
    Platform.PCF -> R.string.platform_pcf
    Platform.SMD -> R.string.platform_smd
    Platform.SCD -> R.string.platform_scd
    Platform.X1S -> R.string.platform_x1s
    Platform.MOB -> R.string.platform_mob
    Platform.OTH -> R.string.platform_oth
}

fun Platform?.iconAssetUri(): String = AssetPaths.platformIcon(this)

object VoteFormatters {
    /**
     * Ordered vote options in VNDB's `10..100` integer scale (`null` = unvoted) paired with
     * localized string resource IDs (A7, A8, A11).
     */
    val options: List<Pair<Int?, Int>> = listOf(
        null to R.string.collection_vote_none,
        100 to R.string.vote_10_masterpiece,
        90 to R.string.vote_9_excellent,
        80 to R.string.vote_8_very_good,
        70 to R.string.vote_7_good,
        60 to R.string.vote_6_decent,
        50 to R.string.vote_5_so_so,
        40 to R.string.vote_4_weak,
        30 to R.string.vote_3_bad,
        20 to R.string.vote_2_awful,
        10 to R.string.vote_1_worst
    )

    @StringRes
    fun labelResIdForVote(vote: Int?): Int {
        if (vote == null || vote !in 10..100) return R.string.collection_vote_none
        val bucket = ((vote + 5) / 10).coerceIn(1, 10) * 10
        return options.firstOrNull { it.first == bucket }?.second ?: R.string.collection_vote_none
    }

    fun formatTenPointRating(ratingTenToHundred: Double?): String? {
        val raw = ratingTenToHundred?.takeIf { it > 0.0 && !it.isNaN() } ?: return null
        val tenScale = if (raw > 10.0) raw / 10.0 else raw
        return String.format(Locale.US, "%.2f", tenScale)
    }

    fun formatTenPointVote(voteTenToHundred: Int?): String? {
        val raw = voteTenToHundred?.takeIf { it in 10..100 } ?: return null
        return if (raw % 10 == 0) {
            (raw / 10).toString()
        } else {
            String.format(Locale.US, "%.1f", raw / 10.0)
        }
    }
}
