package com.vndblite.dcj.ui.screens.settings

import androidx.compose.runtime.Immutable
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId

@Immutable
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val feedbackMessage: UiText? = null
)

sealed interface SettingsAction {
    data class SelectTheme(val themeId: ThemeId) : SettingsAction
    data class ToggleNsfwBlur(val enabled: Boolean) : SettingsAction
    data class ToggleShowChart(val enabled: Boolean) : SettingsAction
    data class UpdateFontScaleOffset(val offset: Float) : SettingsAction
    data class UpdateMaxPreviewItems(val count: Int) : SettingsAction
    data class UpdatePortraitColumns(val columns: Int) : SettingsAction
    data class UpdateLandscapeColumns(val columns: Int) : SettingsAction
    data class MoveStatusTab(val status: CollectionStatus, val direction: Int) : SettingsAction
    data object ClearTransientCache : SettingsAction
    data object ResetSettings : SettingsAction
    data object ClearLocalCollection : SettingsAction
    data object DismissFeedback : SettingsAction
}
