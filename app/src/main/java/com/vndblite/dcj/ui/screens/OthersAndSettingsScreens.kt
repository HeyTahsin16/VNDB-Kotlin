package com.vndblite.dcj.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.data.model.*
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.VnItemGridCard
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import com.vndblite.dcj.ui.viewmodel.RandomVnUiState
import com.vndblite.dcj.ui.viewmodel.SyncUiState

@Composable
fun OthersScreen(
    syncState: SyncUiState,
    settings: SettingsState,
    onOpenSettings: () -> Unit,
    onOpenAuthSync: () -> Unit,
    onOpenRandomVn: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val theme = LocalAppThemeCode.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("others_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = theme.primary.copy(alpha = 0.86f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(18.dp)
                ) {
                    AssetImage(
                        assetPath = "assets/images/icon/app_icon.png",
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "VNDB Lite",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Theme: ${settings.themeCode.themeName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (syncState.userIdentity != null) {
                                "VNDB Account: ${syncState.userIdentity.username}"
                            } else {
                                "Not signed in to VNDB"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.secondary
                        )
                    }
                }
            }
        }

        item {
            OthersMenuTile(
                icon = Icons.Default.Sync,
                title = "VNDB Account & Sync",
                subtitle = if (syncState.userIdentity != null) {
                    "Connected as ${syncState.userIdentity.username} • Tap to manage sync"
                } else {
                    "Authenticate with your VNDB Personal Access Token"
                },
                onClick = onOpenAuthSync,
                testTag = "others_auth_sync_tile"
            )
        }

        item {
            OthersMenuTile(
                icon = Icons.Default.Casino,
                title = "Random High-Rated VN",
                subtitle = "Discover a random acclaimed visual novel (rating 8.0+)",
                onClick = onOpenRandomVn,
                testTag = "others_random_vn_tile"
            )
        }

        item {
            OthersMenuTile(
                icon = Icons.Default.Settings,
                title = "Settings & Themes",
                subtitle = "Customize 11 visual novel themes, font scale, cover censor & layout",
                onClick = onOpenSettings,
                testTag = "others_settings_tile"
            )
        }

        item {
            OthersMenuTile(
                icon = Icons.Default.Info,
                title = "About VNDB Lite",
                subtitle = "Version info, credits, source code & license",
                onClick = onOpenAbout,
                testTag = "others_about_tile"
            )
        }

        item {
            OthersMenuTile(
                icon = Icons.Default.BugReport,
                title = "Report a Bug / Feedback",
                subtitle = "Open issue tracker on GitHub",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Daniel-C-J/vndb-lite/issues"))
                        )
                    }
                },
                testTag = "others_bug_report_tile"
            )
        }
    }
}

@Composable
private fun OthersMenuTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    val theme = LocalAppThemeCode.current
    Surface(
        color = theme.primary.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = theme.secondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = theme.secondary)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.tertiary.copy(alpha = 0.78f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = theme.tertiary.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
    onBack: () -> Unit,
    onUpdateSettings: ((SettingsState) -> SettingsState) -> Unit,
    onResetSettings: () -> Unit,
    onClearCollection: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalAppThemeCode.current
    var showClearCollectionDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }
    var showTabOrderDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings & Themes", color = theme.tertiary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.tertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.primary.copy(alpha = 0.90f))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Visual Novel Theme Picker (11 Themes)
            item {
                Surface(
                    color = theme.primary.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Visual Novel Theme (${ThemeCode.entries.size} Presets)",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select your favorite classic visual novel background and color scheme.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.tertiary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeCode.entries.forEach { candidate ->
                                val isCurrent = settings.themeCode == candidate
                                Surface(
                                    color = candidate.primary.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isCurrent) 2.5.dp else 1.dp,
                                        color = if (isCurrent) candidate.secondary else candidate.tertiary.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdateSettings { it.copy(themeCode = candidate) }
                                        }
                                        .testTag("theme_option_${candidate.code}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AssetImage(
                                                assetPath = candidate.backgroundImgPath,
                                                contentDescription = candidate.themeName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = candidate.themeName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = candidate.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (candidate.isDark) "Dark Palette" else "Light Palette",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = candidate.tertiary.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Active Theme",
                                                tint = candidate.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Display & Appearance Preferences
            item {
                Surface(
                    color = theme.primary.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Display & Content",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        // Explicit Cover Censor Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Blur Explicit (18+) Covers & Screenshots",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Automatically blur images flagged for sexual or violent content",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.tertiary.copy(alpha = 0.75f)
                                )
                            }
                            Switch(
                                checked = settings.showCoverCensor,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { it.copy(showCoverCensor = checked) }
                                },
                                modifier = Modifier.testTag("switch_cover_censor")
                            )
                        }

                        HorizontalDivider(color = theme.tertiary.copy(alpha = 0.15f))

                        // Show VNDB Radar Chart on Home
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show VNDB Stats Chart on Home",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Display the live VNDB database radar chart at the bottom of Home",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.tertiary.copy(alpha = 0.75f)
                                )
                            }
                            Switch(
                                checked = settings.showChart,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { it.copy(showChart = checked) }
                                },
                                modifier = Modifier.testTag("switch_show_chart")
                            )
                        }

                        HorizontalDivider(color = theme.tertiary.copy(alpha = 0.15f))

                        // Font Size Offset Slider
                        Text(
                            text = "Custom Font Size Offset: ${settings.fontSizeOffset.toInt()} sp",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = settings.fontSizeOffset,
                            onValueChange = { v -> onUpdateSettings { it.copy(fontSizeOffset = v) } },
                            valueRange = -2f..4f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.secondary,
                                activeTrackColor = theme.secondary
                            )
                        )

                        HorizontalDivider(color = theme.tertiary.copy(alpha = 0.15f))

                        // Max Home Preview Items
                        Text(
                            text = "Max Home Preview Items: ${settings.maxPreviewItem}",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = settings.maxPreviewItem.toFloat(),
                            onValueChange = { v -> onUpdateSettings { it.copy(maxPreviewItem = v.toInt()) } },
                            valueRange = 6f..30f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.secondary,
                                activeTrackColor = theme.secondary
                            )
                        )

                        HorizontalDivider(color = theme.tertiary.copy(alpha = 0.15f))

                        // Grid Columns Portrait & Landscape
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grid Columns (Portrait)",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.tertiary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 3).forEach { cols ->
                                    val selected = settings.maxItemPerRowPortrait == cols
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onUpdateSettings { it.copy(maxItemPerRowPortrait = cols) } },
                                        label = {
                                            Text(
                                                "$cols cols",
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(selected, theme)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grid Columns (Landscape)",
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.tertiary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 4, 5).forEach { cols ->
                                    val selected = settings.maxItemPerRowLandscape == cols
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onUpdateSettings { it.copy(maxItemPerRowLandscape = cols) } },
                                        label = {
                                            Text(
                                                "$cols cols",
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = vndbFilterChipColors(theme),
                                        border = vndbFilterChipBorder(selected, theme)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = theme.tertiary.copy(alpha = 0.15f))

                        // Reorder Collection Tabs
                        OutlinedButton(
                            onClick = { showTabOrderDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Reorder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Customize Collection Status Tab Order")
                        }
                    }
                }
            }

            // 3. Data & Reset Management
            item {
                Surface(
                    color = theme.primary.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = { showResetSettingsDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.tertiary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All Settings to Default")
                        }

                        Button(
                            onClick = { showClearCollectionDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_local_collection_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Local Collection", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Collection Tab Reorder Dialog
    if (showTabOrderDialog) {
        var currentOrder by remember {
            mutableStateOf(settings.collectionStatusTabArrangement)
        }
        AlertDialog(
            onDismissRequest = { showTabOrderDialog = false },
            containerColor = theme.primary,
            titleContentColor = theme.secondary,
            textContentColor = theme.tertiary,
            title = { Text("Collection Tab Order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentOrder.forEachIndexed { index, statusName ->
                        val status = CollectionStatusCode.fromName(statusName)
                        Surface(
                            color = theme.tertiary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AssetImage(
                                        assetPath = status.imagePath,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(status.displayTitle, fontWeight = FontWeight.SemiBold)
                                }
                                Row {
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            val list = currentOrder.toMutableList()
                                            val tmp = list[index - 1]
                                            list[index - 1] = list[index]
                                            list[index] = tmp
                                            currentOrder = list
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = theme.tertiary)
                                    }
                                    IconButton(
                                        enabled = index < currentOrder.lastIndex,
                                        onClick = {
                                            val list = currentOrder.toMutableList()
                                            val tmp = list[index + 1]
                                            list[index + 1] = list[index]
                                            list[index] = tmp
                                            currentOrder = list
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = theme.tertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSettings { it.copy(collectionStatusTabArrangement = currentOrder) }
                        showTabOrderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = Color.Black)
                ) {
                    Text("Save Order", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTabOrderDialog = false }) {
                    Text("Cancel", color = theme.tertiary)
                }
            }
        )
    }

    // Clear Collection Dialog
    if (showClearCollectionDialog) {
        var deleteOnNextSync by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showClearCollectionDialog = false },
            containerColor = theme.primary,
            titleContentColor = Color(0xFFFF8A80),
            textContentColor = theme.tertiary,
            title = { Text("Clear Local Collection?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will remove all saved visual novel records from this device.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteOnNextSync,
                            onCheckedChange = { deleteOnNextSync = it }
                        )
                        Text(
                            text = "Also delete from VNDB cloud on next sync",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCollection(deleteOnNextSync)
                        showClearCollectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCollectionDialog = false }) {
                    Text("Cancel", color = theme.tertiary)
                }
            }
        )
    }

    // Reset Settings Dialog
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            containerColor = theme.primary,
            titleContentColor = theme.secondary,
            textContentColor = theme.tertiary,
            title = { Text("Reset Settings?") },
            text = { Text("Restore theme, font size, and layout preferences back to default?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSettings()
                        showResetSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = Color.Black)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text("Cancel", color = theme.tertiary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    syncState: SyncUiState,
    onBack: () -> Unit,
    onSubmitToken: (String) -> Unit,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler(onBack = onBack)

    val theme = LocalAppThemeCode.current
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf(syncState.userIdentity?.authToken ?: "") }
    var showToken by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("VNDB Cloud Sync", color = theme.tertiary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("auth_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.tertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.primary.copy(alpha = 0.90f))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = theme.primary.copy(alpha = 0.88f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssetImage(
                            assetPath = "assets/images/icon/app_icon.png",
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Synchronize with vndb.org",
                                style = MaterialTheme.typography.titleLarge,
                                color = theme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Two-way ulist sync via VNDB API v2 (/kana/authinfo & /kana/ulist)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.tertiary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (syncState.userIdentity != null) {
                        Surface(
                            color = theme.secondary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Connected Account: ${syncState.userIdentity.username} (${syncState.userIdentity.id})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = theme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (syncState.isSynced) "Status: Synchronized" else "Status: Ready to sync",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.tertiary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("VNDB Personal Access Token") },
                        placeholder = { Text("Paste token from vndb.org -> My Profile -> Applications") },
                        singleLine = true,
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle token visibility",
                                    tint = theme.tertiary
                                )
                            }
                        },
                        colors = vndbOutlinedTextFieldColors(theme),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vndb_token_input")
                    )

                    if (!syncState.statusMessage.isNullOrBlank()) {
                        Text(
                            text = syncState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (syncState.isError) Color(0xFFFF8A80) else theme.secondary
                        )
                    }

                    Button(
                        onClick = {
                            if (syncState.userIdentity != null && tokenInput.trim() == syncState.userIdentity.authToken) {
                                onSyncNow()
                            } else {
                                onSubmitToken(tokenInput)
                            }
                        },
                        enabled = tokenInput.isNotBlank() && !syncState.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("authenticate_and_sync_button")
                    ) {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (syncState.userIdentity != null) "Sync Collection Now" else "Verify Token & Sync",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (syncState.userIdentity != null) {
                        OutlinedButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect Account")
                        }
                    }
                }
            }

            // Instructions with Tutorial Images
            Surface(
                color = theme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "How to get your VNDB Token",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://vndb.org")))
                                }
                            }
                        ) {
                            Text("Open vndb.org", color = theme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = theme.secondary, modifier = Modifier.size(15.dp))
                        }
                    }

                    Text(
                        text = "1. Sign in to vndb.org and open 'My Profile' → 'Applications'.\n" +
                            "2. Click 'New token', enable 'listread' and 'listwrite' permissions, and copy the token.\n" +
                            "3. Paste the token above to synchronize your visual novel collection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary.copy(alpha = 0.85f)
                    )

                    listOf(
                        "assets/images/tutorial/tutorial1.png",
                        "assets/images/tutorial/tutorial2.png",
                        "assets/images/tutorial/tutorial3.png"
                    ).forEach { tutPath ->
                        AssetImage(
                            assetPath = tutPath,
                            contentDescription = "Tutorial Step",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val theme = LocalAppThemeCode.current
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("About VNDB Lite", color = theme.tertiary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("about_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.tertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.primary.copy(alpha = 0.90f))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = theme.primary.copy(alpha = 0.88f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    AssetImage(
                        assetPath = "assets/images/icon/app_icon.png",
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VNDB Lite",
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 2.1.0 • Kotlin & Jetpack Compose Edition",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.tertiary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A fast, free, and open-source mobile client for The Visual Novel Database (vndb.org), featuring Character Traits search, local offline caching, 11 classic visual novel themes, and two-way cloud synchronization.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.tertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Author: Daniel C.J • License: GPL-3.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.secondary
                    )
                }
            }

            Surface(
                color = theme.primary.copy(alpha = 0.86f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Project Links",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Daniel-C-J/vndb-lite")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssetImage(
                            assetPath = "assets/images/contact/github.png",
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GitHub Repository", color = theme.tertiary)
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://codeberg.org/Daniel-C-J/vndb-lite")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssetImage(
                            assetPath = "assets/images/contact/codeberg.png",
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Codeberg Mirror", color = theme.tertiary)
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://vndb.org")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = theme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Official vndb.org Website", color = theme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
fun RandomVnDialog(
    randomState: RandomVnUiState,
    showCoverCensor: Boolean,
    rawRecords: Map<String, VnRecord>,
    onDismiss: () -> Unit,
    onRollAgain: () -> Unit,
    onOpenDetail: (VnDataPhase01) -> Unit
) {
    val theme = LocalAppThemeCode.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.primary,
        titleContentColor = theme.secondary,
        textContentColor = theme.tertiary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = theme.secondary)
                Text("Random High-Rated VN", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    randomState.isLoading -> {
                        CircularProgressIndicator(color = theme.secondary)
                    }
                    randomState.error != null -> {
                        Text(randomState.error, color = Color(0xFFFF8A80))
                    }
                    randomState.vn != null -> {
                        val vn = randomState.vn
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VnItemGridCard(
                                vn = vn,
                                labelCode = SortableCode.RATING,
                                showCoverCensorGlobal = showCoverCensor,
                                collectionRecord = rawRecords[vn.id],
                                onClick = { onOpenDetail(vn) },
                                modifier = Modifier.width(190.dp)
                            )
                            Text(
                                text = "Tap cover or 'Read Details' to explore this visual novel.",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.tertiary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRollAgain,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary),
                    modifier = Modifier.testTag("random_vn_reroll_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Another")
                }
                if (randomState.vn != null) {
                    Button(
                        onClick = { onOpenDetail(randomState.vn) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("random_vn_open_button")
                    ) {
                        Text("Read Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = theme.tertiary)
            }
        }
    )
}
