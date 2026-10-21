package com.vndblite.dcj.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.R
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val feedbackFlow = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        feedbackFlow
    ) { settings, feedback ->
        SettingsUiState(
            settings = settings,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectTheme -> update { it.copy(themeId = action.themeId) }
            is SettingsAction.ToggleNsfwBlur -> update { it.copy(showCoverCensor = action.enabled) }
            is SettingsAction.ToggleShowChart -> update { it.copy(showChart = action.enabled) }
            is SettingsAction.UpdateFontScaleOffset -> {
                val rounded = String.format(Locale.US, "%.1f", action.offset).toFloat()
                update { it.copy(fontSizeOffset = rounded) }
            }
            is SettingsAction.UpdateMaxPreviewItems -> {
                update { it.copy(maxPreviewItem = action.count.coerceIn(5, 50)) }
            }
            is SettingsAction.UpdatePortraitColumns -> {
                update { it.copy(maxItemPerRowPortrait = action.columns.coerceIn(1, 5)) }
            }
            is SettingsAction.UpdateLandscapeColumns -> {
                update { it.copy(maxItemPerRowLandscape = action.columns.coerceIn(2, 8)) }
            }
            is SettingsAction.MoveStatusTab -> moveStatusTab(action.status, action.direction)
            SettingsAction.ClearTransientCache -> {
                viewModelScope.launch {
                    vnRepository.clearTransientCaches()
                    feedbackFlow.value = UiText.StringResource(R.string.settings_cache_cleared_toast)
                }
            }
            SettingsAction.ResetSettings -> {
                viewModelScope.launch {
                    settingsRepository.resetSettingsToDefaults()
                }
            }
            SettingsAction.ClearLocalCollection -> {
                viewModelScope.launch {
                    collectionRepository.clearLocalCollection(queueForRemoteDelete = false)
                }
            }
            SettingsAction.DismissFeedback -> {
                feedbackFlow.value = null
            }
        }
    }

    private fun update(transform: (com.vndblite.dcj.domain.model.AppSettings) -> com.vndblite.dcj.domain.model.AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    private fun moveStatusTab(status: CollectionStatus, direction: Int) {
        update { current ->
            val list = current.collectionStatusTabArrangement.toMutableList()
            val index = list.indexOf(status)
            val target = index + direction
            if (index != -1 && target in list.indices) {
                val temp = list[target]
                list[target] = list[index]
                list[index] = temp
            }
            current.copy(collectionStatusTabArrangement = list)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    vnRepository = container.vnRepository,
                    collectionRepository = container.collectionRepository
                )
            }
        }
    }
}
