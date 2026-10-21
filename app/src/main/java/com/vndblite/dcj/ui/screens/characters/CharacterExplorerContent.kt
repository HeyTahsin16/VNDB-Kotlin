package com.vndblite.dcj.ui.screens.characters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.components.EmptyStateView
import com.vndblite.dcj.ui.components.ErrorStateView
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.components.PreviewData
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors

@Composable
fun CharacterExplorerContent(
    state: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "char_filter_card") {
            CharacterFilterControlsCard(
                state = state,
                onAction = onAction
            )
        }

        when {
            state.isLoading -> {
                item(key = "char_loading") {
                    LoadingStateView()
                }
            }
            state.error != null && state.characters.isEmpty() -> {
                item(key = "char_error") {
                    ErrorStateView(
                        message = state.error,
                        onRetry = { onAction(CharacterSearchAction.Retry) }
                    )
                }
            }
            state.characters.isEmpty() -> {
                item(key = "char_empty") {
                    EmptyStateView(
                        title = stringResource(R.string.char_empty_results),
                        icon = Icons.Default.PersonSearch
                    )
                }
            }
            else -> {
                items(state.characters, key = { it.id }) { character ->
                    CharacterItemCard(
                        character = character,
                        showCoverCensor = state.showCoverCensor,
                        isNsfwRevealed = character.id in state.nsfwRevealedIds,
                        onToggleNsfwReveal = {
                            onAction(CharacterSearchAction.ToggleNsfwReveal(character.id))
                        },
                        onTraitClick = { trait ->
                            onAction(CharacterSearchAction.ToggleTrait(trait))
                        },
                        onVnClick = { vn ->
                            onAction(CharacterSearchAction.OpenVnDetail(vn))
                        }
                    )
                }

                if (state.canLoadMore) {
                    item(key = "char_load_more") {
                        CharacterLoadMoreFooter(
                            isLoadingMore = state.isLoadingMore,
                            onLoadMore = { onAction(CharacterSearchAction.LoadNextPage) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterFilterControlsCard(
    state: CharacterSearchUiState,
    onAction: (CharacterSearchAction) -> Unit
) {
    val spec = VndbThemeTokens.spec
    val semantic = VndbThemeTokens.colors
    val keyboard = LocalSoftwareKeyboardController.current

    Surface(
        color = spec.surfaceColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = state.filters.searchQuery,
                onValueChange = { onAction(CharacterSearchAction.UpdateNameQuery(it)) },
                placeholder = { Text(stringResource(R.string.char_search_name_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = spec.accentColor)
                },
                trailingIcon = {
                    if (state.filters.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onAction(CharacterSearchAction.UpdateNameQuery("")) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_clear_cd),
                                tint = spec.onSurfaceColor
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        onAction(CharacterSearchAction.ExecuteSearchNow)
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = vndbOutlinedTextFieldColors(spec),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("character_search_input")
            )

            CharacterTraitAutocompleteSection(state = state, onAction = onAction)
            CharacterPopularTraitsRow(state = state, onAction = onAction)
            CharacterRoleAndSexSection(state = state, onAction = onAction)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        keyboard?.hide()
                        onAction(CharacterSearchAction.ExecuteSearchNow)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = spec.accentColor,
                        contentColor = semantic.onAccentButton
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("char_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.char_find_button), fontWeight = FontWeight.Bold)
                }

                if (state.filters.hasActiveFilters) {
                    OutlinedButton(
                        onClick = { onAction(CharacterSearchAction.ClearAllFilters) },
                        border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("char_clear_filters_button")
                    ) {
                        Text(stringResource(R.string.action_clear), color = semantic.danger)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterExplorerContentPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        CharacterExplorerContent(
            state = CharacterSearchUiState(
                characters = listOf(PreviewData.sampleCharacter)
            ),
            onAction = {}
        )
    }
}
