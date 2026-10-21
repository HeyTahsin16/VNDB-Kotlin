package com.vndblite.dcj.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Type-safe `@Serializable` routes for Jetpack Navigation Compose (Section 5, B4, A5).
 */
@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object CollectionRoute

@Serializable
data object OthersRoute

@Serializable
data class VnDetailRoute(
    val vnId: String,
    val initialTitle: String = ""
)

@Serializable
data object SettingsRoute

@Serializable
data object AuthRoute

@Serializable
data object AboutRoute

enum class BottomNavTab(
    val route: Any,
    @StringRes val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(
        route = HomeRoute,
        labelResId = R.string.tab_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    SEARCH(
        route = SearchRoute,
        labelResId = R.string.tab_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_search"
    ),
    COLLECTION(
        route = CollectionRoute,
        labelResId = R.string.tab_collection,
        selectedIcon = Icons.Filled.CollectionsBookmark,
        unselectedIcon = Icons.Outlined.CollectionsBookmark,
        testTag = "nav_collection"
    ),
    OTHERS(
        route = OthersRoute,
        labelResId = R.string.tab_others,
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        testTag = "nav_others"
    )
}

/**
 * Cross-screen search navigation command dispatched when the user taps a Tag or Trait on
 * VN Detail, taps "See More" on a Home carousel, or opens the Character Trait Finder from Home (A5).
 */
sealed interface SearchNavCommand {
    data class FilterVnsByTag(val tag: Tag) : SearchNavCommand
    data class FilterVnsByTrait(val trait: Trait) : SearchNavCommand
    data class ApplyHomeSection(val section: HomeSection) : SearchNavCommand
    data class OpenCharacterExplorer(val presetTrait: Trait? = null) : SearchNavCommand
}

class SearchCommandBus {
    private val _pendingCommand = MutableStateFlow<SearchNavCommand?>(null)
    val pendingCommand: StateFlow<SearchNavCommand?> = _pendingCommand.asStateFlow()

    fun dispatch(command: SearchNavCommand) {
        _pendingCommand.value = command
    }

    fun consume(): SearchNavCommand? {
        val current = _pendingCommand.value
        _pendingCommand.value = null
        return current
    }
}
