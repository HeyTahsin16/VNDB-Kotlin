package com.vndblite.dcj.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.CollectionRecordDialog
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.components.VnItemGridCard
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import com.vndblite.dcj.ui.viewmodel.VnDetailUiState
import androidx.compose.ui.text.font.FontStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VnDetailScreen(
    vnId: String,
    detailState: VnDetailUiState?,
    settings: SettingsState,
    collectionRecord: VnRecord?,
    rawRecords: Map<String, VnRecord>,
    onBack: () -> Unit,
    onSaveToCollection: (VnDataPhase01, String, Int, String?, String?, String?) -> Unit,
    onRemoveFromCollection: (String) -> Unit,
    onTagClick: (VnTag) -> Unit,
    onTraitClick: (VnTrait) -> Unit,
    onRelatedVnClick: (VnDataPhase01) -> Unit,
    onExpandFullReview: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val theme = LocalAppThemeCode.current
    val context = LocalContext.current
    val p1 = detailState?.p1
    val p2 = detailState?.p2
    val p3 = detailState?.p3

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var fullscreenScreenshot by remember { mutableStateOf<VnImage?>(null) }
    var selectedCharacterModal by remember { mutableStateOf<VnCharacter?>(null) }
    var showSpoilersInTags by remember { mutableStateOf(false) }

    val tabs = listOf("General", "Characters", "Reviews", "Releases", "Relations")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = p1?.title ?: vnId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = theme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("vn_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.tertiary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://vndb.org/$vnId"))
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open on vndb.org",
                            tint = theme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.primary.copy(alpha = 0.90f)
                )
            )
        }
    ) { innerPadding ->
        if (p1 == null && detailState?.isLoading == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = theme.secondary)
            }
            return@Scaffold
        }

        if (p1 == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = detailState?.error ?: "Visual novel details unavailable.",
                    color = theme.tertiary
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("vn_detail_scroll_list"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Top Hero Header Card
            item {
                VnDetailHeaderCard(
                    p1 = p1,
                    p2 = p2,
                    showCoverCensor = settings.showCoverCensor,
                    collectionRecord = collectionRecord,
                    onEditCollectionClick = { showCollectionDialog = true }
                )
            }

            // Tab Row: General | Characters | Reviews | Releases | Relations
            item {
                Surface(
                    color = theme.primary.copy(alpha = 0.90f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 8.dp,
                        containerColor = Color.Transparent,
                        contentColor = theme.secondary
                    ) {
                        tabs.forEachIndexed { idx, label ->
                            val badgeCount = when (idx) {
                                1 -> detailState.characters.size
                                2 -> detailState.reviews.size
                                4 -> detailState.relatedVns.size
                                else -> 0
                            }
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                modifier = Modifier.testTag("vn_detail_tab_${label.lowercase()}")
                            ) {
                                Text(
                                    text = if (badgeCount > 0) "$label ($badgeCount)" else label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == idx) theme.secondary else theme.tertiary.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tab Body
            when (selectedTab) {
                0 -> {
                    // GENERAL TAB
                    item {
                        VnDetailGeneralSection(
                            p1 = p1,
                            p2 = p2,
                            quotes = detailState.quotes,
                            reviews = detailState.reviews,
                            collectionRecord = collectionRecord,
                            showCoverCensor = settings.showCoverCensor,
                            showSpoilersInTags = showSpoilersInTags,
                            onToggleSpoilers = { showSpoilersInTags = !showSpoilersInTags },
                            onTagClick = onTagClick,
                            onScreenshotClick = { fullscreenScreenshot = it },
                            onOpenReviewsTab = { selectedTab = 2 },
                            onEditCollectionNote = { showCollectionDialog = true }
                        )
                    }
                }

                1 -> {
                    // CHARACTERS & TRAITS TAB
                    if (detailState.isLoadingCharacters) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = theme.secondary)
                            }
                        }
                    } else if (detailState.characters.isEmpty()) {
                        item {
                            Surface(
                                color = theme.primary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "No character entries found on VNDB for this visual novel.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = theme.tertiary,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Tap any character for full profile & biography, or tap any trait chip to search VNs with that character trait.",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.tertiary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(detailState.characters, key = { it.id }) { char ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                CharacterResultCard(
                                    character = char,
                                    showCoverCensor = settings.showCoverCensor,
                                    onClick = { selectedCharacterModal = char },
                                    onTraitClick = onTraitClick,
                                    onFilterVnByTrait = onTraitClick
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // REVIEWS & PERSONAL NOTES TAB
                    item {
                        VnDetailReviewsSection(
                            vnId = vnId,
                            reviews = detailState.reviews,
                            isLoadingReviews = detailState.isLoadingReviews,
                            expandingReviewIds = detailState.expandingReviewIds,
                            collectionRecord = collectionRecord,
                            onEditCollectionNote = { showCollectionDialog = true },
                            onExpandFullReview = onExpandFullReview
                        )
                    }
                }

                3 -> {
                    // RELEASES & PLATFORMS TAB
                    item {
                        VnDetailReleasesSection(p1 = p1, p2 = p2)
                    }
                }

                4 -> {
                    // RELATIONS & EXTERNAL LINKS TAB
                    item {
                        VnDetailRelationsSection(
                            p3 = p3,
                            relatedVns = detailState.relatedVns,
                            showCoverCensor = settings.showCoverCensor,
                            rawRecords = rawRecords,
                            onRelatedVnClick = onRelatedVnClick
                        )
                    }
                }
            }
        }
    }

    // Collection Add/Edit Modal
    if (showCollectionDialog && p1 != null) {
        CollectionRecordDialog(
            vn = p1,
            existingRecord = collectionRecord,
            onDismiss = { showCollectionDialog = false },
            onSave = { status, vote, notes, started, finished ->
                onSaveToCollection(p1, status, vote, notes, started, finished)
                showCollectionDialog = false
            },
            onRemove = {
                onRemoveFromCollection(p1.id)
                showCollectionDialog = false
            }
        )
    }

    // Fullscreen Screenshot Dialog
    fullscreenScreenshot?.let { shot ->
        val isShotExplicit = shot.isExplicit && settings.showCoverCensor
        var revealFullscreenShot by remember(shot.url) { mutableStateOf(false) }
        val shouldBlurFullscreen = isShotExplicit && !revealFullscreenShot

        Dialog(onDismissRequest = { fullscreenScreenshot = null }) {
            Surface(
                color = Color.Black.copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fullscreenScreenshot = null }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NsfwAsyncImage(
                        imageUrl = shot.url,
                        contentDescription = "Screenshot",
                        shouldBlur = shouldBlurFullscreen,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                    if (isShotExplicit) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { revealFullscreenShot = !revealFullscreenShot },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (shouldBlurFullscreen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle NSFW Screenshot",
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (shouldBlurFullscreen) "Reveal NSFW (18+)" else "Hide NSFW",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Character Detail Modal
    selectedCharacterModal?.let { char ->
        CharacterDetailDialog(
            character = char,
            showCoverCensor = settings.showCoverCensor,
            onDismiss = { selectedCharacterModal = null },
            onFilterVnsByTrait = { trait ->
                selectedCharacterModal = null
                onTraitClick(trait)
            },
            onFilterCharsByTrait = { trait ->
                selectedCharacterModal = null
                onTraitClick(trait)
            },
            onOpenVn = { targetId, targetTitle ->
                selectedCharacterModal = null
                if (targetId != vnId) {
                    onRelatedVnClick(VnDataPhase01(id = targetId, title = targetTitle))
                }
            }
        )
    }
}

@Composable
private fun VnDetailHeaderCard(
    p1: VnDataPhase01,
    p2: VnDataPhase02?,
    showCoverCensor: Boolean,
    collectionRecord: VnRecord?,
    onEditCollectionClick: () -> Unit
) {
    val theme = LocalAppThemeCode.current
    val isExplicit = p1.image?.isExplicit == true
    var revealCover by remember(p1.id) { mutableStateOf(false) }
    val shouldBlur = isExplicit && showCoverCensor && !revealCover
    val devStatusInfo = DevStatusData.get(p2?.devstatus)

    Surface(
        color = theme.primary.copy(alpha = 0.85f),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cover Image
                Box(
                    modifier = Modifier
                        .width(125.dp)
                        .height(182.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.tertiary.copy(alpha = 0.1f))
                        .clickable { if (isExplicit) revealCover = !revealCover }
                ) {
                    val url = p1.image?.url?.ifEmpty { p1.image.thumbnail }.orEmpty()
                    if (url.isNotBlank()) {
                        NsfwAsyncImage(
                            imageUrl = url,
                            contentDescription = p1.title,
                            shouldBlur = shouldBlur,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (isExplicit && showCoverCensor) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = if (shouldBlur) "NSFW 18+" else "Hide",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Metadata Summary
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = p1.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    if (!p2?.aliases.isNullOrEmpty()) {
                        Text(
                            text = p2!!.aliases.joinToString(", "),
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.tertiary.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Developer
                    val devText = p2?.developers?.joinToString(", ")?.ifBlank { "Unknown producer" } ?: "Loading..."
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = theme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = devText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Rating & Votes
                    val ratingDisplay = if (p1.rating > 0) {
                        val norm = if (p1.rating > 10.0) p1.rating / 10.0 else p1.rating
                        String.format(Locale.US, "%.2f", norm)
                    } else "N/A"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$ratingDisplay (${p1.votecount} votes)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.tertiary
                        )
                    }

                    // Release Date + Origin Flag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssetImage(
                            assetPath = LangData.getFlagPath(p1.olang),
                            contentDescription = p1.olang,
                            modifier = Modifier
                                .width(18.dp)
                                .height(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Released: ${p1.released}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary
                        )
                    }

                    // Playtime Length
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = theme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatVnLength(p2?.lengthMinutes, p1.length),
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary
                        )
                    }

                    // Development Status Chip
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(devStatusInfo.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = devStatusInfo.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add / Edit Collection Button
            val statusCode = collectionRecord?.let { CollectionStatusCode.fromName(it.status) }
            Button(
                onClick = onEditCollectionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = statusCode?.color ?: theme.secondary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vn_detail_collection_button")
            ) {
                if (statusCode != null && collectionRecord != null) {
                    AssetImage(
                        assetPath = statusCode.imagePath,
                        contentDescription = statusCode.displayTitle,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val voteStr = if (collectionRecord.vote > 0) " • Voted ${collectionRecord.vote}/10" else ""
                    Text(
                        text = "In Collection: ${statusCode.displayTitle}$voteStr",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.LibraryAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to My Collection", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VnDetailGeneralSection(
    p1: VnDataPhase01,
    p2: VnDataPhase02?,
    quotes: List<VnQuote>,
    reviews: List<VnReview>,
    collectionRecord: VnRecord?,
    showCoverCensor: Boolean,
    showSpoilersInTags: Boolean,
    onToggleSpoilers: () -> Unit,
    onTagClick: (VnTag) -> Unit,
    onScreenshotClick: (VnImage) -> Unit,
    onOpenReviewsTab: () -> Unit,
    onEditCollectionNote: () -> Unit
) {
    val theme = LocalAppThemeCode.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Card
        Surface(
            color = theme.primary.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                BbCodeText(
                    rawText = p1.description,
                    textColor = theme.tertiary,
                    linkColor = theme.secondary
                )
            }
        }

        // Screenshots Carousel
        if (!p2?.screenshots.isNullOrEmpty()) {
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Screenshots (${p2!!.screenshots.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(p2.screenshots, key = { it.url }) { shot ->
                            val isShotExplicit = shot.isExplicit && showCoverCensor
                            var revealShot by remember(shot.url) { mutableStateOf(false) }
                            val blurShot = isShotExplicit && !revealShot
                            Box(
                                modifier = Modifier
                                    .width(210.dp)
                                    .height(122.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onScreenshotClick(shot) }
                            ) {
                                NsfwAsyncImage(
                                    imageUrl = shot.url,
                                    contentDescription = "Screenshot",
                                    shouldBlur = blurShot,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isShotExplicit) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(6.dp),
                                        onClick = { revealShot = !revealShot },
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (blurShot) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle NSFW Screenshot",
                                                tint = Color(0xFFFF8A80),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (blurShot) "NSFW 18+" else "Hide",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
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

        // Tags Section
        if (!p2?.tags.isNullOrEmpty()) {
            val filteredTags = remember(p2!!.tags, showSpoilersInTags) {
                p2.tags
                    .filter { showSpoilersInTags || it.spoiler < 0.5 }
                    .sortedByDescending { it.rating }
            }
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tags (${filteredTags.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onToggleSpoilers) {
                            Text(
                                text = if (showSpoilersInTags) "Hide Spoilers" else "Show Spoilers",
                                color = theme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredTags.forEach { tg ->
                            AssistChip(
                                onClick = { onTagClick(tg) },
                                label = {
                                    val r = String.format(Locale.US, "%.1f", tg.rating)
                                    Text("${tg.name} ($r)")
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = theme.tertiary.copy(alpha = 0.1f),
                                    labelColor = theme.tertiary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Memorable Quotes Card
        if (quotes.isNotEmpty()) {
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vn_detail_quotes_section")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = theme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Memorable Quotes (${quotes.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    quotes.forEachIndexed { idx, q ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = theme.tertiary.copy(alpha = 0.12f),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                        Text(
                            text = "“${q.quote}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = theme.tertiary
                        )
                        if (q.characterName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val orig = if (q.characterOriginal.isNotBlank()) " (${q.characterOriginal})" else ""
                            Text(
                                text = "— ${q.characterName}$orig",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Community Reviews & Personal Notes Summary Card
        Surface(
            color = theme.primary.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vn_detail_reviews_preview_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = theme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (reviews.isNotEmpty()) "Reviews (${reviews.size})" else "Reviews & Notes",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onOpenReviewsTab,
                        modifier = Modifier.testTag("vn_detail_open_reviews_button")
                    ) {
                        Text(
                            text = if (reviews.isNotEmpty()) "View all (${reviews.size})" else "Open Reviews Tab",
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = theme.secondary
                        )
                    }
                }

                if (!collectionRecord?.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = theme.secondary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "My Personal Review / Note",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = collectionRecord!!.notes!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.tertiary
                            )
                        }
                    }
                }

                val topReview = reviews.firstOrNull()
                if (topReview != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${topReview.reviewType} review by ${topReview.username}",
                            style = MaterialTheme.typography.labelLarge,
                            color = theme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        if (topReview.rating.isNotBlank()) {
                            Surface(
                                color = theme.secondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "★ ${topReview.rating}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = topReview.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary.copy(alpha = 0.9f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onEditCollectionNote,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Write Personal Note / Review")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VnDetailReviewsSection(
    vnId: String,
    reviews: List<VnReview>,
    isLoadingReviews: Boolean,
    expandingReviewIds: Set<String>,
    collectionRecord: VnRecord?,
    onEditCollectionNote: () -> Unit,
    onExpandFullReview: (String) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val context = LocalContext.current
    var selectedTypeFilter by remember { mutableStateOf("All") }

    val filteredReviews = remember(reviews, selectedTypeFilter) {
        if (selectedTypeFilter == "All") {
            reviews
        } else {
            reviews.filter { it.reviewType.equals(selectedTypeFilter, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("vn_detail_reviews_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Personal Collection Review / Note Card
        Surface(
            color = theme.primary.copy(alpha = 0.88f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = theme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "My Personal Review & Notes",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saved in your collection & synced to VNDB ulist notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.tertiary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onEditCollectionNote,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("vn_detail_edit_personal_review_button")
                    ) {
                        Text(
                            text = if (collectionRecord?.notes.isNullOrBlank()) "Write" else "Edit",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!collectionRecord?.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (collectionRecord!!.vote > 0) {
                        Text(
                            text = "My Vote: ★ ${collectionRecord.vote}/10",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = collectionRecord.notes!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary
                    )
                }
            }
        }

        // Header + Filter pills for VNDB Community Reviews
        Surface(
            color = theme.primary.copy(alpha = 0.88f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VNDB Community Reviews (${reviews.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Published reviews from vndb.org/$vnId/reviews",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.tertiary.copy(alpha = 0.7f)
                        )
                    }
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://vndb.org/$vnId/reviews"))
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open all reviews on VNDB",
                            tint = theme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VNDB", color = theme.secondary, fontWeight = FontWeight.Bold)
                    }
                }

                if (reviews.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Long", "Medium", "Short").forEach { typeOpt ->
                            val count = if (typeOpt == "All") {
                                reviews.size
                            } else {
                                reviews.count { it.reviewType.equals(typeOpt, ignoreCase = true) }
                            }
                            if (typeOpt == "All" || count > 0) {
                                FilterChip(
                                    selected = selectedTypeFilter == typeOpt,
                                    onClick = { selectedTypeFilter = typeOpt },
                                    label = { Text("$typeOpt ($count)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = theme.secondary.copy(alpha = 0.25f),
                                        selectedLabelColor = theme.tertiary,
                                        labelColor = theme.tertiary.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isLoadingReviews) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = theme.secondary)
            }
        } else if (filteredReviews.isEmpty()) {
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No community reviews found on VNDB for this visual novel yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.tertiary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            filteredReviews.forEach { rev ->
                val isExpanding = expandingReviewIds.contains(rev.id)
                Surface(
                    color = theme.primary.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vn_review_card_${rev.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top metadata row: Reviewer + Type + Rating
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = if (rev.reviewType.equals("Long", ignoreCase = true)) {
                                        theme.secondary.copy(alpha = 0.22f)
                                    } else {
                                        theme.tertiary.copy(alpha = 0.14f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = rev.reviewType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (rev.reviewType.equals("Long", ignoreCase = true)) {
                                            theme.secondary
                                        } else {
                                            theme.tertiary
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = rev.username,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = theme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (rev.date.isNotBlank()) {
                                        Text(
                                            text = "Published on ${rev.date}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.tertiary.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }

                            if (rev.rating.isNotBlank()) {
                                Surface(
                                    color = theme.secondary.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = rev.rating,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = theme.tertiary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Review body text
                        Text(
                            text = rev.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary.copy(alpha = 0.94f)
                        )

                        // "Read Full Review" button if truncated on overview page
                        if (rev.hasReadMore) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onExpandFullReview(rev.id) },
                                enabled = !isExpanding,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary),
                                modifier = Modifier.testTag("vn_review_expand_${rev.id}")
                            ) {
                                if (isExpanding) {
                                    CircularProgressIndicator(
                                        color = theme.secondary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading full review...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Read Full Review", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Footer: Helpfulness points + comments + open review on VNDB
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = theme.tertiary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (rev.points.isNotBlank()) {
                                    Text(
                                        text = "▲ ${rev.points}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (rev.commentsCount.isNotBlank()) {
                                    Text(
                                        text = "💬 ${rev.commentsCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.tertiary.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://vndb.org/${rev.id}"))
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Thread (${rev.id})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open review on VNDB",
                                    tint = theme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VnDetailReleasesSection(
    p1: VnDataPhase01,
    p2: VnDataPhase02?
) {
    val theme = LocalAppThemeCode.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Minimum Age Card
        Surface(
            color = theme.primary.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Age Rating & Release Info",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val ageStr = if (p2?.minage.isNullOrEmpty()) {
                    "All Ages / Unspecified"
                } else {
                    p2!!.minage.joinToString(" - ") { if (it == 0) "All Ages" else "${it}+" }
                }
                Text(
                    text = "Minimum Age: $ageStr",
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "First Release Date: ${p1.released}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.tertiary
                )
            }
        }

        // Available Languages & MTL
        Surface(
            color = theme.primary.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Available Languages",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                val langs = p2?.languages.orEmpty().ifEmpty { listOf(p1.olang) }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    langs.forEach { code ->
                        val isMtl = p2?.mtl?.contains(code) == true
                        Surface(
                            color = theme.tertiary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                AssetImage(
                                    assetPath = LangData.getFlagPath(code),
                                    contentDescription = code,
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LangData.getFullLanguage(code) + (if (isMtl) " (MTL)" else ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Supported Platforms
        if (!p2?.platforms.isNullOrEmpty()) {
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Platforms",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        p2!!.platforms.forEach { platCode ->
                            Surface(
                                color = theme.tertiary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    AssetImage(
                                        assetPath = PlatfData.getImgPath(platCode),
                                        contentDescription = platCode,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = PlatfData.DEFINED_CODES[platCode] ?: platCode,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = theme.tertiary
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

@Composable
private fun VnDetailRelationsSection(
    p3: VnDataPhase03?,
    relatedVns: List<VnDataPhase01>,
    showCoverCensor: Boolean,
    rawRecords: Map<String, VnRecord>,
    onRelatedVnClick: (VnDataPhase01) -> Unit
) {
    val theme = LocalAppThemeCode.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // External Links
        if (!p3?.extlinks.isNullOrEmpty()) {
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "External Links",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    p3!!.extlinks.forEach { url ->
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = theme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = url,
                                color = theme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Related Visual Novels
        Surface(
            color = theme.primary.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Related Visual Novels (${relatedVns.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (relatedVns.isEmpty()) {
                    Text(
                        text = "No related visual novels listed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary.copy(alpha = 0.75f)
                    )
                } else {
                    val relationMap = remember(p3?.relations) {
                        p3?.relations?.associateBy { it.id }.orEmpty()
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(relatedVns, key = { it.id }) { relVn ->
                            val relMeta = relationMap[relVn.id]
                            Column(modifier = Modifier.width(155.dp)) {
                                if (relMeta != null) {
                                    Text(
                                        text = formatRelationName(relMeta.relation),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                VnItemGridCard(
                                    vn = relVn,
                                    labelCode = SortableCode.RATING,
                                    showCoverCensorGlobal = showCoverCensor,
                                    collectionRecord = rawRecords[relVn.id],
                                    onClick = { onRelatedVnClick(relVn) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
