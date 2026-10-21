package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.format.VoteFormatters
import com.vndblite.dcj.ui.format.badgeColor
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.displayText
import com.vndblite.dcj.ui.format.formatVnLengthUiText
import com.vndblite.dcj.ui.format.iconAssetUri
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
internal fun NsfwRevealBadge(
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
internal fun CollectionStatusCornerBadge(
    entry: CollectionEntry,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val semanticColors = VndbThemeTokens.colors
    val statusColor = entry.status.badgeColor()
    val statusLabel = entry.status.displayLabel()
    val formattedVote = VoteFormatters.formatTenPointVote(entry.vote)
    val content: @Composable () -> Unit = {
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

    if (onClick != null) {
        Surface(
            color = statusColor.copy(alpha = 0.92f),
            shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
            onClick = onClick,
            modifier = modifier
        ) {
            content()
        }
    } else {
        Surface(
            color = statusColor.copy(alpha = 0.92f),
            shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
            modifier = modifier
        ) {
            content()
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
internal fun VnGridCardSubtitleRow(
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
