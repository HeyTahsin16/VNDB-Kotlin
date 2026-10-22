package com.vndblite.dcj.ui.format

import androidx.annotation.StringRes
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CharacterMeasurements
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
import java.time.LocalDate
import java.util.Locale

/**
 * Pure presentation formatters for domain models (A6, A7, A8, D2).
 */
object VnFormatters {

    fun formatRating(rating: Double?): String {
        if (rating == null || rating <= 0.0 || rating.isNaN()) return "--"
        val normalized = if (rating > 10.0) rating / 10.0 else rating
        return String.format(Locale.US, "%.2f", normalized)
    }

    fun formatVoteTenPoint(vote: Int?): String? {
        if (vote == null || vote < 10 || vote > 100) return null
        val tenPoint = vote / 10.0
        return if (vote % 10 == 0) {
            (vote / 10).toString()
        } else {
            String.format(Locale.US, "%.1f", tenPoint)
        }
    }

    fun formatReleaseDate(releaseDate: ReleaseDate?): UiText = when (releaseDate) {
        null -> UiText.StringResource(R.string.label_unknown)
        ReleaseDate.Tba -> UiText.StringResource(R.string.label_tba)
        is ReleaseDate.Exact -> UiText.DynamicString(releaseDate.date.toString())
        is ReleaseDate.YearMonthOnly -> UiText.DynamicString(releaseDate.toApiString())
        is ReleaseDate.YearOnly -> UiText.DynamicString(releaseDate.toApiString())
    }

    fun formatLocalDate(date: LocalDate?): UiText =
        if (date != null) UiText.DynamicString(date.toString())
        else UiText.StringResource(R.string.label_not_started)

    fun formatPlayLength(lengthMinutes: Int?, bucket: VnLengthBucket?): UiText {
        if (lengthMinutes != null && lengthMinutes > 0) {
            val hours = lengthMinutes / 60
            val minutes = lengthMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> UiText.StringResource(R.string.length_hours_minutes, hours, minutes)
                hours > 0 -> UiText.StringResource(R.string.length_hours_only, hours)
                else -> UiText.StringResource(R.string.length_minutes_only, minutes)
            }
        }
        return when (bucket) {
            VnLengthBucket.VERY_SHORT -> UiText.StringResource(R.string.length_very_short)
            VnLengthBucket.SHORT -> UiText.StringResource(R.string.length_short)
            VnLengthBucket.MEDIUM -> UiText.StringResource(R.string.length_medium)
            VnLengthBucket.LONG -> UiText.StringResource(R.string.length_long)
            VnLengthBucket.VERY_LONG -> UiText.StringResource(R.string.length_very_long)
            null -> UiText.StringResource(R.string.label_unknown)
        }
    }

    fun formatMeasurements(measurements: CharacterMeasurements?): String? {
        if (measurements == null) return null
        val b = measurements.bustCm
        val w = measurements.waistCm
        val h = measurements.hipsCm
        if (b == null && w == null && h == null) return null
        val bStr = b?.toString() ?: "?"
        val wStr = w?.toString() ?: "?"
        val hStr = h?.toString() ?: "?"
        val cupSuffix = measurements.cupSize?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
        return "$bStr/$wStr/$hStr cm$cupSuffix"
    }

    @StringRes
    fun voteScoreLabelRes(tenPointVote: Int?): Int = when (tenPointVote) {
        1 -> R.string.vote_1
        2 -> R.string.vote_2
        3 -> R.string.vote_3
        4 -> R.string.vote_4
        5 -> R.string.vote_5
        6 -> R.string.vote_6
        7 -> R.string.vote_7
        8 -> R.string.vote_8
        9 -> R.string.vote_9
        10 -> R.string.vote_10
        else -> R.string.vote_none
    }
}

@StringRes
fun ThemeId.displayNameRes(): Int = when (this) {
    ThemeId.AGL -> R.string.theme_name_agl
    ThemeId.AIR -> R.string.theme_name_air
    ThemeId.EV17 -> R.string.theme_name_ev17
    ThemeId.FATE -> R.string.theme_name_fate
    ThemeId.CARNVL -> R.string.theme_name_carnvl
    ThemeId.HGRSHI -> R.string.theme_name_hgrshi
    ThemeId.TSUKHME -> R.string.theme_name_tsukhme
    ThemeId.TOHO -> R.string.theme_name_toho
    ThemeId.SEINRKN -> R.string.theme_name_seinrkn
    ThemeId.LILBUSTS -> R.string.theme_name_lilbusts
    ThemeId.SAYA -> R.string.theme_name_saya
}

@StringRes
fun CollectionStatus.labelRes(): Int = when (this) {
    CollectionStatus.PLAYING -> R.string.status_playing
    CollectionStatus.FINISHED -> R.string.status_finished
    CollectionStatus.STALLED -> R.string.status_stalled
    CollectionStatus.DROPPED -> R.string.status_dropped
    CollectionStatus.WISHLIST -> R.string.status_wishlist
    CollectionStatus.BLACKLIST -> R.string.status_blacklist
}

@StringRes
fun DevStatus?.labelRes(): Int = when (this) {
    DevStatus.FINISHED -> R.string.dev_status_finished
    DevStatus.IN_DEVELOPMENT -> R.string.dev_status_in_development
    DevStatus.CANCELLED -> R.string.dev_status_cancelled
    null -> R.string.dev_status_unknown
}

@StringRes
fun HomeSection.titleRes(): Int = when (this) {
    HomeSection.COLLECTION -> R.string.home_section_collection
    HomeSection.TOP_RATED -> R.string.home_section_top_rated
    HomeSection.TOP_VOTED -> R.string.home_section_top_voted
    HomeSection.POPULAR_ONGOING -> R.string.home_section_popular_ongoing
    HomeSection.LATEST_RELEASES -> R.string.home_section_latest_releases
    HomeSection.UPCOMING_RELEASES -> R.string.home_section_upcoming_releases
}

fun HomeSection.seeMoreTestTag(): String = when (this) {
    HomeSection.COLLECTION -> "see_more_collection"
    HomeSection.TOP_RATED -> "see_more_top_rated"
    HomeSection.TOP_VOTED -> "see_more_top_voted"
    HomeSection.POPULAR_ONGOING -> "see_more_popular_ongoing"
    HomeSection.LATEST_RELEASES -> "see_more_latest_releases"
    HomeSection.UPCOMING_RELEASES -> "see_more_upcoming_releases"
}

@StringRes
fun SortField.labelRes(): Int = when (this) {
    SortField.SEARCH_RANK -> R.string.sort_search_rank
    SortField.ID -> R.string.sort_id
    SortField.TITLE -> R.string.sort_title
    SortField.ADDED -> R.string.sort_added
    SortField.STARTED -> R.string.sort_started
    SortField.FINISHED -> R.string.sort_finished
    SortField.LAST_MODIFIED -> R.string.sort_last_modified
    SortField.LENGTH_MINUTES -> R.string.sort_length
    SortField.RATING -> R.string.sort_rating
    SortField.RELEASED -> R.string.sort_released
    SortField.VOTE_COUNT -> R.string.sort_vote_count
    SortField.USER_VOTE -> R.string.sort_user_vote
    SortField.COLLECTION -> R.string.sort_collection
}

@StringRes
fun ReleaseWindow.labelRes(): Int = when (this) {
    ReleaseWindow.ALL -> R.string.search_release_all
    ReleaseWindow.LATEST -> R.string.search_release_latest
    ReleaseWindow.UPCOMING -> R.string.search_release_upcoming
}

@StringRes
fun CharacterRole.labelRes(): Int = when (this) {
    CharacterRole.MAIN -> R.string.char_role_main
    CharacterRole.PRIMARY -> R.string.char_role_primary
    CharacterRole.SIDE -> R.string.char_role_side
    CharacterRole.APPEARS -> R.string.char_role_appears
}

@StringRes
fun CharacterSex.labelRes(): Int = when (this) {
    CharacterSex.FEMALE -> R.string.char_sex_female
    CharacterSex.MALE -> R.string.char_sex_male
    CharacterSex.BOTH -> R.string.char_sex_both
    CharacterSex.SEXLESS -> R.string.char_sex_sexless
}

@StringRes
fun VnRelationType.labelRes(): Int = when (this) {
    VnRelationType.SEQUEL -> R.string.relation_sequel
    VnRelationType.PREQUEL -> R.string.relation_prequel
    VnRelationType.SAME_SETTING -> R.string.relation_same_setting
    VnRelationType.ALTERNATIVE_VERSION -> R.string.relation_alternative_version
    VnRelationType.SHARES_CHARACTERS -> R.string.relation_shares_characters
    VnRelationType.SIDE_STORY -> R.string.relation_side_story
    VnRelationType.PARENT_STORY -> R.string.relation_parent_story
    VnRelationType.SAME_SERIES -> R.string.relation_same_series
    VnRelationType.FANDISC -> R.string.relation_fandisc
    VnRelationType.ORIGINAL_GAME -> R.string.relation_original_game
    VnRelationType.OTHER -> R.string.relation_other
}

@StringRes
fun Language?.labelRes(): Int = when (this) {
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
    null -> R.string.lang_other
}

@StringRes
fun Platform?.labelRes(): Int = when (this) {
    Platform.WIN -> R.string.platform_win
    Platform.LIN -> R.string.platform_lin
    Platform.MAC -> R.string.platform_mac
    Platform.WEB -> R.string.platform_web
    Platform.TDO -> R.string.platform_tdo
    Platform.IOS -> R.string.platform_ios
    Platform.AND -> R.string.platform_and
    Platform.BDP -> R.string.platform_bdp
    Platform.DOS -> R.string.platform_dos
    Platform.DVD -> R.string.platform_dvd
    Platform.DRC -> R.string.platform_drc
    Platform.NES -> R.string.platform_nes
    Platform.SFC -> R.string.platform_sfc
    Platform.FM7 -> R.string.platform_fm7
    Platform.FM8 -> R.string.platform_fm8
    Platform.FMT -> R.string.platform_fmt
    Platform.GBA -> R.string.platform_gba
    Platform.GBC -> R.string.platform_gbc
    Platform.MSX -> R.string.platform_msx
    Platform.NDS -> R.string.platform_nds
    Platform.SWI -> R.string.platform_swi
    Platform.SW2 -> R.string.platform_sw2
    Platform.WII -> R.string.platform_wii
    Platform.WIU -> R.string.platform_wiu
    Platform.N3D -> R.string.platform_n3d
    Platform.P88 -> R.string.platform_p88
    Platform.P98 -> R.string.platform_p98
    Platform.PCE -> R.string.platform_pce
    Platform.PCF -> R.string.platform_pcf
    Platform.PSP -> R.string.platform_psp
    Platform.PS1 -> R.string.platform_ps1
    Platform.PS2 -> R.string.platform_ps2
    Platform.PS3 -> R.string.platform_ps3
    Platform.PS4 -> R.string.platform_ps4
    Platform.PS5 -> R.string.platform_ps5
    Platform.PSV -> R.string.platform_psv
    Platform.SMD -> R.string.platform_smd
    Platform.SCD -> R.string.platform_scd
    Platform.SAT -> R.string.platform_sat
    Platform.VND -> R.string.platform_vnd
    Platform.X1S -> R.string.platform_x1s
    Platform.X68 -> R.string.platform_x68
    Platform.XB1 -> R.string.platform_xb1
    Platform.XB3 -> R.string.platform_xb3
    Platform.XBO -> R.string.platform_xbo
    Platform.XXS -> R.string.platform_xxs
    Platform.MOB -> R.string.platform_mob
    Platform.OTH, null -> R.string.platform_oth
}
