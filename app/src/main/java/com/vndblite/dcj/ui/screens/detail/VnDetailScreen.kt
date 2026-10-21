package com.vndblite.dcj.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vndblite.dcj.LocalAppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.CollectionEntryDialog
import com.vndblite.dcj.ui.components.InlineSectionErrorCard
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.format.VoteFormatters
import com.vndblite.dcj.ui.format.badgeColor
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
fun VnDetailScreen(
    vnId: String,
    initialTitle: String,
    onBack: () -> Unit,
    onOpenRelatedVn: (VnSummary) -> Unit,
    onFilterByTag: (Tag) -> Unit,
    onFilterByTrait: (Trait) -> Unit,
    viewModel: VnDetailViewModel = viewModel(
        key = "vn_detail_$vnId",
        factory = VnDetailViewModel.factory(
            vnId = vnId,
            initialTitle = initialTitle,
            container = LocalAppContainer.current
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VnDetailContent(
        state = state,
        onAction = { action ->
            when (action) {
                VnDetailAction.Back -> onBack()
                is VnDetailAction.OpenRelatedVn -> onOpenRelatedVn(action.vn)
                is VnDetailAction.FilterByTag -> onFilterByTag(action.tag)
                is VnDetailAction.FilterByTrait -> onFilterByTrait(action.trait)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
fun VnDetailContent(
    state: VnDetailUiState,
    onAction: (VnDetailAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    var showCollectionDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            val existingEntry = state.collectionEntry
            ExtendedFloatingActionButton(
                onClick = { showCollectionDialog = true },
                containerColor = existingEntry?.status?.badgeColor() ?: spec.accentColor,
                contentColor = if (existingEntry != null) semantic.onStatusBadge else semantic.onAccentButton,
                icon = {
                    Icon(
                        imageVector = if (existingEntry != null) Icons.Default.Edit else Icons.Default.BookmarkAdd,
                        contentDescription = null
                    )
                },
                text = {
                    val label = if (existingEntry != null) {
                        val voteStr = VoteFormatters.formatTenPointVote(existingEntry.vote)
                        if (voteStr != null) {
                            "${existingEntry.status.displayLabel()} ★ $voteStr"
                        } else {
                            existingEntry.status.displayLabel()
                        }
                    } else {
                        stringResource(R.string.add_to_collection)
                    }
                    Text(label, fontWeight = FontWeight.Bold)
                },
                modifier = Modifier.testTag("detail_collection_fab")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "detail_top_bar") {
                VnDetailTopBar(
                    title = state.displayTitle,
                    onBack = { onAction(VnDetailAction.Back) }
                )
            }

            item(key = "detail_hero_card") {
                VnDetailHeroCard(
                    state = state,
                    onToggleReveal = { onAction(VnDetailAction.ToggleNsfwReveal(state.vnId)) }
                )
            }

            state.mainError?.let { err ->
                item(key = "detail_main_error") {
                    InlineSectionErrorCard(
                        message = err,
                        onRetry = { onAction(VnDetailAction.RetryMainDetails) }
                    )
                }
            }

            item(key = "detail_tabs") {
                VnDetailTabRow(
                    selectedTab = state.selectedTab,
                    onSelectTab = { onAction(VnDetailAction.SelectTab(it)) }
                )
            }

            item(key = "detail_active_tab_${state.selectedTab.name}") {
                when (state.selectedTab) {
                    VnDetailTab.GENERAL -> VnDetailGeneralTabContent(
                        summary = state.summary,
                        details = state.details,
                        maxTagSpoilerLevel = state.maxTagSpoilerLevel,
                        showCoverCensor = state.settings.showCoverCensor,
                        nsfwRevealedIds = state.nsfwRevealedIds,
                        onAction = onAction
                    )
                    VnDetailTab.CHARACTERS -> VnDetailCharactersTabContent(
                        charactersState = state.charactersState,
                        showCoverCensor = state.settings.showCoverCensor,
                        nsfwRevealedIds = state.nsfwRevealedIds,
                        onAction = onAction
                    )
                    VnDetailTab.RELATIONS -> VnDetailRelationsTabContent(
                        vnId = state.vnId,
                        details = state.details,
                        relatedCollectionEntries = state.relatedCollectionEntries,
                        showCoverCensor = state.settings.showCoverCensor,
                        nsfwRevealedIds = state.nsfwRevealedIds,
                        onAction = onAction
                    )
                    VnDetailTab.QUOTES -> VnDetailQuotesTabContent(
                        quotesState = state.quotesState,
                        onRetry = { onAction(VnDetailAction.RetryQuotes) }
                    )
                    VnDetailTab.REVIEWS -> VnDetailReviewsTabContent(
                        reviewsState = state.reviewsState,
                        onRetry = { onAction(VnDetailAction.RetryReviews) }
                    )
                }
            }

            item(key = "detail_bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showCollectionDialog) {
        CollectionEntryDialog(
            vnTitle = state.displayTitle,
            existingEntry = state.collectionEntry,
            isAccountSynced = state.isAccountSynced,
            onSave = { payload ->
                showCollectionDialog = false
                onAction(VnDetailAction.SaveCollectionEntry(payload))
            },
            onRemove = {
                showCollectionDialog = false
                onAction(VnDetailAction.RemoveCollectionEntry)
            },
            onDismiss = { showCollectionDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VnDetailContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        VnDetailContent(
            state = VnDetailUiState(
                vnId = "v17",
                summary = PreviewData.sampleVnSummary,
                details = PreviewData.sampleVnDetails,
                collectionEntry = PreviewData.sampleCollectionEntry
            ),
            onAction = {}
        )
    }
}
