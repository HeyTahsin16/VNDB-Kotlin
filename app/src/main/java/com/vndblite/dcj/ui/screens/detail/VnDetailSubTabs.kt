package com.vndblite.dcj.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.SectionStateContainer
import com.vndblite.dcj.ui.components.SectionUiState
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.components.VnGridCardDisplayState
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.openExternalUrl
import com.vndblite.dcj.ui.screens.characters.CharacterItemCard
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
internal fun VnDetailCharactersTabContent(
    charactersState: SectionUiState<CharactersTabPayload>,
    showCoverCensor: Boolean,
    nsfwRevealedIds: Set<String>,
    onAction: (VnDetailAction) -> Unit
) {
    val spec = VndbThemeTokens.spec
    SectionStateContainer(
        state = charactersState,
        emptyMessage = UiText.StringResource(R.string.detail_characters_empty),
        onRetry = { onAction(VnDetailAction.RetryCharacters) }
    ) { payload ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (payload.isTruncated) {
                Surface(
                    color = spec.accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.detail_characters_truncated_notice),
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.onSurfaceColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            payload.characters.forEach { character ->
                CharacterItemCard(
                    character = character,
                    showCoverCensor = showCoverCensor,
                    isNsfwRevealed = character.id in nsfwRevealedIds,
                    onToggleNsfwReveal = { onAction(VnDetailAction.ToggleNsfwReveal(character.id)) },
                    onTraitClick = { trait -> onAction(VnDetailAction.FilterByTrait(trait)) },
                    onVnClick = { vn -> onAction(VnDetailAction.OpenRelatedVn(vn)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VnDetailRelationsTabContent(
    vnId: String,
    details: VnDetails?,
    relatedCollectionEntries: Map<String, CollectionEntry>,
    showCoverCensor: Boolean,
    nsfwRevealedIds: Set<String>,
    onAction: (VnDetailAction) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val context = LocalContext.current
    val relations = details?.relations.orEmpty()
    val extLinks = details?.externalLinks.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = spec.surfaceColor.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.detail_relations_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (relations.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_relations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.onSurfaceColor.copy(alpha = 0.75f)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(relations, key = { it.id }) { rel ->
                            val relatedVn = rel.relatedVn ?: VnSummary(
                                id = rel.id,
                                title = rel.id
                            )
                            Column(modifier = Modifier.width(156.dp)) {
                                Text(
                                    text = rel.relationType.displayLabel(rel.isOfficial),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = spec.accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                VnGridCard(
                                    vn = relatedVn,
                                    onClick = { onAction(VnDetailAction.OpenRelatedVn(relatedVn)) },
                                    displayState = VnGridCardDisplayState(
                                        subtitleSortField = SortField.RATING,
                                        collectionEntry = relatedCollectionEntries[rel.id],
                                        blurExplicitCover = showCoverCensor,
                                        isNsfwRevealed = rel.id in nsfwRevealedIds
                                    ),
                                    onToggleNsfwReveal = {
                                        onAction(VnDetailAction.ToggleNsfwReveal(rel.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = spec.surfaceColor.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.detail_external_links_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.accentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { openExternalUrl(context, "${AppInfo.VNDB_URL}/$vnId") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = spec.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.detail_open_on_vndb)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = spec.accentColor.copy(alpha = 0.16f),
                            labelColor = spec.onSurfaceColor
                        ),
                        border = BorderStroke(1.dp, spec.accentColor)
                    )
                    extLinks.forEach { linkUrl ->
                        val domainLabel = runCatching {
                            java.net.URI(linkUrl).host?.removePrefix("www.")
                        }.getOrNull()?.takeIf { it.isNotBlank() } ?: linkUrl
                        AssistChip(
                            onClick = { openExternalUrl(context, linkUrl) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = spec.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(domainLabel) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = spec.onSurfaceColor.copy(alpha = 0.08f),
                                labelColor = spec.onSurfaceColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VnDetailQuotesTabContent(
    quotesState: SectionUiState<List<Quote>>,
    onRetry: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    SectionStateContainer(
        state = quotesState,
        emptyMessage = UiText.StringResource(R.string.detail_quotes_empty),
        onRetry = onRetry
    ) { quotes ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            quotes.forEach { quote ->
                Surface(
                    color = spec.surfaceColor.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = spec.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = quote.characterName
                                    ?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.home_quote_speaker_unknown),
                                style = MaterialTheme.typography.titleMedium,
                                color = spec.accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${quote.text}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = spec.onSurfaceColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VnDetailReviewsTabContent(
    reviewsState: SectionUiState<List<Review>>,
    onRetry: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    val context = LocalContext.current

    SectionStateContainer(
        state = reviewsState,
        emptyMessage = UiText.StringResource(R.string.detail_reviews_empty),
        onRetry = onRetry
    ) { reviews ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            reviews.forEach { review ->
                Surface(
                    color = spec.surfaceColor.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_card_${review.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = review.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = spec.accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                                review.date?.let { d ->
                                    Text(
                                        text = d.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = spec.onSurfaceColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            review.ratingText?.takeIf { it.isNotBlank() }?.let { score ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = semantic.starGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = score,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = spec.onSurfaceColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        BbCodeText(
                            rawText = review.text,
                            textColor = spec.onSurfaceColor,
                            linkColor = spec.accentColor,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = listOfNotNull(review.pointsText, review.commentsCountText)
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = spec.onSurfaceColor.copy(alpha = 0.7f)
                            )
                            if (review.hasReadMore) {
                                TextButton(
                                    onClick = { openExternalUrl(context, "${AppInfo.VNDB_URL}/${review.id}") }
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_review_read_full),
                                        color = spec.accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
