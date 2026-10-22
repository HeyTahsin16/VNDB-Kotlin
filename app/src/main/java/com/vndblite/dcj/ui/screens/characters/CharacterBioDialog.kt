package com.vndblite.dcj.ui.screens.characters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.VnFormatters
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.vndbFilterChipBorder
import com.vndblite.dcj.ui.theme.vndbFilterChipColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterBioDialog(
    character: Character,
    showCoverCensor: Boolean,
    isNsfwRevealed: Boolean,
    onToggleNsfwReveal: () -> Unit,
    onSelectTrait: ((Trait) -> Unit)? = null,
    onOpenVnDetail: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    val isExplicit = character.image?.isExplicit == true
    val shouldBlur = isExplicit && showCoverCensor && !isNsfwRevealed

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeSpec.surfaceColor,
        titleContentColor = themeSpec.contentColor,
        textContentColor = themeSpec.contentColor,
        title = {
            Column {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeSpec.accentColor
                )
                character.originalName?.takeIf { it.isNotBlank() }?.let { orig ->
                    Text(
                        text = orig,
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CharacterBioHeaderRow(
                    character = character,
                    shouldBlur = shouldBlur,
                    showNsfwToggle = isExplicit && showCoverCensor,
                    onToggleNsfwReveal = onToggleNsfwReveal
                )

                val desc = character.description?.takeIf { it.isNotBlank() }
                if (desc != null) {
                    BbCodeText(rawText = desc)
                } else {
                    Text(
                        text = stringResource(R.string.char_bio_no_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeSpec.contentColor.copy(alpha = 0.7f)
                    )
                }

                if (character.vns.isNotEmpty()) {
                    HorizontalDivider(color = themeSpec.contentColor.copy(alpha = 0.2f))
                    Text(
                        text = stringResource(R.string.char_bio_appearances),
                        style = MaterialTheme.typography.titleMedium,
                        color = themeSpec.accentColor
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        character.vns.forEach { appearance ->
                            Surface(
                                color = themeSpec.contentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { role = Role.Button }
                                    .clickable {
                                        onDismiss()
                                        onOpenVnDetail(appearance.vnId)
                                    }
                                    .testTag("char_bio_vn_${appearance.vnId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = appearance.vnTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeSpec.contentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(appearance.role.labelRes()),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = themeSpec.accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                if (character.traits.isNotEmpty()) {
                    HorizontalDivider(color = themeSpec.contentColor.copy(alpha = 0.2f))
                    Text(
                        text = stringResource(R.string.char_bio_traits),
                        style = MaterialTheme.typography.titleMedium,
                        color = themeSpec.accentColor
                    )
                    val grouped = character.traits.groupBy { it.groupName?.takeIf { g -> g.isNotBlank() } ?: "" }
                    grouped.forEach { (groupName, groupTraits) ->
                        if (groupName.isNotEmpty()) {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.labelLarge,
                                color = themeSpec.accentColor.copy(alpha = 0.9f)
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            groupTraits.forEach { trait ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (onSelectTrait != null) {
                                            onDismiss()
                                            onSelectTrait(trait)
                                        }
                                    },
                                    label = { Text(trait.name) },
                                    colors = vndbFilterChipColors(),
                                    border = vndbFilterChipBorder(selected = false),
                                    modifier = Modifier.testTag("char_bio_trait_${trait.id}")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("char_bio_close_button")
            ) {
                Text(
                    text = stringResource(R.string.action_close),
                    color = themeSpec.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun CharacterBioHeaderRow(
    character: Character,
    shouldBlur: Boolean,
    showNsfwToggle: Boolean,
    onToggleNsfwReveal: () -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        val imageUrl = character.image?.previewUrl.orEmpty()
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 144.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (imageUrl.isNotBlank()) {
                NsfwAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = character.name,
                    shouldBlur = shouldBlur,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 104.dp, height = 144.dp)
                )
            } else {
                Surface(
                    color = themeSpec.contentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(width = 104.dp, height = 144.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = themeSpec.contentColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            if (showNsfwToggle) {
                Surface(
                    color = ColorTokens.ScrimDarkLight,
                    shape = RoundedCornerShape(6.dp),
                    onClick = onToggleNsfwReveal,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (shouldBlur) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.nsfw_toggle_cd),
                            tint = ColorTokens.NsfwWarningTint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (shouldBlur) {
                                stringResource(R.string.nsfw_badge_reveal)
                            } else {
                                stringResource(R.string.nsfw_badge_hide)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTokens.CardOverlayText
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            character.sex?.let { sex ->
                Text(
                    text = stringResource(sex.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = themeSpec.accentColor
                )
            }
            character.birthday?.let { bday ->
                Text(
                    text = stringResource(R.string.char_bio_birthday, bday.month, bday.day),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            character.ageYears?.let { age ->
                Text(
                    text = stringResource(R.string.char_bio_age, age),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            character.bloodType?.takeIf { it.isNotBlank() }?.let { bt ->
                Text(
                    text = stringResource(R.string.char_bio_blood_type, bt.uppercase()),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            character.heightCm?.let { h ->
                Text(
                    text = stringResource(R.string.char_bio_height, h),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            character.weightKg?.let { w ->
                Text(
                    text = stringResource(R.string.char_bio_weight, w),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            VnFormatters.formatMeasurements(character.measurements)?.let { mStr ->
                Text(
                    text = stringResource(R.string.char_bio_measurements, mStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor
                )
            }
            if (character.aliases.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.char_bio_aliases)}: ${character.aliases.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeSpec.contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}
