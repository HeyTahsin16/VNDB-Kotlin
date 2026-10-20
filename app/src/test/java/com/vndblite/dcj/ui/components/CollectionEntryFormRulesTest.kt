package com.vndblite.dcj.ui.components

import android.os.Build
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CollectionEntryFormRulesTest {

    private val sampleToday = LocalDate.of(2026, 4, 10)

    @Test
    fun `initialStartedDate pre-fills today ONLY for brand-new PLAYING entries and never for WISHLIST or BLACKLIST (F8)`() {
        assertEquals(
            sampleToday,
            CollectionEntryFormRules.initialStartedDate(
                existingEntry = null,
                status = CollectionStatus.PLAYING,
                today = sampleToday
            )
        )

        assertNull(
            CollectionEntryFormRules.initialStartedDate(
                existingEntry = null,
                status = CollectionStatus.WISHLIST,
                today = sampleToday
            )
        )
        assertNull(
            CollectionEntryFormRules.initialStartedDate(
                existingEntry = null,
                status = CollectionStatus.BLACKLIST,
                today = sampleToday
            )
        )
        assertNull(
            CollectionEntryFormRules.initialStartedDate(
                existingEntry = null,
                status = CollectionStatus.FINISHED,
                today = sampleToday
            )
        )

        val existingWishlist = CollectionEntry(
            vnId = "v17",
            status = CollectionStatus.WISHLIST,
            startedDate = null,
            lastModifiedAt = Instant.parse("2026-04-01T00:00:00Z")
        )
        assertNull(
            CollectionEntryFormRules.initialStartedDate(
                existingEntry = existingWishlist,
                status = CollectionStatus.WISHLIST,
                today = sampleToday
            )
        )
    }

    @Test
    fun `switching a new entry from PLAYING to WISHLIST or BLACKLIST clears auto-prefilled start date (F8)`() {
        val afterSwitchToWishlist = CollectionEntryFormRules.onStatusChanged(
            isNewEntry = true,
            previousStatus = CollectionStatus.PLAYING,
            newStatus = CollectionStatus.WISHLIST,
            currentStartedDate = sampleToday,
            today = sampleToday
        )
        assertNull(afterSwitchToWishlist)

        val afterSwitchBackToPlaying = CollectionEntryFormRules.onStatusChanged(
            isNewEntry = true,
            previousStatus = CollectionStatus.WISHLIST,
            newStatus = CollectionStatus.PLAYING,
            currentStartedDate = afterSwitchToWishlist,
            today = sampleToday
        )
        assertEquals(sampleToday, afterSwitchBackToPlaying)

        val customDate = LocalDate.of(2025, 12, 25)
        val preservedCustom = CollectionEntryFormRules.onStatusChanged(
            isNewEntry = true,
            previousStatus = CollectionStatus.PLAYING,
            newStatus = CollectionStatus.WISHLIST,
            currentStartedDate = customDate,
            today = sampleToday
        )
        assertEquals(customDate, preservedCustom)
    }

    @Test
    fun `isDateRangeValid enforces finishedDate greater than or equal to startedDate (F8)`() {
        val start = LocalDate.of(2026, 3, 10)
        val sameDay = LocalDate.of(2026, 3, 10)
        val later = LocalDate.of(2026, 3, 20)
        val earlier = LocalDate.of(2026, 3, 1)

        assertTrue(CollectionEntryFormRules.isDateRangeValid(null, null))
        assertTrue(CollectionEntryFormRules.isDateRangeValid(start, null))
        assertTrue(CollectionEntryFormRules.isDateRangeValid(null, later))
        assertTrue(CollectionEntryFormRules.isDateRangeValid(start, sameDay))
        assertTrue(CollectionEntryFormRules.isDateRangeValid(start, later))
        assertFalse(CollectionEntryFormRules.isDateRangeValid(start, earlier))
    }

    @Test
    fun `shouldUseHardwareModifierBlur selects exactly one blur mechanism based on API level (U3)`() {
        assertTrue(shouldUseHardwareModifierBlur(Build.VERSION_CODES.S))
        assertTrue(shouldUseHardwareModifierBlur(Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
        assertFalse(shouldUseHardwareModifierBlur(Build.VERSION_CODES.R))
        assertFalse(shouldUseHardwareModifierBlur(Build.VERSION_CODES.Q))
    }

    @Test
    fun `AssetPaths builds valid android_asset URIs for themes statuses flags and platforms (U4)`() {
        assertEquals(
            "file:///android_asset/images/bg/bgEver17.png",
            AssetPaths.toCoilAssetUri(AssetPaths.themeBackground(ThemeId.EV17))
        )
        assertEquals(
            "file:///android_asset/images/status/playing.png",
            AssetPaths.toCoilAssetUri(AssetPaths.statusIcon(CollectionStatus.PLAYING))
        )
        assertEquals(
            "file:///android_asset/images/flags/ja.png",
            AssetPaths.toCoilAssetUri(AssetPaths.languageFlag(Language.JA))
        )
        assertEquals(
            "file:///android_asset/images/os/win.png",
            AssetPaths.toCoilAssetUri(AssetPaths.platformIcon(Platform.WIN))
        )
    }
}
