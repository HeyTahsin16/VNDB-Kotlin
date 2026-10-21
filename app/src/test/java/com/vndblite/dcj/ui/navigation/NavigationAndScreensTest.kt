package com.vndblite.dcj.ui.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.QuickGenrePreset
import com.vndblite.dcj.domain.model.ReleaseWindow
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSearchFilters
import com.vndblite.dcj.ui.screens.search.ActiveFilterChip
import com.vndblite.dcj.ui.screens.search.SearchFilterMutations
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric & unit verification for navigation routes, `BottomNavTab`, `SearchCommandBus`,
 * and `SearchFilterMutations` (Section 10, B4, A4, A5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationAndScreensTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `SearchCommandBus dispatches and consumes commands atomically`() {
        val bus = SearchCommandBus()
        assertNull(bus.pendingCommand.value)

        val tagCmd = SearchNavCommand.FilterVnsByTag(Tag(id = "g7", name = "Sci-fi"))
        bus.dispatch(tagCmd)
        assertEquals(tagCmd, bus.pendingCommand.value)
        assertEquals(tagCmd, bus.consume())
        assertNull(bus.pendingCommand.value)
        assertNull(bus.consume())

        val traitCmd = SearchNavCommand.OpenCharacterExplorer(Trait(id = "i1", name = "Tsundere"))
        bus.dispatch(traitCmd)
        assertEquals(traitCmd, bus.consume())

        val homeCmd = SearchNavCommand.ApplyHomeSection(HomeSection.TOP_RATED)
        bus.dispatch(homeCmd)
        assertEquals(homeCmd, bus.consume())
    }

    @Test
    fun `BottomNavTab metadata and serializable routes resolve valid resources and round-trip`() {
        assertEquals(4, BottomNavTab.entries.size)
        BottomNavTab.entries.forEach { tab ->
            assertTrue(context.getString(tab.labelResId).isNotBlank())
            assertTrue(tab.testTag.startsWith("nav_"))
        }

        val detailRoute = VnDetailRoute(vnId = "v17", initialTitle = "Ever17 -the out of infinity-")
        val encoded = Json.encodeToString(detailRoute)
        val decoded = Json.decodeFromString<VnDetailRoute>(encoded)
        assertEquals(detailRoute, decoded)
    }

    @Test
    fun `SearchFilterMutations toggles quick genres, removes chips, and maps HomeSection presets`() {
        val initial = VnSearchFilters()
        val withAction = SearchFilterMutations.toggleQuickGenre(initial, QuickGenrePreset.ACTION)
        assertEquals(1, withAction.tags.size)
        assertEquals(QuickGenrePreset.ACTION.tagId, withAction.tags.first().id)

        val toggledOff = SearchFilterMutations.toggleQuickGenre(withAction, QuickGenrePreset.ACTION)
        assertTrue(toggledOff.tags.isEmpty())

        val populated = VnSearchFilters(
            tags = listOf(Tag("g7", "Sci-fi")),
            characterTraits = listOf(Trait("i1", "Tsundere")),
            developers = listOf(Producer("p1", "KID")),
            languages = listOf(Language.EN),
            originalLanguages = listOf(Language.JA),
            platforms = listOf(Platform.WIN),
            devStatuses = listOf(DevStatus.FINISHED),
            characterRoles = listOf(CharacterRole.MAIN),
            characterSexes = listOf(CharacterSex.FEMALE),
            releaseWindow = ReleaseWindow.LATEST
        )

        var current = populated
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.TagChip(Tag("g7", "Sci-fi")))
        assertTrue(current.tags.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.TraitChip(Trait("i1", "Tsundere")))
        assertTrue(current.characterTraits.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.DeveloperChip(Producer("p1", "KID")))
        assertTrue(current.developers.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.LanguageChip(Language.EN))
        assertTrue(current.languages.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.OriginLanguageChip(Language.JA))
        assertTrue(current.originalLanguages.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.PlatformChip(Platform.WIN))
        assertTrue(current.platforms.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.DevStatusChip(DevStatus.FINISHED))
        assertTrue(current.devStatuses.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.CharacterRoleChip(CharacterRole.MAIN))
        assertTrue(current.characterRoles.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.CharacterSexChip(CharacterSex.FEMALE))
        assertTrue(current.characterSexes.isEmpty())
        current = SearchFilterMutations.removeChip(current, ActiveFilterChip.ReleaseWindowChip)
        assertEquals(ReleaseWindow.ANY, current.releaseWindow)
        assertEquals(VnSearchFilters(), current)

        val (ongoingFilters, ongoingSort) = SearchFilterMutations.filtersAndSortForHomeSection(HomeSection.POPULAR_ONGOING)
        assertEquals(listOf(DevStatus.IN_DEVELOPMENT), ongoingFilters.devStatuses)
        assertEquals(SortField.VOTE_COUNT, ongoingSort.field)
        assertTrue(ongoingSort.reverse)
    }
}
