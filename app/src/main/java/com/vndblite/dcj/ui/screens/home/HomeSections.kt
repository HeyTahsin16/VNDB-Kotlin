package com.vndblite.dcj.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.GenrePreset
import com.vndblite.dcj.domain.model.PopularTraitPreset
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.badgeColor
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GenrePresetsSection(
    onSelectGenre: (GenrePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.explore_genres),
            style = MaterialTheme.typography.titleLarge,
            color = themeSpec.accentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GenrePreset.entries, key = { it.tagId }) { preset ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .width(148.dp)
                        .height(92.dp)
                        .semantics { role = Role.Button }
                        .clickable { onSelectGenre(preset) }
                        .testTag("genre_preset_${preset.name.lowercase()}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AssetImage(
                            assetPath = AssetPaths.genreArtPath(preset),
                            contentDescription = preset.defaultName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ColorTokens.Transparent,
                                            ColorTokens.ScrimDarkHeavy
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = preset.defaultName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorTokens.CardOverlayText,
                                modifier = Modifier.padding(10.dp)
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
fun PopularTraitsSection(
    onSelectTrait: (PopularTraitPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.home_popular_traits_title),
            style = MaterialTheme.typography.titleLarge,
            color = themeSpec.accentColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PopularTraitPreset.entries.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectTrait(preset) },
                    label = { Text("${preset.defaultGroupName}: ${preset.defaultName}") },
                    colors = vndbFilterChipColors(),
                    border = vndbFilterChipBorder(selected = false),
                    modifier = Modifier.testTag("trait_preset_${preset.traitId}")
                )
            }
        }
    }
}

@Composable
fun MemorableQuotesSection(
    quotes: List<Quote>,
    isLoading: Boolean,
    error: ApiError?,
    showCoverCensor: Boolean,
    revealedNsfwIds: Set<String>,
    onRefreshQuotes: () -> Unit,
    onOpenVnDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_quotes_title),
                style = MaterialTheme.typography.titleLarge,
                color = themeSpec.accentColor
            )
            IconButton(
                onClick = onRefreshQuotes,
                modifier = Modifier.testTag("home_shuffle_quotes_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.home_quotes_shuffle),
                    tint = themeSpec.accentColor
                )
            }
        }

        when {
            isLoading && quotes.isEmpty() -> LoadingStateView()
            error != null && quotes.isEmpty() -> ErrorStateView(error = error, onRetry = onRefreshQuotes)
            quotes.isEmpty() -> Text(
                text = stringResource(R.string.home_quotes_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = themeSpec.contentColor.copy(alpha = 0.75f)
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quotes.forEach { quote ->
                    QuoteItemCard(
                        quote = quote,
                        showCoverCensor = showCoverCensor,
                        isNsfwRevealed = quote.id in revealedNsfwIds || (quote.vn?.id in revealedNsfwIds),
                        onOpenVnDetail = onOpenVnDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun QuoteItemCard(
    quote: Quote,
    showCoverCensor: Boolean,
    isNsfwRevealed: Boolean,
    onOpenVnDetail: (String) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val vn = quote.vn
    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (vn != null) {
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable { onOpenVnDetail(vn.id) }
                } else {
                    Modifier
                }
            )
            .testTag("quote_card_${quote.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverUrl = vn?.image?.previewUrl.orEmpty()
            if (coverUrl.isNotBlank()) {
                val shouldBlur = vn?.image?.isExplicit == true && showCoverCensor && !isNsfwRevealed
                NsfwAsyncImage(
                    imageUrl = coverUrl,
                    contentDescription = vn?.title,
                    shouldBlur = shouldBlur,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 52.dp, height = 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = themeSpec.accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "“${quote.text}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = themeSpec.contentColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                val speaker = quote.characterName?.takeIf { it.isNotBlank() }
                val vnTitle = vn?.title?.takeIf { it.isNotBlank() }
                val attribution = listOfNotNull(speaker, vnTitle).joinToString(" — ")
                if (attribution.isNotBlank()) {
                    Text(
                        text = attribution,
                        style = MaterialTheme.typography.labelMedium,
                        color = themeSpec.accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionChartSection(
    statusCounts: Map<CollectionStatus, Int>,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val totalCount = statusCounts.values.sum().coerceAtLeast(1)

    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_collection_chart")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_collection_chart_title),
                style = MaterialTheme.typography.titleLarge,
                color = themeSpec.accentColor
            )
            CollectionStatus.entries.forEach { status ->
                val count = statusCounts[status] ?: 0
                val fraction = (count.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssetImage(
                        assetPath = AssetPaths.collectionStatusIconPath(status),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(status.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor,
                        modifier = Modifier.width(82.dp)
                    )
                    LinearProgressIndicator(
                        progress = { fraction },
                        color = status.badgeColor(),
                        trackColor = themeSpec.contentColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeSpec.contentColor,
                        modifier = Modifier.width(34.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiteStatsSection(
    stats: SiteStats?,
    isLoading: Boolean,
    error: ApiError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current
    val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

    Surface(
        color = themeSpec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_site_stats")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_site_stats_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = themeSpec.accentColor
                )
                IconButton(onClick = onRetry, modifier = Modifier.testTag("home_retry_stats_button")) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = themeSpec.accentColor
                    )
                }
            }

            when {
                isLoading && stats == null -> LoadingStateView()
                stats == null -> {
                    Text(
                        text = error?.let { stringResource(R.string.home_site_stats_unavailable) }
                            ?: stringResource(R.string.home_site_stats_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.8f)
                    )
                }
                else -> {
                    val entries = listOf(
                        stringResource(R.string.home_stat_vns) to stats.vn,
                        stringResource(R.string.home_stat_releases) to stats.releases,
                        stringResource(R.string.home_stat_producers) to stats.producers,
                        stringResource(R.string.home_stat_chars) to stats.chars,
                        stringResource(R.string.home_stat_staff) to stats.staff,
                        stringResource(R.string.home_stat_tags) to stats.tags,
                        stringResource(R.string.home_stat_traits) to stats.traits
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        entries.forEach { (label, value) ->
                            Surface(
                                color = themeSpec.contentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = numberFormat.format(value),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = themeSpec.accentColor
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = themeSpec.contentColor.copy(alpha = 0.88f)
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
