package com.vndblite.dcj.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectTheme -> viewModelScope.launch {
                settingsRepository.setThemeId(action.themeId)
            }
            is SettingsAction.SetCoverCensor -> viewModelScope.launch {
                settingsRepository.setShowCoverCensor(action.enabled)
            }
            is SettingsAction.SetShowChart -> viewModelScope.launch {
                settingsRepository.setShowChart(action.enabled)
            }
            is SettingsAction.SetFontSizeOffset -> viewModelScope.launch {
                settingsRepository.setFontSizeOffset(action.offset)
            }
            is SettingsAction.SetMaxPreviewItem -> viewModelScope.launch {
                settingsRepository.setMaxPreviewItem(action.count)
            }
            is SettingsAction.SetPortraitColumns -> viewModelScope.launch {
                settingsRepository.setMaxItemPerRowPortrait(action.count)
            }
            is SettingsAction.SetLandscapeColumns -> viewModelScope.launch {
                settingsRepository.setMaxItemPerRowLandscape(action.count)
            }
            is SettingsAction.MoveTab -> moveStatusTab(action.status, action.direction)
            SettingsAction.RequestClearCache -> _uiState.update { it.copy(showClearCacheConfirm = true) }
            SettingsAction.DismissClearCache -> _uiState.update { it.copy(showClearCacheConfirm = false) }
            SettingsAction.ConfirmClearCache -> {
                _uiState.update { it.copy(showClearCacheConfirm = false) }
                viewModelScope.launch { vnRepository.clearTransientCache() }
            }
            SettingsAction.RequestClearCollection -> _uiState.update { it.copy(showClearCollectionConfirm = true) }
            SettingsAction.DismissClearCollection -> _uiState.update { it.copy(showClearCollectionConfirm = false) }
            SettingsAction.ConfirmClearCollection -> {
                _uiState.update { it.copy(showClearCollectionConfirm = false) }
                viewModelScope.launch { collectionRepository.clearAllLocalEntries(queueForRemoteDelete = false) }
            }
            SettingsAction.RequestResetDefaults -> _uiState.update { it.copy(showResetDefaultsConfirm = true) }
            SettingsAction.DismissResetDefaults -> _uiState.update { it.copy(showResetDefaultsConfirm = false) }
            SettingsAction.ConfirmResetDefaults -> {
                _uiState.update { it.copy(showResetDefaultsConfirm = false) }
                viewModelScope.launch { settingsRepository.resetToDefaults() }
            }
        }
    }

    private fun moveStatusTab(status: CollectionStatus, direction: Int) {
        val current = _uiState.value.settings.collectionStatusTabArrangement
            .ifEmpty { CollectionStatus.entries }
            .toMutableList()
        val index = current.indexOf(status)
        if (index < 0) return
        val target = (index + direction).coerceIn(0, current.lastIndex)
        if (target == index) return
        current.removeAt(index)
        current.add(target, status)
        viewModelScope.launch {
            settingsRepository.setCollectionStatusTabArrangement(current)
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            vnRepository: VnRepository,
            collectionRepository: CollectionRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    settingsRepository = settingsRepository,
                    vnRepository = vnRepository,
                    collectionRepository = collectionRepository
                ) as T
            }
        }
    }
}
