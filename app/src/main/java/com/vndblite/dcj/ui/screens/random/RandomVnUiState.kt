package com.vndblite.dcj.ui.screens.random

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.VnSummary

@Immutable
data class RandomVnUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val vn: VnSummary? = null,
    val error: UiText? = null,
    val collectionEntries: Map<String, CollectionEntry> = emptyMap(),
    val nsfwRevealedIds: Set<String> = emptySet(),
    val showCoverCensor: Boolean = true
)

sealed interface RandomVnAction {
    data object OpenAndRoll : RandomVnAction
    data object RollAgain : RandomVnAction
    data object Dismiss : RandomVnAction
    data class ToggleNsfwReveal(val vnId: String) : RandomVnAction
}
