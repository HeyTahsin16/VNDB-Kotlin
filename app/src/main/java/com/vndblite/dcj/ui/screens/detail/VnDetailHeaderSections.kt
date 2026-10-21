package com.vndblite.dcj.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.VoteFormatters
import com.vndblite.dcj.ui.format.badgeColor
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
internal fun VnDetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("detail_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back_cd),
                tint = spec.accentColor
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = spec.accentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun VnDetailHeroCard(
    state: VnDetailUiState,
    onToggleReveal: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    val summary = state.summary
    val isExplicit = summary?.image?.isExplicit == true
    val shouldBlur = isExplicit && state.settings.showCoverCensor && (state.vnId !in state.nsfwRevealedIds)

    Surface(
        color = spec.surfaceColor.copy(alpha = 0.86f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(166.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                NsfwAsyncImage(
                    imageUrl = summary?.image?.url.orEmpty(),
                    contentDescription = null,
                    shouldBlur = shouldBlur,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isExplicit && state.settings.showCoverCensor) {
                    Surface(
                        color = semantic.nsfwBadgeContainer,
                        shape = RoundedCornerShape(8.dp),
                        onClick = onToggleReveal,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(
                                if (shouldBlur) R.string.card_nsfw_toggle_cd_hidden
                                else R.string.card_nsfw_toggle_cd_revealed
                            ),
                            tint = semantic.nsfwBadgeTint,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(15.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.onSurfaceColor,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = semantic.starGold,
                        modifier = Modifier.size(18.dp)
                    )
                    val formattedRating = VoteFormatters.formatTenPointRating(summary?.rating)
                    Text(
                        text = if (formattedRating != null) {
                            stringResource(
                                R.string.card_rating_with_votes,
                                formattedRating,
                                summary?.voteCount ?: 0
                            )
                        } else {
                            stringResource(R.string.card_rating_na, summary?.voteCount ?: 0)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.onSurfaceColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                state.collectionEntry?.let { entry ->
                    Surface(
                        color = entry.status.badgeColor().copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AssetImage(
                                assetPath = entry.status.iconAssetUri(),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = entry.status.displayLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = spec.onSurfaceColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun VnDetailTabRow(
    selectedTab: VnDetailTab,
    onSelectTab: (VnDetailTab) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(VnDetailTab.entries, key = { it.name }) { tab ->
            val selected = tab == selectedTab
            Surface(
                color = if (selected) spec.accentColor else spec.surfaceColor.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelectTab(tab) }
                    )
                    .testTag(tab.testTag)
            ) {
                Text(
                    text = stringResource(tab.labelResId),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) semantic.onAccentButton else spec.onSurfaceColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
