package com.vndblite.dcj.ui.theme

import com.vndblite.dcj.R
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.CharacterMeasurements
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.ui.format.UiText
import com.vndblite.dcj.ui.format.VnFormatters
import com.vndblite.dcj.ui.format.toUiText
import com.vndblite.dcj.ui.navigation.CharacterSearchRoute
import com.vndblite.dcj.ui.navigation.SearchRoute
import com.vndblite.dcj.ui.navigation.VnDetailRoute
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ThemeAndFormattersTest {

    @Test
    fun `all 11 themes meet WCAG 4_5 contrast ratio between surface and content`() {
        assertEquals(11, ThemeSpecs.all.size)
        for (spec in ThemeSpecs.all) {
            val ratio = spec.surfaceContentContrastRatio()
            assertTrue(
                "Theme ${spec.themeId} contrast ratio $ratio is below 4.5",
                ratio >= 4.5f
            )
        }
    }

    @Test
    fun `hasDarkSurface and paletteLabelRes are derived from surface luminance`() {
        val agl = ThemeId.AGL.toThemeSpec()
        assertTrue(agl.hasDarkSurface)
        assertEquals(R.string.theme_palette_dark, agl.paletteLabelRes)

        val air = ThemeId.AIR.toThemeSpec()
        assertTrue(!air.hasDarkSurface)
        assertEquals(R.string.theme_palette_light, air.paletteLabelRes)
    }

    @Test
    fun `VnFormatters formats ratings votes dates lengths and measurements accurately`() {
        assertEquals("--", VnFormatters.formatRating(null))
        assertEquals("--", VnFormatters.formatRating(0.0))
        assertEquals("8.75", VnFormatters.formatRating(87.5))
        assertEquals("8.75", VnFormatters.formatRating(8.75))

        assertNull(VnFormatters.formatVoteTenPoint(null))
        assertEquals("8", VnFormatters.formatVoteTenPoint(80))
        assertEquals("8.5", VnFormatters.formatVoteTenPoint(85))

        assertEquals(
            UiText.StringResource(R.string.label_unknown),
            VnFormatters.formatReleaseDate(null)
        )
        assertEquals(
            UiText.StringResource(R.string.label_tba),
            VnFormatters.formatReleaseDate(ReleaseDate.Tba)
        )
        assertEquals(
            UiText.DynamicString("2004-01-30"),
            VnFormatters.formatReleaseDate(ReleaseDate.Exact(LocalDate.of(2004, 1, 30)))
        )

        assertEquals(
            UiText.StringResource(R.string.length_hours_minutes, 2, 15),
            VnFormatters.formatPlayLength(135, VnLengthBucket.SHORT)
        )
        assertEquals(
            UiText.StringResource(R.string.length_medium),
            VnFormatters.formatPlayLength(null, VnLengthBucket.MEDIUM)
        )

        assertNull(VnFormatters.formatMeasurements(null))
        assertEquals(
            "82/56/84 cm (C)",
            VnFormatters.formatMeasurements(
                CharacterMeasurements(bustCm = 82, waistCm = 56, hipsCm = 84, cupSize = "C")
            )
        )
    }

    @Test
    fun `ApiError maps to localized UiText resources`() {
        assertEquals(
            UiText.StringResource(R.string.error_offline),
            ApiError.Offline.toUiText()
        )
        assertEquals(
            UiText.StringResource(R.string.error_unauthorized),
            ApiError.Unauthorized.toUiText()
        )
        assertEquals(
            UiText.StringResource(R.string.error_sync_in_progress),
            ApiError.SyncInProgress.toUiText()
        )
        assertEquals(
            UiText.StringResource(R.string.error_rate_limited_seconds, 12L),
            ApiError.RateLimited(retryAfterSeconds = 12L).toUiText()
        )
    }

    @Test
    fun `type safe navigation routes serialize and deserialize losslessly`() {
        val search = SearchRoute(
            initialQuery = "Steins",
            initialTagId = "g7",
            initialTagName = "Sci-Fi",
            initialSortFieldCode = "rating",
            initialReverseSort = true
        )
        val encodedSearch = Json.encodeToString(SearchRoute.serializer(), search)
        assertEquals(search, Json.decodeFromString(SearchRoute.serializer(), encodedSearch))

        val charRoute = CharacterSearchRoute(
            initialQuery = "Saber",
            initialTraitId = "i12",
            initialTraitName = "Ahoge",
            initialTraitGroupName = "Hair"
        )
        val encodedChar = Json.encodeToString(CharacterSearchRoute.serializer(), charRoute)
        assertEquals(charRoute, Json.decodeFromString(CharacterSearchRoute.serializer(), encodedChar))

        val detailRoute = VnDetailRoute(vnId = "v17")
        val encodedDetail = Json.encodeToString(VnDetailRoute.serializer(), detailRoute)
        assertEquals(detailRoute, Json.decodeFromString(VnDetailRoute.serializer(), encodedDetail))
    }
}
