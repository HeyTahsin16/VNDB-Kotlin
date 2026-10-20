package com.vndblite.dcj.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.VnItemGridCard
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import com.vndblite.dcj.ui.viewmodel.HomeUiState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    homeState: HomeUiState,
    settings: SettingsState,
    rawRecords: Map<String, VnRecord>,
    onVnClick: (VnDataPhase01) -> Unit,
    onSeeMoreSection: (HomeSectionsCode) -> Unit,
    onGoToCollectionTab: () -> Unit,
    onOpenTraitFinder: (VnTrait?) -> Unit,
    onOpenRandomVn: () -> Unit,
    onShuffleQuotes: () -> Unit,
    onRefresh: () -> Unit
) {
    val theme = LocalAppThemeCode.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Greeting & Quick Actions Banner
        item {
            Surface(
                color = theme.primary.copy(alpha = 0.82f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                assetPath = "assets/images/icon/app_icon.png",
                                contentDescription = "VNDB Lite Icon",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome to VNDB Lite!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = theme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Visual novel database & character trait explorer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.tertiary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Home",
                                tint = theme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Action Buttons: Character Trait Search & Random VN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpenTraitFinder(null) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = theme.secondary.copy(alpha = 0.22f),
                                contentColor = theme.tertiary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_trait_finder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = theme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trait Search", maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = onOpenRandomVn,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = theme.secondary.copy(alpha = 0.22f),
                                contentColor = theme.tertiary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_random_vn_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = theme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Random VN", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Loading indicator if initial load
        if (homeState.isLoading && homeState.sections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = theme.secondary)
                }
            }
        }

        // Preview Sections (includes Top Rated, Top Voted, Popular Ongoing, Latest Releases, Upcoming Releases)
        items(HomeSectionsCode.entries) { section ->
            val itemsForSection = homeState.sections[section].orEmpty()
            // Only show My Collection if not empty
            if (section == HomeSectionsCode.COLLECTION && itemsForSection.isEmpty()) {
                return@items
            }

            Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            if (section == HomeSectionsCode.COLLECTION) {
                                onGoToCollectionTab()
                            } else {
                                onSeeMoreSection(section)
                            }
                        },
                        modifier = Modifier.testTag("see_more_${section.name.lowercase()}")
                    ) {
                        Text(
                            text = "See more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = theme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (itemsForSection.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (homeState.isLoading) {
                            CircularProgressIndicator(
                                color = theme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Text(
                                text = "No visual novels loaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.tertiary.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(itemsForSection, key = { "${section.name}_${it.id}" }) { vn ->
                            VnItemGridCard(
                                vn = vn,
                                labelCode = if (section == HomeSectionsCode.COLLECTION) SortableCode.RATING else section.labelCode,
                                showCoverCensorGlobal = settings.showCoverCensor,
                                collectionRecord = rawRecords[vn.id],
                                onClick = { onVnClick(vn) },
                                modifier = Modifier.width(155.dp)
                            )
                        }
                    }
                }
            }
        }

        // Popular Character Traits Carousel on Home
        item {
            Column(modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Popular Character Traits",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onOpenTraitFinder(null) }) {
                        Text("Explore traits", color = theme.tertiary)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PopularCharacterTraits.presets, key = { it.id }) { trait ->
                        Surface(
                            color = theme.primary.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.secondary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { onOpenTraitFinder(trait) }
                                .testTag("home_trait_chip_${trait.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = trait.groupName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondary
                                )
                                Text(
                                    text = trait.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // VNDB Database Radar Chart & Stats
        if (settings.showChart) {
            item {
                StatChartSection(
                    stats = homeState.stats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        // VNDB Quotes at the Bottom of Home Tab
        item {
            VndbQuotesSection(
                quotes = homeState.quotes,
                isRefreshingQuotes = homeState.isRefreshingQuotes,
                showCoverCensor = settings.showCoverCensor,
                onShuffleQuotes = onShuffleQuotes,
                onVnClick = onVnClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun VndbQuotesSection(
    quotes: List<VnQuote>,
    isRefreshingQuotes: Boolean,
    showCoverCensor: Boolean,
    onShuffleQuotes: () -> Unit,
    onVnClick: (VnDataPhase01) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppThemeCode.current

    Surface(
        color = theme.primary.copy(alpha = 0.86f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.secondary.copy(alpha = 0.35f)),
        modifier = modifier.testTag("vndb_quotes_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = theme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VNDB Quotes",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Random & top-rated quotes from visual novels",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.tertiary.copy(alpha = 0.8f)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onShuffleQuotes,
                    enabled = !isRefreshingQuotes,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = theme.secondary.copy(alpha = 0.24f),
                        contentColor = theme.tertiary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("shuffle_quotes_button")
                ) {
                    if (isRefreshingQuotes) {
                        CircularProgressIndicator(
                            color = theme.secondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Shuffle Quotes",
                            tint = theme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (quotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshingQuotes) "Loading quotes from VNDB..." else "Tap Shuffle to load visual novel quotes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary.copy(alpha = 0.75f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quotes.forEach { q ->
                        Surface(
                            color = theme.tertiary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                theme.secondary.copy(alpha = 0.30f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (q.vn != null) {
                                        Modifier.clickable { onVnClick(q.vn) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("quote_card_${q.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "“${q.quote}”",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.tertiary
                                )

                                if (q.characterName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val origPart = if (q.characterOriginal.isNotBlank()) " (${q.characterOriginal})" else ""
                                    Text(
                                        text = "— ${q.characterName}$origPart",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = theme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (q.vn != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = theme.tertiary.copy(alpha = 0.14f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val imgUrl = q.vn.image?.displayUrl.orEmpty()
                                            val shouldBlur = (q.vn.image?.isExplicit == true) && showCoverCensor
                                            if (imgUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = imgUrl,
                                                    contentDescription = q.vn.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(width = 34.dp, height = 46.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .then(if (shouldBlur) Modifier.blur(10.dp) else Modifier)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }
                                            Column {
                                                Text(
                                                    text = q.vn.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.tertiary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val metaParts = mutableListOf<String>()
                                                if (q.vn.released.isNotBlank() && q.vn.released != "Unknown") {
                                                    metaParts.add(q.vn.released)
                                                }
                                                if (q.vn.rating > 0) {
                                                    metaParts.add(String.format(Locale.US, "★ %.2f", q.vn.rating / 10.0))
                                                }
                                                if (metaParts.isNotEmpty()) {
                                                    Text(
                                                        text = metaParts.joinToString(" • "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = theme.tertiary.copy(alpha = 0.78f)
                                                    )
                                                }
                                            }
                                        }

                                        if (q.score > 0) {
                                            Surface(
                                                color = theme.secondary.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Score ${q.score}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = theme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        }
    }
}

@Composable
fun StatChartSection(
    stats: List<ChartStatItem>,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppThemeCode.current
    val numberFormat = NumberFormat.getIntegerInstance(Locale.US)
    val maxVal = (stats.maxOfOrNull { it.count } ?: 150000).coerceAtLeast(1)

    Surface(
        color = theme.primary.copy(alpha = 0.84f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.testTag("vndb_stats_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VNDB Database Overview",
                style = MaterialTheme.typography.titleLarge,
                color = theme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live statistics from api.vndb.org/kana/stats",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.tertiary.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Radar Polygon Chart
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f
                    val n = stats.size.coerceAtLeast(3)

                    // Draw concentric web rings
                    for (ring in 1..4) {
                        val r = radius * (ring / 4f)
                        val ringPath = Path()
                        for (i in 0 until n) {
                            val angle = (2 * PI * i / n) - (PI / 2)
                            val x = center.x + (r * cos(angle)).toFloat()
                            val y = center.y + (r * sin(angle)).toFloat()
                            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                        }
                        ringPath.close()
                        drawPath(
                            path = ringPath,
                            color = theme.tertiary.copy(alpha = 0.2f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Draw axes and data polygon
                    val dataPath = Path()
                    val points = mutableListOf<Pair<Offset, Color>>()
                    for (i in 0 until n) {
                        val angle = (2 * PI * i / n) - (PI / 2)
                        val axisEnd = Offset(
                            center.x + (radius * cos(angle)).toFloat(),
                            center.y + (radius * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = theme.tertiary.copy(alpha = 0.2f),
                            start = center,
                            end = axisEnd,
                            strokeWidth = 1.dp.toPx()
                        )

                        val item = stats[i]
                        val fraction = (item.count.toFloat() / maxVal.toFloat()).coerceIn(0.12f, 1f)
                        val pt = Offset(
                            center.x + (radius * fraction * cos(angle)).toFloat(),
                            center.y + (radius * fraction * sin(angle)).toFloat()
                        )
                        if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
                        points.add(pt to item.color)
                    }
                    dataPath.close()

                    drawPath(
                        path = dataPath,
                        color = theme.secondary.copy(alpha = 0.30f)
                    )
                    drawPath(
                        path = dataPath,
                        color = theme.secondary,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    points.forEach { (pt, color) ->
                        drawCircle(
                            color = color,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend & Counts Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { stat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        theme.tertiary.copy(alpha = 0.08f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(stat.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stat.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.tertiary.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = numberFormat.format(stat.count),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.tertiary
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
