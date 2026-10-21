package com.vndblite.dcj.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.core.AppInfo
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.PopularTraitPreset
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.AssetImage
import com.vndblite.dcj.ui.components.AssetPaths
import com.vndblite.dcj.ui.components.SectionLoadState
import com.vndblite.dcj.ui.components.SectionStateContainer
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@Composable
internal fun HomeTopHeader() {
    val spec = VndbThemeTokens.spec
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        AssetImage(
            assetPath = AssetPaths.APP_ICON_TRANSPARENT,
            contentDescription = null,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = AppInfo.APP_NAME,
                style = MaterialTheme.typography.displayMedium,
                color = spec.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.home_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = spec.onSurfaceColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
internal fun HomeDiscoveryBanner(
    onOpenTraitFinder: (Trait?) -> Unit,
    onOpenRandomVn: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.84f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = spec.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_character_trait_finder_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = spec.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onOpenTraitFinder(null) },
                        border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("home_trait_finder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = spec.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.search_mode_characters),
                            style = MaterialTheme.typography.labelMedium,
                            color = spec.onSurfaceColor
                        )
                    }

                    Button(
                        onClick = onOpenRandomVn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.accentColor,
                            contentColor = semantic.onAccentButton
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("home_random_vn_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_random_vn_action),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_character_trait_finder_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = spec.onSurfaceColor.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PopularTraitPreset.entries, key = { it.traitId }) { preset ->
                    AssistChip(
                        onClick = { onOpenTraitFinder(preset.toTrait()) },
                        label = {
                            Text(
                                text = preset.defaultName,
                                style = MaterialTheme.typography.labelMedium,
                                color = spec.onSurfaceColor
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = spec.onSurfaceColor.copy(alpha = 0.1f),
                            labelColor = spec.onSurfaceColor
                        ),
                        border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("home_trait_preset_${preset.traitId}")
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeQuotesBanner(
    quotesState: SectionLoadState<List<Quote>>,
    displayedQuote: Quote?,
    onShuffle: () -> Unit,
    onRetry: () -> Unit,
    onOpenVn: (VnSummary) -> Unit
) {
    val spec = VndbThemeTokens.spec
    Surface(
        color = spec.surfaceColor.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = displayedQuote?.vn != null) {
                displayedQuote?.vn?.let(onOpenVn)
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = spec.accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.home_section_quotes),
                        style = MaterialTheme.typography.titleMedium,
                        color = spec.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.home_shuffle_quotes_cd),
                        tint = spec.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            SectionStateContainer(
                state = quotesState,
                emptyMessage = UiText.StringResource(R.string.home_quotes_empty),
                onRetry = onRetry
            ) {
                if (displayedQuote != null) {
                    Text(
                        text = "\"${displayedQuote.text}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = spec.onSurfaceColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val speaker = displayedQuote.characterName
                        ?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.home_quote_speaker_unknown)
                    val vnTitle = displayedQuote.vn?.title.orEmpty()
                    Text(
                        text = if (vnTitle.isNotBlank()) {
                            stringResource(R.string.home_quote_from_vn, speaker, vnTitle)
                        } else {
                            stringResource(R.string.home_quote_speaker_only, speaker)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.accentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
