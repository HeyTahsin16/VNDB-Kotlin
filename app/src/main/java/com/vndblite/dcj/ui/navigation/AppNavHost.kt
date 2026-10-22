package com.vndblite.dcj.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vndblite.dcj.R
import com.vndblite.dcj.di.AppContainer
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.ui.screens.about.AboutScreen
import com.vndblite.dcj.ui.screens.auth.AuthSyncScreen
import com.vndblite.dcj.ui.screens.auth.AuthSyncViewModel
import com.vndblite.dcj.ui.screens.characters.CharacterSearchScreen
import com.vndblite.dcj.ui.screens.characters.CharacterSearchViewModel
import com.vndblite.dcj.ui.screens.collection.CollectionScreen
import com.vndblite.dcj.ui.screens.collection.CollectionViewModel
import com.vndblite.dcj.ui.screens.detail.VnDetailNavigationCallbacks
import com.vndblite.dcj.ui.screens.detail.VnDetailScreen
import com.vndblite.dcj.ui.screens.detail.VnDetailViewModel
import com.vndblite.dcj.ui.screens.home.HomeNavigationCallbacks
import com.vndblite.dcj.ui.screens.home.HomeScreen
import com.vndblite.dcj.ui.screens.home.HomeViewModel
import com.vndblite.dcj.ui.screens.others.OthersScreen
import com.vndblite.dcj.ui.screens.random.RandomVnScreen
import com.vndblite.dcj.ui.screens.random.RandomVnViewModel
import com.vndblite.dcj.ui.screens.search.SearchScreen
import com.vndblite.dcj.ui.screens.search.SearchViewModel
import com.vndblite.dcj.ui.screens.settings.SettingsScreen
import com.vndblite.dcj.ui.screens.settings.SettingsViewModel
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec

private data class TopLevelDestination<T : Any>(
    val route: T,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val activeAccount by container.syncRepository
        .observeActiveAccount()
        .collectAsStateWithLifecycle(initialValue = null)

    val topLevelDestinations = listOf(
        TopLevelDestination(HomeRoute, R.string.tab_home, Icons.Default.Home, "nav_tab_home"),
        TopLevelDestination(SearchRoute(), R.string.tab_search, Icons.Default.Search, "nav_tab_search"),
        TopLevelDestination(CollectionRoute, R.string.tab_collection, Icons.Default.Bookmarks, "nav_tab_collection"),
        TopLevelDestination(OthersRoute, R.string.tab_others, Icons.Default.MoreHoriz, "nav_tab_others")
    )

    Scaffold(
        containerColor = ColorTokens.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = themeSpec.surfaceColor.copy(alpha = 0.94f),
                contentColor = themeSpec.contentColor
            ) {
                topLevelDestinations.forEach { topDest ->
                    val isSelected = when (topDest.route) {
                        is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                        is SearchRoute -> currentDest?.hasRoute<SearchRoute>() == true ||
                            currentDest?.hasRoute<CharacterSearchRoute>() == true
                        is CollectionRoute -> currentDest?.hasRoute<CollectionRoute>() == true
                        is OthersRoute -> currentDest?.hasRoute<OthersRoute>() == true ||
                            currentDest?.hasRoute<SettingsRoute>() == true ||
                            currentDest?.hasRoute<AuthSyncRoute>() == true ||
                            currentDest?.hasRoute<RandomVnRoute>() == true ||
                            currentDest?.hasRoute<AboutRoute>() == true
                        else -> false
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(topDest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(topDest.icon, contentDescription = stringResource(topDest.labelRes)) },
                        label = { Text(stringResource(topDest.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = themeSpec.onAccentColor,
                            selectedTextColor = themeSpec.accentColor,
                            indicatorColor = themeSpec.accentColor,
                            unselectedIconColor = themeSpec.contentColor.copy(alpha = 0.7f),
                            unselectedTextColor = themeSpec.contentColor.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag(topDest.testTag)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable<HomeRoute> {
                val vm: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(
                        vnRepository = container.vnRepository,
                        collectionRepository = container.collectionRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    navigation = HomeNavigationCallbacks(
                        onOpenVnDetail = { vnId -> navController.navigate(VnDetailRoute(vnId)) },
                        onSeeMoreSection = { section ->
                            if (section == HomeSection.COLLECTION) {
                                navController.navigate(CollectionRoute)
                            } else {
                                navController.navigate(
                                    SearchRoute(
                                        initialSortFieldCode = section.sortField.code,
                                        initialReverseSort = section.reverseSort,
                                        initialDevStatusCode = section.devStatusFilter?.apiCode,
                                        initialReleaseWindowKey = section.releaseWindow.storageKey
                                    )
                                )
                            }
                        },
                        onOpenGenrePreset = { preset ->
                            navController.navigate(
                                SearchRoute(
                                    initialTagId = preset.tagId,
                                    initialTagName = preset.defaultName
                                )
                            )
                        },
                        onOpenTraitPreset = { preset ->
                            navController.navigate(
                                CharacterSearchRoute(
                                    initialTraitId = preset.traitId,
                                    initialTraitName = preset.defaultName,
                                    initialTraitGroupName = preset.defaultGroupName
                                )
                            )
                        },
                        onOpenCharacterFinder = { navController.navigate(CharacterSearchRoute()) },
                        onOpenRandomVn = { navController.navigate(RandomVnRoute) }
                    )
                )
            }

            composable<SearchRoute> { entry ->
                val route = entry.toRoute<SearchRoute>()
                val vm: SearchViewModel = viewModel(
                    factory = SearchViewModel.factory(
                        vnRepository = container.vnRepository,
                        collectionRepository = container.collectionRepository,
                        settingsRepository = container.settingsRepository,
                        initialRoute = route
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                SearchScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    onOpenVnDetail = { vnId -> navController.navigate(VnDetailRoute(vnId)) },
                    onSwitchToCharacterExplorer = { navController.navigate(CharacterSearchRoute()) }
                )
            }

            composable<CharacterSearchRoute> { entry ->
                val route = entry.toRoute<CharacterSearchRoute>()
                val vm: CharacterSearchViewModel = viewModel(
                    factory = CharacterSearchViewModel.factory(
                        vnRepository = container.vnRepository,
                        settingsRepository = container.settingsRepository,
                        initialRoute = route
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                CharacterSearchScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    onOpenVnDetail = { vnId -> navController.navigate(VnDetailRoute(vnId)) },
                    onSwitchToVnSearch = { navController.navigate(SearchRoute()) }
                )
            }

            composable<CollectionRoute> {
                val vm: CollectionViewModel = viewModel(
                    factory = CollectionViewModel.factory(
                        collectionRepository = container.collectionRepository,
                        settingsRepository = container.settingsRepository,
                        syncRepository = container.syncRepository
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                CollectionScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    onOpenVnDetail = { vnId -> navController.navigate(VnDetailRoute(vnId)) },
                    onOpenAuthSync = { navController.navigate(AuthSyncRoute) }
                )
            }

            composable<VnDetailRoute> { entry ->
                val route = entry.toRoute<VnDetailRoute>()
                val vm: VnDetailViewModel = viewModel(
                    factory = VnDetailViewModel.factory(
                        vnId = route.vnId,
                        vnRepository = container.vnRepository,
                        collectionRepository = container.collectionRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                VnDetailScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    navigation = VnDetailNavigationCallbacks(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenRelatedVn = { relatedVnId -> navController.navigate(VnDetailRoute(relatedVnId)) },
                        onSearchByTag = { tag ->
                            navController.navigate(
                                SearchRoute(
                                    initialTagId = tag.id,
                                    initialTagName = tag.name
                                )
                            )
                        },
                        onSearchByTrait = { trait ->
                            navController.navigate(
                                CharacterSearchRoute(
                                    initialTraitId = trait.id,
                                    initialTraitName = trait.name,
                                    initialTraitGroupName = trait.groupName
                                )
                            )
                        }
                    )
                )
            }

            composable<OthersRoute> {
                OthersScreen(
                    activeAccount = activeAccount,
                    onOpenAuthSync = { navController.navigate(AuthSyncRoute) },
                    onOpenRandomVn = { navController.navigate(RandomVnRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) }
                )
            }

            composable<SettingsRoute> {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(
                        settingsRepository = container.settingsRepository,
                        vnRepository = container.vnRepository,
                        collectionRepository = container.collectionRepository
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<AuthSyncRoute> {
                val vm: AuthSyncViewModel = viewModel(
                    factory = AuthSyncViewModel.factory(container.syncRepository)
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                AuthSyncScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<RandomVnRoute> {
                val vm: RandomVnViewModel = viewModel(
                    factory = RandomVnViewModel.factory(
                        vnRepository = container.vnRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                RandomVnScreen(
                    uiState = uiState,
                    onAction = vm::onAction,
                    onOpenVnDetail = { vnId -> navController.navigate(VnDetailRoute(vnId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<AboutRoute> {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
