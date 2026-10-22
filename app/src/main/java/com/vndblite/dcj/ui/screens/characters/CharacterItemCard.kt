package com.vndblite.dcj.ui.screens.characters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.format.displayLabel
import com.vndblite.dcj.ui.format.formatStatsLine
import com.vndblite.dcj.ui.format.labelResId
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterItemCard(
    character: Character,
    showCoverCensor: Boolean,
    isNsfwRevealed: Boolean,
    onToggleNsfwReveal: () -> Unit,
    onTraitClick: (Trait) -> Unit,
    onVnClick: (VnSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    val isExplicit = character.image?.isExplicit == true
    val shouldBlur = isExplicit && showCoverCensor && !isNsfwRevealed

    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("character_card_${character.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CharacterPortraitBox(
                imageUrl = character.image?.previewUrl.orEmpty(),
                isExplicit = isExplicit,
                showCoverCensor = showCoverCensor,
                shouldBlur = shouldBlur,
                onToggleNsfwReveal = onToggleNsfwReveal
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = spec.accentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    character.primaryRole?.let { role ->
                        Surface(
                            color = spec.accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(role.labelResId()),
                                style = MaterialTheme.typography.labelSmall,
                                color = spec.accentColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                character.originalName?.takeIf { it.isNotBlank() }?.let { orig ->
                    Text(
                        text = orig,
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.onSurfaceColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                character.formatStatsLine()?.let { statsLine ->
                    Text(
                        text = statsLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.badgeNeutral,
                        fontWeight = FontWeight.Medium
                    )
                }

                character.vns.firstOrNull()?.let { appearance ->
                    Text(
                        text = appearance.vnTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.accentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(role = Role.Button) {
                            onVnClick(VnSummary(id = appearance.vnId, title = appearance.vnTitle))
                        }
                    )
                }

                character.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    BbCodeText(
                        rawText = desc,
                        textColor = spec.onSurfaceColor.copy(alpha = 0.88f),
                        linkColor = spec.accentColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val nonSpoilerTraits = character.traits.filter { it.spoilerLevel == 0 }.take(6)
                if (nonSpoilerTraits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        nonSpoilerTraits.forEach { trait ->
                            AssistChip(
                                onClick = { onTraitClick(trait) },
                                label = {
                                    Text(
                                        text = trait.displayLabel(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = spec.onSurfaceColor.copy(alpha = 0.08f),
                                    labelColor = spec.onSurfaceColor
                                ),
                                border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterPortraitBox(
    imageUrl: String,
    isExplicit: Boolean,
    showCoverCensor: Boolean,
    shouldBlur: Boolean,
    onToggleNsfwReveal: () -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    Box(
        modifier = Modifier
            .width(86.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        if (imageUrl.isNotBlank()) {
            NsfwAsyncImage(
                imageUrl = imageUrl,
                contentDescription = null,
                shouldBlur = shouldBlur,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Surface(
                color = spec.onSurfaceColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = spec.onSurfaceColor.copy(alpha = 0.45f)
                    )
                }
            }
        }

        if (isExplicit && showCoverCensor) {
            Surface(
                color = semantic.nsfwBadgeContainer,
                shape = RoundedCornerShape(6.dp),
                onClick = onToggleNsfwReveal,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (shouldBlur) R.string.card_nsfw_toggle_cd_hidden
                        else R.string.card_nsfw_toggle_cd_revealed
                    ),
                    tint = semantic.nsfwBadgeTint,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(14.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterItemCardPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        CharacterItemCard(
            character = PreviewData.sampleCharacter,
            showCoverCensor = true,
            isNsfwRevealed = false,
            onToggleNsfwReveal = {},
            onTraitClick = {},
            onVnClick = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}
