package com.vndblite.dcj.ui.screens.random

import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.VnSummary

data class RandomVnUiState(
    val settings: AppSettings = AppSettings(),
    val vn: VnSummary? = null,
    val isLoading: Boolean = false,
    val error: ApiError? = null,
    val isNsfwRevealed: Boolean = false
)

sealed interface RandomVnAction {
    data object RollAnother : RandomVnAction
    data object ToggleNsfwReveal : RandomVnAction
}
