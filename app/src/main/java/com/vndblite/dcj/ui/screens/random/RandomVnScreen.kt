package com.vndblite.dcj.ui.screens.random

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.ui.components.BbCodeText
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.NsfwAsyncImage
import com.vndblite.dcj.ui.format.VnFormatters
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec

@Composable
fun RandomVnScreen(
    uiState: RandomVnUiState,
    onAction: (RandomVnAction) -> Unit,
    onOpenVnDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSpec = LocalThemeSpec.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("random_vn_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = themeSpec.accentColor
                )
            }
            Text(
                text = stringResource(R.string.random_vn_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = themeSpec.accentColor
            )
        }

        when {
            uiState.isLoading && uiState.vn == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingStateView()
                }
            }
            uiState.error != null && uiState.vn == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateView(
                        error = uiState.error,
                        onRetry = { onAction(RandomVnAction.RollAnother) }
                    )
                }
            }
            uiState.vn != null -> {
                val vn = uiState.vn
                val shouldBlur = vn.image?.isExplicit == true &&
                    uiState.settings.showCoverCensor &&
                    !uiState.isNsfwRevealed

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            color = themeSpec.surfaceColor.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val coverUrl = vn.image?.url.orEmpty()
                                if (coverUrl.isNotBlank()) {
                                    NsfwAsyncImage(
                                        imageUrl = coverUrl,
                                        contentDescription = vn.title,
                                        shouldBlur = shouldBlur,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 180.dp, height = 250.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Text(
                                    text = vn.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = themeSpec.accentColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ColorTokens.RatingStarGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.rating_with_votes_format,
                                            VnFormatters.formatRating(vn.rating),
                                            vn.voteCount
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeSpec.contentColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val desc = vn.description?.takeIf { it.isNotBlank() }
                                if (desc != null) {
                                    BbCodeText(rawText = desc)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onAction(RandomVnAction.RollAnother) },
                                        enabled = !uiState.isLoading,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("random_vn_roll_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = null,
                                            tint = themeSpec.accentColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.random_vn_roll_another),
                                            color = themeSpec.contentColor
                                        )
                                    }
                                    Button(
                                        onClick = { onOpenVnDetail(vn.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = themeSpec.accentColor,
                                            contentColor = themeSpec.onAccentColor
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("random_vn_open_detail_button")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.random_vn_open_details),
                                            fontWeight = FontWeight.Bold
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
