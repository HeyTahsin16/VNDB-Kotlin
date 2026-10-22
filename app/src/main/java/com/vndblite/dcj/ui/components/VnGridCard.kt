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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.format.VnFormatters
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.badgeColor
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VnGridCard(
    vn: VnSummary,
    sortField: SortField = SortField.RATING,
    showCoverCensorGlobal: Boolean = true,
    isNsfwRevealed: Boolean = false,
    onToggleNsfwReveal: (() -> Unit)? = null,
    collectionEntry: CollectionEntry? = null,
    details: VnDetails? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val isExplicit = vn.image?.isExplicit == true
    val shouldBlur = isExplicit && showCoverCensorGlobal && !isNsfwRevealed
    val status = collectionEntry?.status

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, themeSpec.accentColor, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isSelected
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("vn_card_${vn.id}"),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = themeSpec.surfaceColor.copy(alpha = 0.85f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = vn.image?.previewUrl.orEmpty()
            if (imageUrl.isNotBlank()) {
                NsfwAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = vn.title,
                    shouldBlur = shouldBlur,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeSpec.surfaceColor.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = stringResource(R.string.label_no_cover),
                        tint = themeSpec.contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (isExplicit && showCoverCensorGlobal && onToggleNsfwReveal != null) {
                Surface(
                    color = ColorTokens.ScrimDarkLight,
                    shape = RoundedCornerShape(8.dp),
                    onClick = onToggleNsfwReveal,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .testTag("nsfw_toggle_${vn.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.nsfw_toggle_cd),
                            tint = ColorTokens.NsfwWarningTint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (shouldBlur) {
                                stringResource(R.string.nsfw_badge_reveal)
                            } else {
                                stringResource(R.string.nsfw_badge_hide)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorTokens.CardOverlayText
                        )
                    }
                }
            }

            if (status != null) {
                val voteLabel = VnFormatters.formatVoteTenPoint(collectionEntry.vote)
                Surface(
                    color = status.badgeColor().copy(alpha = 0.92f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        AssetImage(
                            assetPath = AssetPaths.collectionStatusIconPath(status),
                            contentDescription = stringResource(status.labelRes()),
                            modifier = Modifier.size(14.dp)
                        )
                        if (voteLabel != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = voteLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorTokens.BadgeDarkText
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeSpec.accentColor.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.label_selected),
                        tint = themeSpec.accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ColorTokens.Transparent,
                                ColorTokens.ScrimDarkLight,
                                ColorTokens.ScrimDarkHeavy
                            )
                        )
                    )
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Text(
                    text = vn.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.CardOverlayText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                VnGridCardSubtitle(
                    vn = vn,
                    sortField = sortField,
                    collectionEntry = collectionEntry,
                    details = details
                )
            }
        }
    }
}

@Composable
private fun VnGridCardSubtitle(
    vn: VnSummary,
    sortField: SortField,
    collectionEntry: CollectionEntry?,
    details: VnDetails?
) {
    val accentColor = LocalThemeSpec.current.accentColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AssetImage(
            assetPath = AssetPaths.languageFlagPath(vn.originalLanguage),
            contentDescription = vn.originalLanguage,
            modifier = Modifier
                .width(16.dp)
                .height(11.dp)
        )

        when (sortField) {
            SortField.VOTE_COUNT -> {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = stringResource(R.string.votes_count_format, vn.voteCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
            SortField.RELEASED -> {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = VnFormatters.formatReleaseDate(vn.released).asString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
            SortField.ADDED -> {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                val addedText = collectionEntry?.addedDate?.toString()
                    ?: VnFormatters.formatReleaseDate(vn.released).asString()
                Text(
                    text = addedText,
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
            SortField.STARTED -> {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = VnFormatters.formatLocalDate(collectionEntry?.startedDate).asString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
            SortField.USER_VOTE -> {
                Icon(
                    imageVector = Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                val tenPoint = VnFormatters.formatVoteTenPoint(collectionEntry?.vote)
                Text(
                    text = if (tenPoint != null) {
                        stringResource(R.string.vote_card_format, tenPoint)
                    } else {
                        stringResource(R.string.label_unvoted)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
            SortField.LENGTH_MINUTES -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = VnFormatters.formatPlayLength(details?.lengthMinutes, vn.lengthBucket).asString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = ColorTokens.RatingStarGold,
                    modifier = Modifier.size(14.dp)
                )
                val ratingStr = VnFormatters.formatRating(vn.rating)
                Text(
                    text = stringResource(R.string.rating_with_votes_format, ratingStr, vn.voteCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.CardOverlaySubtext,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VnGridCardPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        Box(modifier = Modifier.width(180.dp).padding(8.dp)) {
            VnGridCard(
                vn = VnSummary(
                    id = "v17",
                    title = "Ever17 -the out of infinity-",
                    rating = 8.61,
                    voteCount = 7420,
                    originalLanguage = "ja",
                    lengthBucket = VnLengthBucket.LONG,
                    released = ReleaseDate.Exact(LocalDate.of(2002, 8, 29))
                ),
                collectionEntry = CollectionEntry(
                    vnId = "v17",
                    title = "Ever17",
                    status = CollectionStatus.FINISHED,
                    vote = 90
                ),
                onClick = {}
            )
        }
    }
}
