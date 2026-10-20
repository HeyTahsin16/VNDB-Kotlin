package com.vndblite.dcj

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.data.model.VnDataPhase01
import com.vndblite.dcj.ui.screens.*
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import com.vndblite.dcj.ui.theme.VndbLiteTheme
import com.vndblite.dcj.ui.viewmodel.SearchTabMode
import com.vndblite.dcj.ui.viewmodel.VndbViewModel

enum class BottomNavSection(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    COLLECTION("Collection", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark, "nav_collection"),
    OTHERS("Others", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_others")
}

sealed interface SubRoute {
    data class VnDetail(val vnId: String) : SubRoute
    data object Settings : SubRoute
    data object AuthSync : SubRoute
    data object About : SubRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VndbLiteApp()
        }
    }
}

@Composable
fun VndbLiteApp(vm: VndbViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val searchState by vm.searchState.collectAsStateWithLifecycle()
    val collectionState by vm.collectionState.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val detailMap by vm.detailMap.collectAsStateWithLifecycle()
    val randomState by vm.randomState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(BottomNavSection.HOME) }
    var routeStack by remember { mutableStateOf<List<SubRoute>>(emptyList()) }
    var showRandomDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 600

    fun pushRoute(route: SubRoute) {
        routeStack = routeStack + route
    }

    fun popRoute() {
        if (routeStack.isNotEmpty()) {
            routeStack = routeStack.dropLast(1)
        } else if (currentTab != BottomNavSection.HOME) {
            currentTab = BottomNavSection.HOME
        }
    }

    fun openVnDetail(vn: VnDataPhase01) {
        vm.loadVnDetail(vn.id, vn)
        pushRoute(SubRoute.VnDetail(vn.id))
    }

    BackHandler(enabled = routeStack.isNotEmpty() || currentTab != BottomNavSection.HOME) {
        popRoute()
    }

    VndbLiteTheme(
        themeCode = settings.themeCode,
        fontSizeOffset = settings.fontSizeOffset
    ) {
        val theme = LocalAppThemeCode.current
        val activeSubRoute = routeStack.lastOrNull()

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (!isLandscape && activeSubRoute == null) {
                    NavigationBar(
                        containerColor = theme.primary.copy(alpha = 0.94f),
                        contentColor = theme.tertiary
                    ) {
                        BottomNavSection.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    routeStack = emptyList()
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = theme.secondary,
                                    indicatorColor = theme.secondary,
                                    unselectedIconColor = theme.tertiary.copy(alpha = 0.7f),
                                    unselectedTextColor = theme.tertiary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLandscape && activeSubRoute == null) {
                    NavigationRail(
                        containerColor = theme.primary.copy(alpha = 0.94f),
                        contentColor = theme.tertiary
                    ) {
                        BottomNavSection.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    routeStack = emptyList()
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = theme.secondary,
                                    indicatorColor = theme.secondary,
                                    unselectedIconColor = theme.tertiary.copy(alpha = 0.7f),
                                    unselectedTextColor = theme.tertiary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val sub = activeSubRoute) {
                        is SubRoute.VnDetail -> {
                            VnDetailScreen(
                                vnId = sub.vnId,
                                detailState = detailMap[sub.vnId],
                                settings = settings,
                                collectionRecord = collectionState.rawRecords[sub.vnId],
                                rawRecords = collectionState.rawRecords,
                                onBack = ::popRoute,
                                onSaveToCollection = { p1, status, vote, notes, started, finished ->
                                    vm.saveOrUpdateCollectionRecord(p1, status, vote, notes, started, finished)
                                },
                                onRemoveFromCollection = { id ->
                                    vm.removeRecordFromCollection(id)
                                },
                                onTagClick = { tag ->
                                    routeStack = emptyList()
                                    currentTab = BottomNavSection.SEARCH
                                    vm.applyQuickGenreSearch(tag)
                                },
                                onTraitClick = { trait ->
                                    routeStack = emptyList()
                                    currentTab = BottomNavSection.SEARCH
                                    vm.applyQuickTraitVnSearch(trait)
                                },
                                onRelatedVnClick = { relVn ->
                                    openVnDetail(relVn)
                                },
                                onExpandFullReview = { reviewId ->
                                    vm.expandFullReview(sub.vnId, reviewId)
                                }
                            )
                        }

                        SubRoute.Settings -> {
                            SettingsScreen(
                                settings = settings,
                                onBack = ::popRoute,
                                onUpdateSettings = { vm.updateSettings(it) },
                                onResetSettings = { vm.resetSettingsToDefault() },
                                onClearCollection = { keepSyncDeletions ->
                                    vm.clearEntireLocalCollection(keepSyncDeletions)
                                }
                            )
                        }

                        SubRoute.AuthSync -> {
                            AuthScreen(
                                syncState = syncState,
                                onBack = ::popRoute,
                                onSubmitToken = { token -> vm.authenticateAndSync(token) },
                                onSyncNow = { vm.syncCurrentAccount() },
                                onLogout = { vm.logoutVnAccount() }
                            )
                        }

                        SubRoute.About -> {
                            AboutScreen(onBack = ::popRoute)
                        }

                        null -> {
                            when (currentTab) {
                                BottomNavSection.HOME -> {
                                    HomeScreen(
                                        homeState = homeState,
                                        settings = settings,
                                        rawRecords = collectionState.rawRecords,
                                        onVnClick = ::openVnDetail,
                                        onSeeMoreSection = { section ->
                                            currentTab = BottomNavSection.SEARCH
                                            vm.applyHomeSeeMore(section)
                                        },
                                        onGoToCollectionTab = {
                                            currentTab = BottomNavSection.COLLECTION
                                        },
                                        onOpenTraitFinder = { presetTrait ->
                                            currentTab = BottomNavSection.SEARCH
                                            if (presetTrait != null) {
                                                vm.setSearchTabMode(SearchTabMode.CHARACTERS_AND_TRAITS)
                                                vm.clearExplorerTraits()
                                                vm.toggleExplorerTrait(presetTrait)
                                            } else {
                                                vm.setSearchTabMode(SearchTabMode.CHARACTERS_AND_TRAITS)
                                            }
                                        },
                                        onOpenRandomVn = {
                                            showRandomDialog = true
                                            vm.loadRandomVn()
                                        },
                                        onShuffleQuotes = { vm.shuffleHomeQuotes() },
                                        onRefresh = { vm.loadHomeData(forceRefresh = true) }
                                    )
                                }

                                BottomNavSection.SEARCH -> {
                                    SearchScreen(
                                        searchState = searchState,
                                        settings = settings,
                                        rawRecords = collectionState.rawRecords,
                                        isLandscape = isLandscape,
                                        onSwitchTabMode = { vm.setSearchTabMode(it) },
                                        onUpdateVnQuery = { vm.updateSearchQuery(it) },
                                        onTriggerVnSearch = { reset -> vm.performVnSearch(reset) },
                                        onQuickGenreClick = { tag -> vm.applyQuickGenreSearch(tag) },
                                        onQuickTraitVnSearch = { trait -> vm.applyQuickTraitVnSearch(trait) },
                                        onRemoveFilterChip = { tg, tr, dv, lg, ol, pf, ds, cr, cs, clrRel ->
                                            vm.removeSingleFilterChip(tg, tr, dv, lg, ol, pf, ds, cr, cs, clrRel)
                                        },
                                        onApplyFilterAndSort = { f, s ->
                                            vm.updateRemoteFilterAndSort(f, s, triggerSearch = true)
                                        },
                                        onResetFilters = { vm.resetRemoteFilters() },
                                        onLiveQueryTags = { vm.queryTagsLive(it) },
                                        onLiveQueryDevs = { vm.queryDevelopersLive(it) },
                                        onLiveQueryTraits = { vm.queryTraitsLive(it) },
                                        onUpdateCharQuery = { vm.updateCharSearchQuery(it) },
                                        onToggleExplorerTrait = { vm.toggleExplorerTrait(it) },
                                        onToggleCharTraitAndOr = { vm.toggleCharTraitAndOr() },
                                        onToggleExplorerRole = { vm.toggleExplorerRole(it) },
                                        onToggleCharRoleAndOr = { vm.toggleCharRoleAndOr() },
                                        onToggleExplorerSex = { vm.toggleExplorerSex(it) },
                                        onToggleCharSexAndOr = { vm.toggleCharSexAndOr() },
                                        onToggleCharOverallAndOr = { vm.toggleCharOverallAndOr() },
                                        onClearExplorerTraits = { vm.clearExplorerTraits() },
                                        onTriggerCharSearch = { reset -> vm.performCharacterSearch(reset) },
                                        onVnClick = ::openVnDetail,
                                        onVnIdClick = { id, title ->
                                            openVnDetail(VnDataPhase01(id = id, title = title))
                                        }
                                    )
                                }

                                BottomNavSection.COLLECTION -> {
                                    CollectionScreen(
                                        collectionState = collectionState,
                                        syncState = syncState,
                                        settings = settings,
                                        isLandscape = isLandscape,
                                        onToggleSearchActive = { vm.setCollectionSearchActive(it) },
                                        onUpdateSearchQuery = { vm.updateCollectionSearchQuery(it) },
                                        onApplyLocalFilterAndSort = { f, s -> vm.updateLocalFilterAndSort(f, s) },
                                        onToggleSelectVn = { vm.toggleRecordSelection(it) },
                                        onSelectAllInTab = { vm.selectAllInStatus(it) },
                                        onClearSelection = { vm.clearRecordSelection() },
                                        onSaveRecord = { vn, st, vt, nt, sr, fn ->
                                            vm.saveOrUpdateCollectionRecord(vn, st, vt, nt, sr, fn)
                                        },
                                        onRemoveRecord = { vm.removeRecordFromCollection(it) },
                                        onBatchUpdateSelected = { st, vt -> vm.batchUpdateSelectedRecords(st, vt) },
                                        onBatchDeleteSelected = { vm.removeSelectedRecordsFromCollection() },
                                        onOpenAuthScreen = { pushRoute(SubRoute.AuthSync) },
                                        onSyncNow = { vm.syncCurrentAccount() },
                                        onVnClick = ::openVnDetail
                                    )
                                }

                                BottomNavSection.OTHERS -> {
                                    OthersScreen(
                                        syncState = syncState,
                                        settings = settings,
                                        onOpenSettings = { pushRoute(SubRoute.Settings) },
                                        onOpenAuthSync = { pushRoute(SubRoute.AuthSync) },
                                        onOpenRandomVn = {
                                            showRandomDialog = true
                                            vm.loadRandomVn()
                                        },
                                        onOpenAbout = { pushRoute(SubRoute.About) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showRandomDialog) {
            RandomVnDialog(
                randomState = randomState,
                showCoverCensor = settings.showCoverCensor,
                rawRecords = collectionState.rawRecords,
                onDismiss = { showRandomDialog = false },
                onRollAgain = { vm.loadRandomVn() },
                onOpenDetail = { vn ->
                    showRandomDialog = false
                    openVnDetail(vn)
                }
            )
        }
    }
}
