package com.vndblite.dcj.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.SectionLoadState
import com.vndblite.dcj.ui.components.SectionStateContainer
import com.vndblite.dcj.ui.components.VnGridCard
import com.vndblite.dcj.ui.components.VnGridCardDisplayState
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import java.text.NumberFormat
import java.util.Locale

internal data class HomeCarouselDisplayConfig(
    val subtitleSortField: SortField,
    val collectionEntries: Map<String, CollectionEntry>,
    val showCoverCensor: Boolean,
    val nsfwRevealedIds: Set<String>
)

@Composable
internal fun HomeVnCarouselSection(
    title: String,
    seeMoreTag: String,
    loadState: SectionLoadState<List<VnSummary>>,
    displayConfig: HomeCarouselDisplayConfig,
    onSeeMore: () -> Unit,
    onRetry: () -> Unit,
    onVnClick: (VnSummary) -> Unit,
    onToggleNsfwReveal: (String) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onSeeMore,
                modifier = Modifier.testTag(seeMoreTag)
            ) {
                Text(
                    text = stringResource(R.string.home_see_more),
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.onSurfaceColor.copy(alpha = 0.85f)
                )
            }
        }

        SectionStateContainer(
            state = loadState,
            emptyMessage = UiText.StringResource(R.string.home_section_empty),
            onRetry = onRetry,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) { items ->
            LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { vn ->
                    Box(modifier = Modifier.width(160.dp)) {
                        VnGridCard(
                            vn = vn,
                            onClick = { onVnClick(vn) },
                            displayState = VnGridCardDisplayState(
                                subtitleSortField = displayConfig.subtitleSortField,
                                collectionEntry = displayConfig.collectionEntries[vn.id],
                                blurExplicitCover = displayConfig.showCoverCensor,
                                isNsfwRevealed = vn.id in displayConfig.nsfwRevealedIds
                            ),
                            onToggleNsfwReveal = { onToggleNsfwReveal(vn.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeStatsChartCard(
    statsState: SectionLoadState<SiteStats>,
    onRetry: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_section_stats),
                style = MaterialTheme.typography.titleLarge,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            SectionStateContainer(
                state = statsState,
                emptyMessage = UiText.StringResource(R.string.home_stats_unavailable),
                onRetry = onRetry
            ) { stats ->
                val entries = listOf(
                    stringResource(R.string.stats_visual_novels) to stats.vn,
                    stringResource(R.string.stats_releases) to stats.releases,
                    stringResource(R.string.stats_characters) to stats.chars,
                    stringResource(R.string.stats_producers) to stats.producers,
                    stringResource(R.string.stats_staff) to stats.staff,
                    stringResource(R.string.stats_tags) to stats.tags,
                    stringResource(R.string.stats_traits) to stats.traits
                )
                val maxValue = entries.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                val numberFormat = NumberFormat.getIntegerInstance(Locale.US)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    entries.forEach { (label, count) ->
                        val ratio = (count.toFloat() / maxValue.toFloat()).coerceIn(0.08f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = spec.onSurfaceColor
                                )
                                Text(
                                    text = numberFormat.format(count),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = spec.accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(spec.onSurfaceColor.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(spec.accentColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
