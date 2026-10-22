package com.vndblite.dcj.ui.format

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.R
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AndOr
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SyncFailure
import com.vndblite.dcj.domain.model.SyncReport
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnRelationType
import com.vndblite.dcj.sync.SyncProgress
import com.vndblite.dcj.sync.SyncStage
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verification tests for domain/screen presentation formatters and string resources (A8, A9, A11).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FormattersTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun assertAssetExists(assetUri: String) {
        val prefix = "file:///android_asset/"
        assertTrue("Expected URI starting with $prefix but got $assetUri", assetUri.startsWith(prefix))
        val relativePath = assetUri.removePrefix(prefix)
        context.assets.open(relativePath).use { stream ->
            assertTrue("Expected non-empty asset at $relativePath", stream.available() > 0)
        }
    }

    @Test
    fun `all domain enum labelResId mappings and packaged assets resolve and open cleanly`() {
        ThemeId.entries.forEach { themeId ->
            assertTrue(context.getString(themeId.displayNameResId()).isNotBlank())
            assertAssetExists(com.vndblite.dcj.ui.components.AssetPaths.themeBackground(themeId))
        }
        CollectionStatus.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
            assertAssetExists(it.iconAssetUri())
        }
        DevStatus.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        CharacterRole.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        (CharacterSex.entries + listOf<CharacterSex?>(null)).forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        (VnLengthBucket.entries + listOf<VnLengthBucket?>(null)).forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        VnRelationType.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        ReleaseWindow.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        AndOr.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        SortField.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
        }
        HomeSection.entries.forEach {
            assertTrue(context.getString(it.titleResId()).isNotBlank())
        }
        Language.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
            assertAssetExists(it.flagAssetUri())
        }
        Platform.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
            assertAssetExists(it.iconAssetUri())
        }
        GenrePreset.entries.forEach {
            assertTrue(context.getString(it.labelResId()).isNotBlank())
            assertAssetExists(it.genrePresetImageUri())
        }
        assertAssetExists(com.vndblite.dcj.ui.components.AssetPaths.APP_ICON)
        assertAssetExists(com.vndblite.dcj.ui.components.AssetPaths.GITHUB_ICON)
        (1..3).forEach { step ->
            assertAssetExists(com.vndblite.dcj.ui.components.AssetPaths.tutorialImage(step))
        }
    }

    @Test
    fun `VoteFormatters formats ten-point ratings, user votes, and bucket labels accurately`() {
        assertNull(VoteFormatters.formatTenPointRating(null))
        assertNull(VoteFormatters.formatTenPointRating(0.0))
        assertEquals("8.64", VoteFormatters.formatTenPointRating(86.4))
        assertEquals("8.64", VoteFormatters.formatTenPointRating(8.64))

        assertNull(VoteFormatters.formatTenPointVote(null))
        assertNull(VoteFormatters.formatTenPointVote(5))
        assertEquals("10", VoteFormatters.formatTenPointVote(100))
        assertEquals("8", VoteFormatters.formatTenPointVote(80))
        assertEquals("8.5", VoteFormatters.formatTenPointVote(85))

        assertEquals(R.string.collection_vote_none, VoteFormatters.labelResIdForVote(null))
        assertEquals(R.string.vote_10_masterpiece, VoteFormatters.labelResIdForVote(100))
        assertEquals(R.string.vote_9_excellent, VoteFormatters.labelResIdForVote(88))
        assertEquals(R.string.vote_1_worst, VoteFormatters.labelResIdForVote(10))
    }

    @Test
    fun `formatVnLengthUiText and ReleaseDate toUiText format partial and exact values`() {
        val withHoursAndBucket = formatVnLengthUiText(150, VnLengthBucket.SHORT).asString(context)
        assertTrue(withHoursAndBucket.contains("2h 30m"))

        val bucketOnly = formatVnLengthUiText(null, VnLengthBucket.LONG).asString(context)
        assertEquals(context.getString(R.string.length_long), bucketOnly)

        assertEquals("2009-10-15", ReleaseDate.Exact(LocalDate.of(2009, 10, 15)).toUiText().asString(context))
        assertEquals("2025-04", ReleaseDate.YearMonthOnly(2025, 4).toUiText().asString(context))
        assertEquals("2026", ReleaseDate.YearOnly(2026).toUiText().asString(context))
        assertEquals(context.getString(R.string.release_date_tba), ReleaseDate.Tba.toUiText().asString(context))
        assertEquals(context.getString(R.string.release_date_unknown), (null as ReleaseDate?).toUiText().asString(context))
    }

    @Test
    fun `Trait displayLabel and SyncProgress status UiText format accurately`() {
        val grouped = Trait(id = "i1", name = "Tsundere", groupName = "Personality")
        val ungrouped = Trait(id = "i2", name = "Glasses", groupName = null)
        assertEquals("Personality: Tsundere", grouped.displayLabel)
        assertEquals("Glasses", ungrouped.displayLabel)

        assertNull(SyncProgress(stage = SyncStage.IDLE).toStatusUiText())
        assertNotNull(SyncProgress(stage = SyncStage.AUTHENTICATING, isRunning = true).toStatusUiText())

        val completeReport = SyncReport(pushed = 2, pulled = 3, deletedLocal = 1, deletedRemote = 0)
        assertTrue(completeReport.toSummaryUiText().asString(context).contains("2"))

        val partialReport = SyncReport(
            pushed = 1,
            pulled = 0,
            deletedLocal = 0,
            deletedRemote = 0,
            failed = listOf(SyncFailure("v17", "push", ApiError.Timeout))
        )
        assertTrue(partialReport.toSummaryUiText().asString(context).contains("1"))
    }
}
