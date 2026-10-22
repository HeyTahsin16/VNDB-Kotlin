package com.vndblite.dcj.ui.screens.settings

import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val showClearCacheConfirm: Boolean = false,
    val showClearCollectionConfirm: Boolean = false,
    val showResetDefaultsConfirm: Boolean = false
)

sealed interface SettingsAction {
    data class SelectTheme(val themeId: ThemeId) : SettingsAction
    data class SetCoverCensor(val enabled: Boolean) : SettingsAction
    data class SetShowChart(val enabled: Boolean) : SettingsAction
    data class SetFontSizeOffset(val offset: Float) : SettingsAction
    data class SetMaxPreviewItem(val count: Int) : SettingsAction
    data class SetPortraitColumns(val count: Int) : SettingsAction
    data class SetLandscapeColumns(val count: Int) : SettingsAction
    data class MoveTab(val status: CollectionStatus, val direction: Int) : SettingsAction
    data object RequestClearCache : SettingsAction
    data object ConfirmClearCache : SettingsAction
    data object DismissClearCache : SettingsAction
    data object RequestClearCollection : SettingsAction
    data object ConfirmClearCollection : SettingsAction
    data object DismissClearCollection : SettingsAction
    data object RequestResetDefaults : SettingsAction
    data object ConfirmResetDefaults : SettingsAction
    data object DismissResetDefaults : SettingsAction
}
