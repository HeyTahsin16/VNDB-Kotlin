package com.vndblite.dcj.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.format.VoteFormatters
import com.vndblite.dcj.ui.format.badgeColor
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.displayText
import com.vndblite.dcj.ui.format.formatVnLengthUiText
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

/**
 * Presentation configuration for [VnGridCard] keeping function parameters <= 8 (Rule 2, U7).
 */
@Immutable
data class VnGridCardDisplayState(
    val sortField: SortField = SortField.RATING,
    val showCoverCensorGlobal: Boolean = true,
    val isNsfwRevealed: Boolean = false,
    val collectionEntry: CollectionEntry? = null,
    val details: VnDetails? = null,
    val isSelected: Boolean = false,
    val inSelectionMode: Boolean = false
)

/**
 * Domain-model-backed visual novel card with:
 * - Session-scoped NSFW reveal state via [VnGridCardDisplayState.isNsfwRevealed] & [onToggleNsfwReveal] (F10)
 * - Merged card semantics, null `contentDescription` on the cover image beside the visible title
 *   to avoid double TalkBack announcement, and announced selection/NSFW states (U1)
 * - Semantic color tokens instead of raw `Color` literals (U2)
 * - Data-driven subtitle row replacing the 7 copy-pasted `when` branches (A9)
 * - Preserved `testTag("vn_card_${vn.id}")` (Rule 1)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VnGridCard(
    vn: VnSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    displayState: VnGridCardDisplayState = VnGridCardDisplayState(),
    onToggleNsfwReveal: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val isExplicit = vn.image?.isExplicit == true
    val shouldBlur = isExplicit && displayState.showCoverCensorGlobal && !displayState.isNsfwRevealed
    val selectedStateDesc = if (displayState.inSelectionMode) {
        if (displayState.isSelected) {
            stringResource(R.string.card_selected_state)
        } else {
            stringResource(R.string.card_unselected_state)
        }
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(14.dp))
            .let {
                if (displayState.isSelected) {
                    it.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                } else {
                    it
                }
            }
            .semantics(mergeDescendants = true) {
                role = if (displayState.inSelectionMode) Role.Checkbox else Role.Button
                if (displayState.inSelectionMode) {
                    selected = displayState.isSelected
                    if (selectedStateDesc != null) stateDescription = selectedStateDesc
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("vn_card_${vn.id}"),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        VnGridCardContentBox(
            vn = vn,
            displayState = displayState,
            isExplicit = isExplicit,
            shouldBlur = shouldBlur,
            onToggleNsfwReveal = onToggleNsfwReveal
        )
    }
}

@Composable
private fun VnGridCardContentBox(
    vn: VnSummary,
    displayState: VnGridCardDisplayState,
    isExplicit: Boolean,
    shouldBlur: Boolean,
    onToggleNsfwReveal: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VnCardCoverLayer(
            imageUrl = vn.image?.previewUrl.orEmpty(),
            shouldBlur = shouldBlur
        )

        if (isExplicit && displayState.showCoverCensorGlobal) {
            NsfwRevealBadge(
                shouldBlur = shouldBlur,
                onToggle = onToggleNsfwReveal,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            )
        }

        if (displayState.collectionEntry != null) {
            CollectionStatusCornerBadge(
                entry = displayState.collectionEntry,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        if (displayState.isSelected) {
            VnCardSelectionOverlay()
        }

        VnCardBottomGradientContent(
            vn = vn,
            displayState = displayState,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun VnCardCoverLayer(
    imageUrl: String,
    shouldBlur: Boolean
) {
    if (imageUrl.isNotBlank()) {
        // contentDescription is null because the visible title is inside this merged card (U1)
        NsfwAsyncImage(
            imageUrl = imageUrl,
            contentDescription = null,
            shouldBlur = shouldBlur,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun VnCardSelectionOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun VnCardBottomGradientContent(
    vn: VnSummary,
    displayState: VnGridCardDisplayState,
    modifier: Modifier = Modifier
) {
    val semanticColors = VndbThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        semanticColors.cardGradientStart,
                        semanticColors.cardGradientMiddle,
                        semanticColors.cardGradientEnd
                    )
                )
            )
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Text(
            text = vn.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = semanticColors.cardOverlayText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        VnGridCardSubtitleRow(
            vn = vn,
            sortField = displayState.sortField,
            collectionEntry = displayState.collectionEntry,
            details = displayState.details
        )
    }
}

@Composable
private fun NsfwRevealBadge(
    shouldBlur: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = VndbThemeTokens.colors
    val toggleCd = if (shouldBlur) {
        stringResource(R.string.card_nsfw_toggle_cd_hidden)
    } else {
        stringResource(R.string.card_nsfw_toggle_cd_revealed)
    }
    val badgeText = if (shouldBlur) {
        stringResource(R.string.card_nsfw_badge_hidden)
    } else {
        stringResource(R.string.card_nsfw_badge_revealed)
    }

    Surface(
        color = semanticColors.nsfwBadgeContainer,
        shape = RoundedCornerShape(8.dp),
        onClick = onToggle,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = toggleCd
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = semanticColors.nsfwBadgeTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = semanticColors.nsfwBadgeContent
            )
        }
    }
}

@Composable
private fun CollectionStatusCornerBadge(
    entry: CollectionEntry,
    modifier: Modifier = Modifier
) {
    val semanticColors = VndbThemeTokens.colors
    val statusColor = entry.status.badgeColor()
    val statusLabel = entry.status.displayLabel()
    val formattedVote = VoteFormatters.formatTenPointVote(entry.vote)

    Surface(
        color = statusColor.copy(alpha = 0.92f),
        shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            AssetImage(
                assetPath = entry.status.iconAssetUri(),
                contentDescription = statusLabel,
                modifier = Modifier.size(14.dp)
            )
            if (formattedVote != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedVote,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = semanticColors.onStatusBadge
                )
            }
        }
    }
}

private data class CardSubtitleSpec(
    val icon: ImageVector,
    val iconTint: Color,
    val text: String
)

/**
 * Single data-driven subtitle row replacing the 7 duplicated `when` branches in the legacy card (A9).
 */
@Composable
private fun VnGridCardSubtitleRow(
    vn: VnSummary,
    sortField: SortField,
    collectionEntry: CollectionEntry?,
    details: VnDetails?
) {
    val semanticColors = VndbThemeTokens.colors
    val accent = MaterialTheme.colorScheme.primary
    val spec = resolveSubtitleSpec(
        vn = vn,
        sortField = sortField,
        collectionEntry = collectionEntry,
        details = details,
        accentColor = accent,
        starColor = semanticColors.starGold
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AssetImage(
            assetPath = AssetPaths.languageFlagByCode(vn.originalLanguage),
            contentDescription = null,
            modifier = Modifier
                .width(16.dp)
                .height(11.dp)
        )
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.iconTint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = spec.text,
            style = MaterialTheme.typography.labelMedium,
            color = semanticColors.cardOverlaySubtleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun resolveSubtitleSpec(
    vn: VnSummary,
    sortField: SortField,
    collectionEntry: CollectionEntry?,
    details: VnDetails?,
    accentColor: Color,
    starColor: Color
): CardSubtitleSpec = when (sortField) {
    SortField.VOTE_COUNT -> CardSubtitleSpec(
        icon = Icons.Default.People,
        iconTint = accentColor,
        text = pluralStringResource(R.plurals.card_vote_count, vn.voteCount, vn.voteCount)
    )
    SortField.RELEASED -> CardSubtitleSpec(
        icon = Icons.Default.CalendarMonth,
        iconTint = accentColor,
        text = vn.released.displayText()
    )
    SortField.ADDED -> CardSubtitleSpec(
        icon = Icons.Default.AddCircleOutline,
        iconTint = accentColor,
        text = collectionEntry?.addedDate?.toString() ?: vn.released.displayText()
    )
    SortField.STARTED -> CardSubtitleSpec(
        icon = Icons.Default.PlayCircleOutline,
        iconTint = accentColor,
        text = collectionEntry?.startedDate?.toString() ?: stringResource(R.string.card_not_started)
    )
    SortField.USER_VOTE -> {
        val voteDisplay = VoteFormatters.formatTenPointVote(collectionEntry?.vote)
        val text = if (voteDisplay != null) {
            stringResource(R.string.card_voted_value, voteDisplay)
        } else {
            stringResource(R.string.card_unvoted)
        }
        CardSubtitleSpec(icon = Icons.Default.HowToVote, iconTint = accentColor, text = text)
    }
    SortField.LENGTH_MINUTES -> CardSubtitleSpec(
        icon = Icons.Default.Schedule,
        iconTint = accentColor,
        text = formatVnLengthUiText(details?.lengthMinutes, vn.lengthBucket).asString()
    )
    else -> {
        val ratingStr = VoteFormatters.formatTenPointRating(vn.rating)
        val text = if (ratingStr != null) {
            stringResource(R.string.card_rating_with_votes, ratingStr, vn.voteCount)
        } else {
            stringResource(R.string.card_rating_na, vn.voteCount)
        }
        CardSubtitleSpec(icon = Icons.Default.Star, iconTint = starColor, text = text)
    }
}

@Preview(name = "VnGridCard - Dark Theme", showBackground = true)
@Composable
private fun VnGridCardDarkPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        VnGridCard(
            vn = PreviewData.sampleVnSummary,
            displayState = VnGridCardDisplayState(collectionEntry = PreviewData.sampleCollectionEntry),
            onClick = {},
            modifier = Modifier
                .width(180.dp)
                .padding(8.dp)
        )
    }
}

@Preview(name = "VnGridCard - Explicit Blurred Light Theme", showBackground = true)
@Composable
private fun VnGridCardExplicitLightPreview() {
    VndbTheme(themeId = ThemeId.AIR, includeBackground = true) {
        VnGridCard(
            vn = PreviewData.sampleExplicitVnSummary,
            displayState = VnGridCardDisplayState(
                showCoverCensorGlobal = true,
                isNsfwRevealed = false
            ),
            onClick = {},
            modifier = Modifier
                .width(180.dp)
                .padding(8.dp)
        )
    }
}
