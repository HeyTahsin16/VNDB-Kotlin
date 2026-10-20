package com.vndblite.dcj.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VnItemGridCard(
    vn: VnDataPhase01,
    labelCode: SortableCode,
    showCoverCensorGlobal: Boolean,
    collectionRecord: VnRecord? = null,
    p2Data: VnDataPhase02? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppThemeCode.current
    val isExplicit = vn.image?.isExplicit == true
    var censorOverrideRevealed by remember(vn.id) { mutableStateOf(false) }
    val shouldBlur = isExplicit && showCoverCensorGlobal && !censorOverrideRevealed

    val statusEnum = remember(collectionRecord?.status) {
        collectionRecord?.status?.let { CollectionStatusCode.fromName(it) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, theme.secondary, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("vn_card_${vn.id}"),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = theme.primary.copy(alpha = 0.85f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = vn.image?.displayUrl.orEmpty()
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = vn.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (shouldBlur) Modifier.blur(20.dp) else Modifier)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.primary.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "No Cover",
                        tint = theme.tertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Explicit censor badge (tap to toggle reveal)
            if (isExplicit && showCoverCensorGlobal) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { censorOverrideRevealed = !censorOverrideRevealed },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Explicit Cover",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (shouldBlur) "18+" else "Hide",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Collection status icon badge in top-right corner
            if (statusEnum != null) {
                Surface(
                    color = statusEnum.color.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        AssetImage(
                            assetPath = statusEnum.imagePath,
                            contentDescription = statusEnum.displayTitle,
                            modifier = Modifier.size(14.dp)
                        )
                        if (collectionRecord != null && collectionRecord.vote > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${collectionRecord.vote}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Selected overlay checkmark
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.secondary.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = theme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Bottom gradient with Title & Info Label
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Text(
                    text = vn.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                VnCardSubtitleRow(
                    vn = vn,
                    labelCode = labelCode,
                    collectionRecord = collectionRecord,
                    p2Data = p2Data,
                    accentColor = theme.secondary
                )
            }
        }
    }
}

@Composable
private fun VnCardSubtitleRow(
    vn: VnDataPhase01,
    labelCode: SortableCode,
    collectionRecord: VnRecord?,
    p2Data: VnDataPhase02?,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Original language flag
        AssetImage(
            assetPath = LangData.getFlagPath(vn.olang),
            contentDescription = vn.olang,
            modifier = Modifier
                .width(16.dp)
                .height(11.dp)
        )

        when (labelCode) {
            SortableCode.VOTECOUNT -> {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${vn.votecount} votes",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
            SortableCode.RELEASED -> {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = vn.released,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
            SortableCode.ADDED -> {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = collectionRecord?.added ?: vn.released,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
            SortableCode.STARTED -> {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = collectionRecord?.started ?: "Not started",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
            SortableCode.VOTE -> {
                Icon(
                    imageVector = Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                val v = collectionRecord?.vote ?: 0
                Text(
                    text = if (v > 0) "Voted: $v/10" else "Unvoted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
            SortableCode.LENGTH_MINUTES -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = formatVnLength(p2Data?.lengthMinutes, vn.length),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(14.dp)
                )
                val formattedRating = if (vn.rating > 0) {
                    val norm = if (vn.rating > 10.0) vn.rating / 10.0 else vn.rating
                    String.format(Locale.US, "%.2f", norm)
                } else {
                    "N/A"
                }
                Text(
                    text = "$formattedRating (${vn.votecount})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }
    }
}
