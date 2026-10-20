package com.vndblite.dcj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.components.VnItemGridCard
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbInputChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import com.vndblite.dcj.ui.viewmodel.SearchTabMode
import com.vndblite.dcj.ui.viewmodel.SearchUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchState: SearchUiState,
    settings: SettingsState,
    rawRecords: Map<String, VnRecord>,
    isLandscape: Boolean,
    onSwitchTabMode: (SearchTabMode) -> Unit,
    onUpdateVnQuery: (String) -> Unit,
    onTriggerVnSearch: (Boolean) -> Unit,
    onQuickGenreClick: (VnTag) -> Unit,
    onQuickTraitVnSearch: (VnTrait) -> Unit,
    onRemoveFilterChip: (
        tag: VnTag?,
        trait: VnTrait?,
        dev: Developer?,
        lang: String?,
        olang: String?,
        platf: String?,
        devstatus: Int?,
        charRole: String?,
        charSex: String?,
        clearReleaseWindow: Boolean
    ) -> Unit,
    onApplyFilterAndSort: (FilterData, SortData) -> Unit,
    onResetFilters: () -> Unit,
    onLiveQueryTags: (String) -> Unit,
    onLiveQueryDevs: (String) -> Unit,
    onLiveQueryTraits: (String) -> Unit,
    onUpdateCharQuery: (String) -> Unit,
    onToggleExplorerTrait: (VnTrait) -> Unit,
    onToggleCharTraitAndOr: () -> Unit,
    onToggleExplorerRole: (String) -> Unit,
    onToggleCharRoleAndOr: () -> Unit,
    onToggleExplorerSex: (String) -> Unit,
    onToggleCharSexAndOr: () -> Unit,
    onToggleCharOverallAndOr: () -> Unit,
    onClearExplorerTraits: () -> Unit,
    onTriggerCharSearch: (Boolean) -> Unit,
    onVnClick: (VnDataPhase01) -> Unit,
    onVnIdClick: (String, String) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val focusManager = LocalFocusManager.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCharacterDetail by remember { mutableStateOf<VnCharacter?>(null) }

    val columns = if (isLandscape) settings.maxItemPerRowLandscape else settings.maxItemPerRowPortrait

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen_root")
    ) {
        // Top Segmented Switcher: Visual Novels vs Characters & Traits
        Surface(
            color = theme.primary.copy(alpha = 0.88f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.tertiary.copy(alpha = 0.1f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isVnTab = searchState.tabMode == SearchTabMode.VISUAL_NOVELS
                    Surface(
                        color = if (isVnTab) theme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchTabMode(SearchTabMode.VISUAL_NOVELS) }
                            .testTag("search_tab_vn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (isVnTab) Color.Black else theme.tertiary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Visual Novels",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isVnTab) Color.Black else theme.tertiary
                            )
                        }
                    }

                    Surface(
                        color = if (!isVnTab) theme.secondary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchTabMode(SearchTabMode.CHARACTERS_AND_TRAITS) }
                            .testTag("search_tab_characters_traits")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = if (!isVnTab) Color.Black else theme.tertiary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Characters & Traits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (!isVnTab) Color.Black else theme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (searchState.tabMode == SearchTabMode.VISUAL_NOVELS) {
                    // VN Search Bar + Search Action + Filter Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchState.filterData.search,
                            onValueChange = onUpdateVnQuery,
                            placeholder = {
                                Text(
                                    "Search visual novels...",
                                    color = theme.tertiary.copy(alpha = 0.75f)
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search VNs",
                                    tint = theme.secondary
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchState.filterData.search.isNotEmpty()) {
                                        IconButton(onClick = { onUpdateVnQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = theme.tertiary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            onTriggerVnSearch(true)
                                        },
                                        modifier = Modifier.testTag("vn_search_submit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Run search",
                                            tint = theme.secondary
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onTriggerVnSearch(true)
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = vndbOutlinedTextFieldColors(theme),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vn_search_input")
                        )

                        BadgedBox(
                            badge = {
                                if (searchState.filterData.hasActiveRemoteFilters) {
                                    Badge(containerColor = theme.secondary, contentColor = Color.Black) {
                                        Text("!", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            FilledIconButton(
                                onClick = { showFilterSheet = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = theme.secondary,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("open_remote_filter_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Sort and Filter VNs (including Character Traits)"
                                )
                            }
                        }
                    }

                    // Active filter chips bar (including Character Traits, Role, Sex, Tags, Developers, etc.)
                    if (searchState.filterData.hasActiveRemoteFilters) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (searchState.filterData.releaseWindow.isNotBlank()) {
                                item(key = "rel_win") {
                                    val label = if (searchState.filterData.releaseWindow.equals("upcoming", ignoreCase = true)) {
                                        "Release: Upcoming"
                                    } else {
                                        "Release: Latest"
                                    }
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveFilterChip(null, null, null, null, null, null, null, null, null, true) },
                                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                        },
                                        colors = vndbInputChipColors(theme)
                                    )
                                }
                            }
                            items(searchState.filterData.charRoles, key = { "crole_$it" }) { roleCode ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, null, null, null, null, null, roleCode, null, false) },
                                    label = { Text("Role: ${CharacterRoleData.getLabel(roleCode)}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.charSexes, key = { "csex_$it" }) { sexCode ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, null, null, null, null, null, null, sexCode, false) },
                                    label = { Text("Sex: ${CharacterSexData.getLabel(sexCode)}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.trait, key = { "tr_${it.id}" }) { tr ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, tr, null, null, null, null, null, null, null, false) },
                                    label = { Text("Trait: ${tr.displayLabel}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.tag, key = { "tg_${it.id}" }) { tg ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(tg, null, null, null, null, null, null, null, null, false) },
                                    label = { Text("Tag: ${tg.name}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.dev, key = { "dv_${it.id}" }) { dv ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, dv, null, null, null, null, null, null, false) },
                                    label = { Text("Dev: ${dv.name}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.lang, key = { "lg_$it" }) { lg ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, null, lg, null, null, null, null, null, false) },
                                    label = { Text("Lang: ${LangData.getFullLanguage(lg)}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.olang, key = { "ol_$it" }) { ol ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, null, null, ol, null, null, null, null, false) },
                                    label = { Text("Origin: ${LangData.getFullLanguage(ol)}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.platform, key = { "pf_$it" }) { pf ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, null, null, null, pf, null, null, null, false) },
                                    label = { Text("OS: ${PlatfData.DEFINED_CODES[pf] ?: pf}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                            items(searchState.filterData.devstatus, key = { "ds_$it" }) { ds ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveFilterChip(null, null, null, null, null, null, ds, null, null, false) },
                                    label = { Text("Status: ${DevStatusData.get(ds).title}", fontWeight = FontWeight.SemiBold) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                                    },
                                    colors = vndbInputChipColors(theme)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Body based on Tab Mode
        if (searchState.tabMode == SearchTabMode.VISUAL_NOVELS) {
            VnSearchBody(
                searchState = searchState,
                settings = settings,
                rawRecords = rawRecords,
                columns = columns,
                onQuickGenreClick = onQuickGenreClick,
                onQuickTraitVnSearch = onQuickTraitVnSearch,
                onTriggerVnSearch = onTriggerVnSearch,
                onVnClick = onVnClick
            )
        } else {
            CharacterTraitExplorerBody(
                searchState = searchState,
                settings = settings,
                onUpdateCharQuery = onUpdateCharQuery,
                onLiveQueryTraits = onLiveQueryTraits,
                onToggleExplorerTrait = onToggleExplorerTrait,
                onToggleCharTraitAndOr = onToggleCharTraitAndOr,
                onToggleExplorerRole = onToggleExplorerRole,
                onToggleCharRoleAndOr = onToggleCharRoleAndOr,
                onToggleExplorerSex = onToggleExplorerSex,
                onToggleCharSexAndOr = onToggleCharSexAndOr,
                onToggleCharOverallAndOr = onToggleCharOverallAndOr,
                onClearExplorerTraits = onClearExplorerTraits,
                onTriggerCharSearch = onTriggerCharSearch,
                onFilterVnsByTrait = onQuickTraitVnSearch,
                onSelectCharacter = { selectedCharacterDetail = it }
            )
        }
    }

    // Remote Sort & Filter Bottom Sheet (includes Character Traits Search!)
    if (showFilterSheet) {
        RemoteSortFilterSheet(
            initialFilter = searchState.filterData,
            initialSort = searchState.sortData,
            tagSuggestions = searchState.tagSearchResults,
            isSearchingTags = searchState.isSearchingTags,
            devSuggestions = searchState.devSearchResults,
            isSearchingDevs = searchState.isSearchingDevs,
            traitSuggestions = searchState.traitSearchResults,
            isSearchingTraits = searchState.isSearchingTraits,
            onQueryTags = onLiveQueryTags,
            onQueryDevs = onLiveQueryDevs,
            onQueryTraits = onLiveQueryTraits,
            onDismiss = { showFilterSheet = false },
            onReset = {
                onResetFilters()
                showFilterSheet = false
            },
            onApply = { filter, sort ->
                showFilterSheet = false
                onApplyFilterAndSort(filter, sort)
            }
        )
    }

    // Character Detail Modal (shows full character bio, traits, and linked Visual Novels)
    selectedCharacterDetail?.let { char ->
        CharacterDetailDialog(
            character = char,
            showCoverCensor = settings.showCoverCensor,
            onDismiss = { selectedCharacterDetail = null },
            onFilterVnsByTrait = { trait ->
                selectedCharacterDetail = null
                onQuickTraitVnSearch(trait)
            },
            onFilterCharsByTrait = { trait ->
                selectedCharacterDetail = null
                onToggleExplorerTrait(trait)
            },
            onOpenVn = { vnId, vnTitle ->
                selectedCharacterDetail = null
                onVnIdClick(vnId, vnTitle)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VnSearchBody(
    searchState: SearchUiState,
    settings: SettingsState,
    rawRecords: Map<String, VnRecord>,
    columns: Int,
    onQuickGenreClick: (VnTag) -> Unit,
    onQuickTraitVnSearch: (VnTrait) -> Unit,
    onTriggerVnSearch: (Boolean) -> Unit,
    onVnClick: (VnDataPhase01) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val gridState = rememberLazyGridState()

    // Trigger pagination when scrolled near end
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && searchState.vnHasMore && !searchState.isVnLoading) {
            onTriggerVnSearch(false)
        }
    }

    if (!searchState.hasInitiatedVnSearch) {
        // Predefined Genres + Popular Character Trait VN Filters
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Search VNs by Character Traits",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any character trait to find visual novels featuring characters with that trait, or open the Filter icon above to search 3,200+ VNDB traits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PopularCharacterTraits.presets.forEach { trait ->
                            AssistChip(
                                onClick = { onQuickTraitVnSearch(trait) },
                                label = { Text("${trait.name} (${trait.groupName})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = theme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = theme.primary.copy(alpha = 0.85f),
                                    labelColor = theme.tertiary
                                ),
                                modifier = Modifier.testTag("vn_quick_trait_${trait.id}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Browse by Popular Genres",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(PredefinedHomeSearch.entries, key = { it.name }) { genre ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { onQuickGenreClick(genre.tag) }
                        .testTag("genre_card_${genre.tag.name.lowercase()}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AssetImage(
                            assetPath = genre.assetPath,
                            contentDescription = genre.tag.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.78f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = genre.tag.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Search Results Grid
        val labelCode = remember(searchState.sortData.sort) {
            SortableCode.entries.firstOrNull { it.code == searchState.sortData.sort } ?: SortableCode.RATING
        }

        if (searchState.vnResults.isEmpty() && searchState.isVnLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.secondary)
            }
        } else if (searchState.vnResults.isEmpty() && searchState.vnError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = searchState.vnError,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFF8A80)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onTriggerVnSearch(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = Color.Black)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else if (searchState.vnResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No visual novels matched your search filters.",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.tertiary.copy(alpha = 0.8f)
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns.coerceAtLeast(2)),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vn_search_results_grid")
            ) {
                items(searchState.vnResults, key = { it.id }) { vn ->
                    VnItemGridCard(
                        vn = vn,
                        labelCode = labelCode,
                        showCoverCensorGlobal = settings.showCoverCensor,
                        collectionRecord = rawRecords[vn.id],
                        onClick = { onVnClick(vn) }
                    )
                }

                if (searchState.isVnLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = theme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterTraitExplorerBody(
    searchState: SearchUiState,
    settings: SettingsState,
    onUpdateCharQuery: (String) -> Unit,
    onLiveQueryTraits: (String) -> Unit,
    onToggleExplorerTrait: (VnTrait) -> Unit,
    onToggleCharTraitAndOr: () -> Unit,
    onToggleExplorerRole: (String) -> Unit,
    onToggleCharRoleAndOr: () -> Unit,
    onToggleExplorerSex: (String) -> Unit,
    onToggleCharSexAndOr: () -> Unit,
    onToggleCharOverallAndOr: () -> Unit,
    onClearExplorerTraits: () -> Unit,
    onTriggerCharSearch: (Boolean) -> Unit,
    onFilterVnsByTrait: (VnTrait) -> Unit,
    onSelectCharacter: (VnCharacter) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val focusManager = LocalFocusManager.current
    var traitLookupInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("character_trait_explorer_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input Card for Characters + Role + Sex + Live Trait Picker
        item {
            Surface(
                color = theme.primary.copy(alpha = 0.86f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Character & Trait Search",
                                style = MaterialTheme.typography.titleLarge,
                                color = theme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Filter characters by Name, Role, Sex, and Traits with AND / OR logic.",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.tertiary.copy(alpha = 0.82f)
                            )
                        }
                        FilterChip(
                            selected = true,
                            onClick = onToggleCharOverallAndOr,
                            label = {
                                Text(
                                    "Combine: ${searchState.charOverallAndOr.uppercase()}",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(true, theme),
                            modifier = Modifier.testTag("char_overall_and_or_chip")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Character Name Input
                    OutlinedTextField(
                        value = searchState.charQuery,
                        onValueChange = onUpdateCharQuery,
                        label = { Text("Character Name (optional)") },
                        placeholder = { Text("e.g., Saber, Kurisu, Beatrice...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = theme.secondary)
                        },
                        trailingIcon = {
                            if (searchState.charQuery.isNotEmpty()) {
                                IconButton(onClick = { onUpdateCharQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = theme.tertiary)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onTriggerCharSearch(true)
                            }
                        ),
                        colors = vndbOutlinedTextFieldColors(theme),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("char_name_search_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Role Filter (with AND / OR toggle, matching original VNDB) ---
                    Surface(
                        color = theme.tertiary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            theme.secondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Role",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = theme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isRoleAnd = searchState.charRoleAndOr == "and"
                                    FilterChip(
                                        selected = isRoleAnd,
                                        onClick = { if (!isRoleAnd) onToggleCharRoleAndOr() },
                                        label = {
                                            Text(
                                                "AND",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isRoleAnd) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(isRoleAnd, theme),
                                        modifier = Modifier.testTag("char_role_and_chip")
                                    )
                                    val isRoleOr = searchState.charRoleAndOr == "or"
                                    FilterChip(
                                        selected = isRoleOr,
                                        onClick = { if (!isRoleOr) onToggleCharRoleAndOr() },
                                        label = {
                                            Text(
                                                "OR",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isRoleOr) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(isRoleOr, theme),
                                        modifier = Modifier.testTag("char_role_or_chip")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CharacterRoleData.DEFINED_CODES.forEach { (code, label) ->
                                    val chosen = searchState.selectedCharRoles.contains(code)
                                    FilterChip(
                                        selected = chosen,
                                        onClick = { onToggleExplorerRole(code) },
                                        label = {
                                            Text(
                                                label,
                                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = if (chosen) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        } else null,
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(chosen, theme),
                                        modifier = Modifier.testTag("explorer_role_$code")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Sex Filter (with AND / OR toggle, matching original VNDB) ---
                    Surface(
                        color = theme.tertiary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            theme.secondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Sex",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = theme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isSexAnd = searchState.charSexAndOr == "and"
                                    FilterChip(
                                        selected = isSexAnd,
                                        onClick = { if (!isSexAnd) onToggleCharSexAndOr() },
                                        label = {
                                            Text(
                                                "AND",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSexAnd) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(isSexAnd, theme),
                                        modifier = Modifier.testTag("char_sex_and_chip")
                                    )
                                    val isSexOr = searchState.charSexAndOr == "or"
                                    FilterChip(
                                        selected = isSexOr,
                                        onClick = { if (!isSexOr) onToggleCharSexAndOr() },
                                        label = {
                                            Text(
                                                "OR",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSexOr) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(isSexOr, theme),
                                        modifier = Modifier.testTag("char_sex_or_chip")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CharacterSexData.DEFINED_CODES.forEach { (code, label) ->
                                    val chosen = searchState.selectedCharSexes.contains(code)
                                    FilterChip(
                                        selected = chosen,
                                        onClick = { onToggleExplorerSex(code) },
                                        label = {
                                            Text(
                                                label,
                                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = if (chosen) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        } else null,
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(chosen, theme),
                                        modifier = Modifier.testTag("explorer_sex_$code")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Trait Lookup Input
                    OutlinedTextField(
                        value = traitLookupInput,
                        onValueChange = {
                            traitLookupInput = it
                            onLiveQueryTraits(it)
                        },
                        label = { Text("Search VNDB Character Traits (/kana/trait)") },
                        placeholder = { Text("Type trait e.g. Tsundere, Twin tails, Maid, Vampire...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = theme.secondary)
                        },
                        trailingIcon = {
                            if (searchState.isSearchingTraits) {
                                CircularProgressIndicator(
                                    color = theme.secondary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (traitLookupInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        traitLookupInput = ""
                                        onLiveQueryTraits("")
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = theme.tertiary)
                                }
                            }
                        },
                        colors = vndbOutlinedTextFieldColors(theme),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trait_live_search_input")
                    )

                    // Live Trait Autocomplete Suggestions
                    if (searchState.traitSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Matching Traits (tap to add to character search or filter VNs):",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchState.traitSearchResults, key = { it.id }) { trait ->
                                val isSelected = searchState.selectedCharTraits.any { it.id == trait.id }
                                Surface(
                                    color = if (isSelected) theme.secondary.copy(alpha = 0.3f) else theme.tertiary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) theme.secondary else theme.tertiary.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.clickable { onToggleExplorerTrait(trait) }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = trait.displayLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.tertiary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "${trait.charCount} chars",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = theme.tertiary.copy(alpha = 0.85f)
                                            )
                                            Text(
                                                text = "Filter VNs →",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = theme.secondary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { onFilterVnsByTrait(trait) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Character Traits / Roles / Sex Bar + Trait AND/OR toggle
                    val hasAnyCharSelections = searchState.selectedCharTraits.isNotEmpty() ||
                        searchState.selectedCharRoles.isNotEmpty() ||
                        searchState.selectedCharSexes.isNotEmpty()

                    if (hasAnyCharSelections) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Traits Match Mode (${searchState.selectedCharTraits.size} traits):",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.secondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isTraitAnd = searchState.charTraitAndOr == "and"
                                FilterChip(
                                    selected = isTraitAnd,
                                    onClick = { if (!isTraitAnd) onToggleCharTraitAndOr() },
                                    label = {
                                        Text(
                                            "Traits: AND",
                                            fontWeight = if (isTraitAnd) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = vndbFilterChipColors(theme),
                                    border = vndbFilterChipBorder(isTraitAnd, theme)
                                )
                                val isTraitOr = searchState.charTraitAndOr == "or"
                                FilterChip(
                                    selected = isTraitOr,
                                    onClick = { if (!isTraitOr) onToggleCharTraitAndOr() },
                                    label = {
                                        Text(
                                            "Traits: OR",
                                            fontWeight = if (isTraitOr) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = vndbFilterChipColors(theme),
                                    border = vndbFilterChipBorder(isTraitOr, theme)
                                )
                                TextButton(onClick = onClearExplorerTraits) {
                                    Text("Clear All", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (searchState.selectedCharTraits.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                searchState.selectedCharTraits.forEach { tr ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onToggleExplorerTrait(tr) },
                                        label = { Text(tr.displayLabel, fontWeight = FontWeight.SemiBold) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                        },
                                        colors = vndbInputChipColors(theme)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Popular Trait Presets
                    Text(
                        text = "Popular Trait Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.tertiary.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PopularCharacterTraits.presets, key = { it.id }) { preset ->
                            val chosen = searchState.selectedCharTraits.any { it.id == preset.id }
                            FilterChip(
                                selected = chosen,
                                onClick = { onToggleExplorerTrait(preset) },
                                label = {
                                    Text(
                                        preset.displayLabel,
                                        fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(chosen, theme),
                                modifier = Modifier.testTag("explorer_preset_trait_${preset.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onTriggerCharSearch(true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_characters_submit_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Characters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Character Results
        if (searchState.isCharLoading && searchState.charResults.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = theme.secondary)
                }
            }
        } else if (searchState.charError != null && searchState.charResults.isEmpty()) {
            item {
                Surface(
                    color = theme.primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(searchState.charError, color = Color(0xFFFF8A80))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onTriggerCharSearch(true) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else if (searchState.hasInitiatedCharSearch && searchState.charResults.isEmpty()) {
            item {
                Surface(
                    color = theme.primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No characters found matching those traits.",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.tertiary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(searchState.charResults, key = { it.id }) { character ->
                CharacterResultCard(
                    character = character,
                    showCoverCensor = settings.showCoverCensor,
                    onClick = { onSelectCharacter(character) },
                    onTraitClick = onToggleExplorerTrait,
                    onFilterVnByTrait = onFilterVnsByTrait
                )
            }

            if (searchState.charHasMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchState.isCharLoading) {
                            CircularProgressIndicator(color = theme.secondary, modifier = Modifier.size(28.dp))
                        } else {
                            OutlinedButton(
                                onClick = { onTriggerCharSearch(false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary)
                            ) {
                                Text("Load More Characters")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterResultCard(
    character: VnCharacter,
    showCoverCensor: Boolean,
    onClick: () -> Unit,
    onTraitClick: (VnTrait) -> Unit,
    onFilterVnByTrait: (VnTrait) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val isExplicit = character.image?.isExplicit == true
    var revealPortrait by remember(character.id) { mutableStateOf(false) }
    val shouldBlur = isExplicit && showCoverCensor && !revealPortrait

    Surface(
        color = theme.primary.copy(alpha = 0.86f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("character_card_${character.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Character Portrait
            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(118.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.tertiary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val imgUrl = character.image?.displayUrl.orEmpty()
                if (imgUrl.isNotBlank()) {
                    NsfwAsyncImage(
                        imageUrl = imgUrl,
                        contentDescription = character.name,
                        shouldBlur = shouldBlur,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isExplicit && showCoverCensor) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(6.dp),
                            onClick = { revealPortrait = !revealPortrait },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = if (shouldBlur) "18+" else "Hide",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = theme.tertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Character Info & Trait Chips
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (character.original.isNotBlank()) {
                            Text(
                                text = character.original,
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.tertiary.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (character.RoleSummary.isNotBlank()) {
                        Surface(
                            color = theme.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = character.RoleSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                val statsLine = character.formattedStats
                if (statsLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statsLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.tertiary.copy(alpha = 0.85f)
                    )
                }

                // Appears in VNs
                if (character.vns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Appears in: " + character.vns.take(2).joinToString(", ") { it.title },
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Top non-spoiler traits preview
                val nonSpoilerTraits = remember(character.traits) {
                    character.traits.filter { it.spoiler == 0 }.take(6)
                }
                if (nonSpoilerTraits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        nonSpoilerTraits.forEach { tr ->
                            Surface(
                                color = theme.tertiary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { onTraitClick(tr) }
                            ) {
                                Text(
                                    text = tr.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.tertiary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterDetailDialog(
    character: VnCharacter,
    showCoverCensor: Boolean,
    onDismiss: () -> Unit,
    onFilterVnsByTrait: (VnTrait) -> Unit,
    onFilterCharsByTrait: (VnTrait) -> Unit,
    onOpenVn: (vnId: String, vnTitle: String) -> Unit
) {
    val theme = LocalAppThemeCode.current
    var showSpoilerTraits by remember { mutableStateOf(false) }

    val visibleTraits = remember(character.traits, showSpoilerTraits) {
        if (showSpoilerTraits) character.traits else character.traits.filter { it.spoiler == 0 }
    }
    val groupedTraits = remember(visibleTraits) {
        visibleTraits.groupBy { it.groupName.ifBlank { "General" } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.primary,
        titleContentColor = theme.tertiary,
        textContentColor = theme.tertiary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val imgUrl = character.image?.displayUrl.orEmpty()
                if (imgUrl.isNotBlank()) {
                    val isCharExplicit = character.image?.isExplicit == true && showCoverCensor
                    var revealModalImg by remember(character.id) { mutableStateOf(false) }
                    NsfwAsyncImage(
                        imageUrl = imgUrl,
                        contentDescription = character.name,
                        shouldBlur = isCharExplicit && !revealModalImg,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = isCharExplicit) { revealModalImg = !revealModalImg }
                    )
                }
                Column {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (character.original.isNotBlank()) {
                        Text(
                            text = character.original,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary.copy(alpha = 0.8f)
                        )
                    }
                    if (character.formattedStats.isNotBlank()) {
                        Text(
                            text = character.formattedStats,
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.tertiary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (character.description.isNotBlank()) {
                    Text(
                        text = "Biography",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    BbCodeText(
                        rawText = character.description,
                        textColor = theme.tertiary,
                        linkColor = theme.secondary
                    )
                }

                // Visual Novels this character appears in
                if (character.vns.isNotEmpty()) {
                    Text(
                        text = "Visual Novels (${character.vns.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    character.vns.forEach { vnLink ->
                        Surface(
                            color = theme.tertiary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVn(vnLink.id, vnLink.title) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vnLink.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = theme.tertiary
                                    )
                                    Text(
                                        text = vnLink.roleDisplay,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.secondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open VN",
                                    tint = theme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Grouped Character Traits
                if (character.traits.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Character Traits (${visibleTraits.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showSpoilerTraits = !showSpoilerTraits }) {
                            Text(
                                text = if (showSpoilerTraits) "Hide Spoilers" else "Show Spoilers",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.secondary
                            )
                        }
                    }
                    Text(
                        text = "Tap any trait to find Visual Novels featuring characters with that trait:",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.tertiary.copy(alpha = 0.7f)
                    )

                    groupedTraits.forEach { (group, traitsInGroup) ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.secondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            traitsInGroup.forEach { tr ->
                                AssistChip(
                                    onClick = { onFilterVnsByTrait(tr) },
                                    label = { Text(tr.name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = theme.tertiary.copy(alpha = 0.1f),
                                        labelColor = theme.tertiary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = theme.secondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemoteSortFilterSheet(
    initialFilter: FilterData,
    initialSort: SortData,
    tagSuggestions: List<VnTag>,
    isSearchingTags: Boolean,
    devSuggestions: List<Developer>,
    isSearchingDevs: Boolean,
    traitSuggestions: List<VnTrait>,
    isSearchingTraits: Boolean,
    onQueryTags: (String) -> Unit,
    onQueryDevs: (String) -> Unit,
    onQueryTraits: (String) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (FilterData, SortData) -> Unit
) {
    val theme = LocalAppThemeCode.current
    var draftFilter by remember { mutableStateOf(initialFilter) }
    var draftSort by remember { mutableStateOf(initialSort) }

    var traitInput by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var devInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.primary,
        contentColor = theme.tertiary,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = theme.tertiary.copy(alpha = 0.55f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(horizontal = 16.dp)
        ) {
            // Sheet Header with Reset and Apply
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sort & Filter VNs",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onReset) {
                        Text("Reset", color = Color(0xFFFF8A80), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onApply(draftFilter, draftSort) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("apply_remote_filter_button")
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(
                color = theme.tertiary.copy(alpha = 0.25f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Sort Section
                Text(
                    text = "Sort Results By",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortableCode.remoteSortables.forEach { sortOption ->
                        val isChosen = draftSort.sort == sortOption.code
                        FilterChip(
                            selected = isChosen,
                            onClick = { draftSort = draftSort.copy(sort = sortOption.code) },
                            label = {
                                Text(
                                    sortOption.title,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (isChosen) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isChosen, theme)
                        )
                    }
                    FilterChip(
                        selected = draftSort.reverse,
                        onClick = { draftSort = draftSort.copy(reverse = !draftSort.reverse) },
                        label = {
                            Text(
                                if (draftSort.reverse) "Order: Descending ↓" else "Order: Ascending ↑",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = vndbFilterChipColors(theme),
                        border = vndbFilterChipBorder(draftSort.reverse, theme)
                    )
                }

                // 2. Filter Match Mode (AND / OR)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Filter Combination Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isAnd = draftFilter.andOr == "and"
                        FilterChip(
                            selected = isAnd,
                            onClick = { draftFilter = draftFilter.copy(andOr = "and") },
                            label = {
                                Text(
                                    "AND (All)",
                                    fontWeight = if (isAnd) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isAnd, theme)
                        )
                        val isOr = draftFilter.andOr == "or"
                        FilterChip(
                            selected = isOr,
                            onClick = { draftFilter = draftFilter.copy(andOr = "or") },
                            label = {
                                Text(
                                    "OR (Any)",
                                    fontWeight = if (isOr) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isOr, theme)
                        )
                    }
                }

                // 3. Character Traits, Role & Sex Filter Section (/kana/trait & character role/sex)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Character Traits (VNDB /kana/trait)",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isTrAnd = draftFilter.traitAndOr == "and"
                        FilterChip(
                            selected = isTrAnd,
                            onClick = { draftFilter = draftFilter.copy(traitAndOr = "and") },
                            label = {
                                Text(
                                    "AND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isTrAnd) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isTrAnd, theme)
                        )
                        val isTrOr = draftFilter.traitAndOr == "or"
                        FilterChip(
                            selected = isTrOr,
                            onClick = { draftFilter = draftFilter.copy(traitAndOr = "or") },
                            label = {
                                Text(
                                    "OR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isTrOr) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isTrOr, theme)
                        )
                    }
                }
                OutlinedTextField(
                    value = traitInput,
                    onValueChange = {
                        traitInput = it
                        onQueryTraits(it)
                    },
                    label = { Text("Search Character Traits") },
                    placeholder = { Text("e.g. Tsundere, Glasses, Blonde, Maid...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = theme.secondary
                        )
                    },
                    trailingIcon = {
                        if (isSearchingTraits) {
                            CircularProgressIndicator(
                                color = theme.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (traitInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    traitInput = ""
                                    onQueryTraits("")
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = theme.tertiary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = vndbOutlinedTextFieldColors(theme),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filter_sheet_trait_input")
                )

                // Quick popular traits when user hasn't typed a custom query yet
                if (traitInput.isBlank() && traitSuggestions.isEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PopularCharacterTraits.presets, key = { "pop_${it.id}" }) { preset ->
                            val added = draftFilter.trait.any { it.id == preset.id }
                            FilterChip(
                                selected = added,
                                onClick = {
                                    val updated = if (added) {
                                        draftFilter.trait.filterNot { it.id == preset.id }
                                    } else {
                                        draftFilter.trait + preset
                                    }
                                    draftFilter = draftFilter.copy(trait = updated)
                                },
                                label = {
                                    Text(
                                        preset.displayLabel,
                                        fontWeight = if (added) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(added, theme)
                            )
                        }
                    }
                }

                if (traitSuggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(traitSuggestions, key = { it.id }) { sug ->
                            val added = draftFilter.trait.any { it.id == sug.id }
                            FilterChip(
                                selected = added,
                                onClick = {
                                    val updated = if (added) {
                                        draftFilter.trait.filterNot { it.id == sug.id }
                                    } else {
                                        draftFilter.trait + sug
                                    }
                                    draftFilter = draftFilter.copy(trait = updated)
                                },
                                label = {
                                    Text(
                                        "${sug.displayLabel} (${sug.charCount})",
                                        fontWeight = if (added) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(added, theme)
                            )
                        }
                    }
                }

                // Selected traits in draft
                if (draftFilter.trait.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        draftFilter.trait.forEach { tr ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    draftFilter = draftFilter.copy(
                                        trait = draftFilter.trait.filterNot { it.id == tr.id }
                                    )
                                },
                                label = { Text(tr.displayLabel, fontWeight = FontWeight.SemiBold) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = vndbInputChipColors(theme)
                            )
                        }
                    }
                }

                // Character Role & Sex inside VN Filter Sheet (with AND/OR)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Character Role",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isRoleAnd = draftFilter.charRoleAndOr == "and"
                        FilterChip(
                            selected = isRoleAnd,
                            onClick = { draftFilter = draftFilter.copy(charRoleAndOr = "and") },
                            label = {
                                Text(
                                    "AND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isRoleAnd) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isRoleAnd, theme)
                        )
                        val isRoleOr = draftFilter.charRoleAndOr == "or"
                        FilterChip(
                            selected = isRoleOr,
                            onClick = { draftFilter = draftFilter.copy(charRoleAndOr = "or") },
                            label = {
                                Text(
                                    "OR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isRoleOr) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isRoleOr, theme)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CharacterRoleData.DEFINED_CODES.forEach { (code, label) ->
                        val chosen = draftFilter.charRoles.contains(code)
                        FilterChip(
                            selected = chosen,
                            onClick = {
                                val next = if (chosen) draftFilter.charRoles - code else draftFilter.charRoles + code
                                draftFilter = draftFilter.copy(charRoles = next)
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (chosen) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(chosen, theme)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Character Sex",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isSexAnd = draftFilter.charSexAndOr == "and"
                        FilterChip(
                            selected = isSexAnd,
                            onClick = { draftFilter = draftFilter.copy(charSexAndOr = "and") },
                            label = {
                                Text(
                                    "AND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSexAnd) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isSexAnd, theme)
                        )
                        val isSexOr = draftFilter.charSexAndOr == "or"
                        FilterChip(
                            selected = isSexOr,
                            onClick = { draftFilter = draftFilter.copy(charSexAndOr = "or") },
                            label = {
                                Text(
                                    "OR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSexOr) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(isSexOr, theme)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CharacterSexData.DEFINED_CODES.forEach { (code, label) ->
                        val chosen = draftFilter.charSexes.contains(code)
                        FilterChip(
                            selected = chosen,
                            onClick = {
                                val next = if (chosen) draftFilter.charSexes - code else draftFilter.charSexes + code
                                draftFilter = draftFilter.copy(charSexes = next)
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (chosen) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(chosen, theme)
                        )
                    }
                }

                // 4. Tags Filter Section (/kana/tag)
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = {
                        tagInput = it
                        onQueryTags(it)
                    },
                    label = { Text("Search VN Tags") },
                    placeholder = { Text("e.g. Romance, Time Travel, Mystery...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            tint = theme.secondary
                        )
                    },
                    trailingIcon = {
                        if (isSearchingTags) {
                            CircularProgressIndicator(
                                color = theme.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (tagInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    tagInput = ""
                                    onQueryTags("")
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = theme.tertiary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = vndbOutlinedTextFieldColors(theme),
                    modifier = Modifier.fillMaxWidth()
                )

                if (tagSuggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tagSuggestions, key = { it.id }) { sug ->
                            val added = draftFilter.tag.any { it.id == sug.id }
                            FilterChip(
                                selected = added,
                                onClick = {
                                    val updated = if (added) {
                                        draftFilter.tag.filterNot { it.id == sug.id }
                                    } else {
                                        draftFilter.tag + sug
                                    }
                                    draftFilter = draftFilter.copy(tag = updated)
                                },
                                label = {
                                    Text(
                                        sug.name,
                                        fontWeight = if (added) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(added, theme)
                            )
                        }
                    }
                }

                if (draftFilter.tag.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        draftFilter.tag.forEach { tg ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    draftFilter = draftFilter.copy(
                                        tag = draftFilter.tag.filterNot { it.id == tg.id }
                                    )
                                },
                                label = { Text(tg.name, fontWeight = FontWeight.SemiBold) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = vndbInputChipColors(theme)
                            )
                        }
                    }
                }

                // 5. Developers Filter Section (/kana/producer)
                Text(
                    text = "Developers / Producers",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = devInput,
                    onValueChange = {
                        devInput = it
                        onQueryDevs(it)
                    },
                    label = { Text("Search Developers") },
                    placeholder = { Text("e.g. Type-Moon, Key, Nitroplus...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = theme.secondary
                        )
                    },
                    trailingIcon = {
                        if (isSearchingDevs) {
                            CircularProgressIndicator(
                                color = theme.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (devInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    devInput = ""
                                    onQueryDevs("")
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = theme.tertiary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = vndbOutlinedTextFieldColors(theme),
                    modifier = Modifier.fillMaxWidth()
                )

                if (devSuggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(devSuggestions, key = { it.id }) { sug ->
                            val added = draftFilter.dev.any { it.id == sug.id }
                            FilterChip(
                                selected = added,
                                onClick = {
                                    val updated = if (added) {
                                        draftFilter.dev.filterNot { it.id == sug.id }
                                    } else {
                                        draftFilter.dev + sug
                                    }
                                    draftFilter = draftFilter.copy(dev = updated)
                                },
                                label = {
                                    Text(
                                        sug.name,
                                        fontWeight = if (added) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = vndbFilterChipColors(theme),
                                border = vndbFilterChipBorder(added, theme)
                            )
                        }
                    }
                }

                if (draftFilter.dev.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        draftFilter.dev.forEach { dv ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    draftFilter = draftFilter.copy(
                                        dev = draftFilter.dev.filterNot { it.id == dv.id }
                                    )
                                },
                                label = { Text(dv.name, fontWeight = FontWeight.SemiBold) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = vndbInputChipColors(theme)
                            )
                        }
                    }
                }

                // 6. Development Status
                Text(
                    text = "Development Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 1, 2).forEach { code ->
                        val info = DevStatusData.get(code)
                        val selected = draftFilter.devstatus.contains(code)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draftFilter.devstatus - code else draftFilter.devstatus + code
                                draftFilter = draftFilter.copy(devstatus = next)
                            },
                            label = {
                                Text(
                                    info.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(selected, theme)
                        )
                    }
                }

                // 7. Available Languages
                Text(
                    text = "Available Languages",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LangData.DEFINED_CODES.forEach { (code, label) ->
                        val selected = draftFilter.lang.contains(code)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draftFilter.lang - code else draftFilter.lang + code
                                draftFilter = draftFilter.copy(lang = next)
                            },
                            leadingIcon = {
                                AssetImage(
                                    assetPath = LangData.getFlagPath(code),
                                    contentDescription = label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(selected, theme)
                        )
                    }
                }

                // 8. Origin Languages
                Text(
                    text = "Original Language",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LangData.DEFINED_CODES_ORIGIN.forEach { (code, label) ->
                        val selected = draftFilter.olang.contains(code)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draftFilter.olang - code else draftFilter.olang + code
                                draftFilter = draftFilter.copy(olang = next)
                            },
                            leadingIcon = {
                                AssetImage(
                                    assetPath = LangData.getFlagPath(code),
                                    contentDescription = label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(selected, theme)
                        )
                    }
                }

                // 9. Platforms
                Text(
                    text = "Platforms",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlatfData.DEFINED_CODES.forEach { (code, label) ->
                        val selected = draftFilter.platform.contains(code)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draftFilter.platform - code else draftFilter.platform + code
                                draftFilter = draftFilter.copy(platform = next)
                            },
                            leadingIcon = {
                                AssetImage(
                                    assetPath = PlatfData.getImgPath(code),
                                    contentDescription = label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = vndbFilterChipColors(theme),
                            border = vndbFilterChipBorder(selected, theme)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
