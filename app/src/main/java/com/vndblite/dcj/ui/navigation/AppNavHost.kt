package com.vndblite.dcj.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.format.isWideLayout
import com.vndblite.dcj.ui.screens.about.AboutScreen
import com.vndblite.dcj.ui.screens.auth.AuthScreen
import com.vndblite.dcj.ui.screens.collection.CollectionScreen
import com.vndblite.dcj.ui.screens.detail.VnDetailScreen
import com.vndblite.dcj.ui.screens.home.HomeScreen
import com.vndblite.dcj.ui.screens.others.OthersAction
import com.vndblite.dcj.ui.screens.others.OthersScreen
import com.vndblite.dcj.ui.screens.others.OthersUiState
import com.vndblite.dcj.ui.screens.random.RandomVnAction
import com.vndblite.dcj.ui.screens.random.RandomVnDialog
import com.vndblite.dcj.ui.screens.random.RandomVnViewModel
import com.vndblite.dcj.ui.screens.search.SearchScreen
import com.vndblite.dcj.ui.screens.settings.SettingsScreen
import com.vndblite.dcj.ui.theme.VndbThemeTokens

/**
 * Type-safe Navigation Compose shell with adaptive bottom bar on handheld portrait and
 * navigation rail on wide screens, hidden on sub-routes (A5, A9, B4, Section 7).
 */
@Composable
fun AppNavHost(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val container = LocalAppContainer.current
    val wide = isWideLayout()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val activeTab = currentDest.resolveBottomNavTab()
    val showPrimaryNav = activeTab != null

    val randomViewModel: RandomVnViewModel = viewModel(
        factory = RandomVnViewModel.factory(container)
    )
    val randomState by randomViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showPrimaryNav && !wide) {
                VndbBottomNavigationBar(
                    activeTab = activeTab,
                    onSelectTab = { tab -> navController.navigateToPrimaryTab(tab) }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showPrimaryNav && wide) {
                VndbSideNavigationRail(
                    activeTab = activeTab,
                    onSelectTab = { tab -> navController.navigateToPrimaryTab(tab) }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onOpenVnDetail = { vn -> navController.navigateToVnDetail(vn) },
                            onSeeMoreSection = { section ->
                                container.searchCommandBus.dispatch(
                                    SearchNavCommand.ApplyHomeSection(section)
                                )
                                navController.navigateToPrimaryTab(BottomNavTab.SEARCH)
                            },
                            onGoToCollectionTab = {
                                navController.navigateToPrimaryTab(BottomNavTab.COLLECTION)
                            },
                            onOpenTraitFinder = { trait ->
                                container.searchCommandBus.dispatch(
                                    SearchNavCommand.OpenCharacterExplorer(trait)
                                )
                                navController.navigateToPrimaryTab(BottomNavTab.SEARCH)
                            },
                            onOpenRandomVn = {
                                randomViewModel.onAction(RandomVnAction.OpenAndRoll)
                            }
                        )
                    }

                    composable<SearchRoute> {
                        SearchScreen(
                            onOpenVnDetail = { vn -> navController.navigateToVnDetail(vn) }
                        )
                    }

                    composable<CollectionRoute> {
                        CollectionScreen(
                            onOpenAuthScreen = { navController.navigate(AuthRoute) },
                            onOpenVnDetail = { vn -> navController.navigateToVnDetail(vn) }
                        )
                    }

                    composable<OthersRoute> {
                        val account by container.syncService.activeAccountFlow.collectAsStateWithLifecycle(initialValue = null)
                        val isSynced by container.syncService.isAccountSyncedFlow.collectAsStateWithLifecycle(initialValue = false)
                        OthersScreen(
                            state = OthersUiState(
                                activeAccount = account,
                                isAccountSynced = isSynced,
                                settings = settings
                            ),
                            onAction = { action ->
                                when (action) {
                                    OthersAction.OpenAuthSync -> navController.navigate(AuthRoute)
                                    OthersAction.OpenRandomVn ->
                                        randomViewModel.onAction(RandomVnAction.OpenAndRoll)
                                    OthersAction.OpenSettings -> navController.navigate(SettingsRoute)
                                    OthersAction.OpenAbout -> navController.navigate(AboutRoute)
                                }
                            }
                        )
                    }

                    composable<VnDetailRoute> { entry ->
                        val route = entry.toRoute<VnDetailRoute>()
                        VnDetailScreen(
                            vnId = route.vnId,
                            initialTitle = route.initialTitle,
                            onBack = { navController.popBackStack() },
                            onOpenRelatedVn = { vn -> navController.navigateToVnDetail(vn) },
                            onFilterByTag = { tag ->
                                container.searchCommandBus.dispatch(
                                    SearchNavCommand.FilterVnsByTag(tag)
                                )
                                navController.navigateToPrimaryTab(BottomNavTab.SEARCH)
                            },
                            onFilterByTrait = { trait ->
                                container.searchCommandBus.dispatch(
                                    SearchNavCommand.FilterVnsByTrait(trait)
                                )
                                navController.navigateToPrimaryTab(BottomNavTab.SEARCH)
                            }
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }

                    composable<AuthRoute> {
                        AuthScreen(onBack = { navController.popBackStack() })
                    }

                    composable<AboutRoute> {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    RandomVnDialog(
        state = randomState,
        onAction = randomViewModel::onAction,
        onOpenDetail = { vn ->
            randomViewModel.onAction(RandomVnAction.Dismiss)
            navController.navigateToVnDetail(vn)
        }
    )
}

private fun NavDestination?.resolveBottomNavTab(): BottomNavTab? {
    if (this == null) return null
    return when {
        hasRoute<HomeRoute>() -> BottomNavTab.HOME
        hasRoute<SearchRoute>() -> BottomNavTab.SEARCH
        hasRoute<CollectionRoute>() -> BottomNavTab.COLLECTION
        hasRoute<OthersRoute>() -> BottomNavTab.OTHERS
        else -> null
    }
}

private fun NavHostController.navigateToPrimaryTab(tab: BottomNavTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

private fun NavHostController.navigateToVnDetail(vn: VnSummary) {
    navigate(VnDetailRoute(vnId = vn.id, initialTitle = vn.title))
}

@Composable
private fun VndbBottomNavigationBar(
    activeTab: BottomNavTab?,
    onSelectTab: (BottomNavTab) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    NavigationBar(
        containerColor = spec.surfaceColor.copy(alpha = 0.94f),
        contentColor = spec.onSurfaceColor
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val label = stringResource(tab.labelResId)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = semantic.onAccentButton,
                    selectedTextColor = spec.accentColor,
                    indicatorColor = spec.accentColor,
                    unselectedIconColor = spec.onSurfaceColor.copy(alpha = 0.72f),
                    unselectedTextColor = spec.onSurfaceColor.copy(alpha = 0.72f)
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

@Composable
private fun VndbSideNavigationRail(
    activeTab: BottomNavTab?,
    onSelectTab: (BottomNavTab) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    NavigationRail(
        containerColor = spec.surfaceColor.copy(alpha = 0.94f),
        contentColor = spec.onSurfaceColor
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val label = stringResource(tab.labelResId)
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = semantic.onAccentButton,
                    selectedTextColor = spec.accentColor,
                    indicatorColor = spec.accentColor,
                    unselectedIconColor = spec.onSurfaceColor.copy(alpha = 0.72f),
                    unselectedTextColor = spec.onSurfaceColor.copy(alpha = 0.72f)
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
