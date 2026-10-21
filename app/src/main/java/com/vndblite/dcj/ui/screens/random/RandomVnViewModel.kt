package com.vndblite.dcj.ui.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vndblite.dcj.AppContainer
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.UiText
import com.vndblite.dcj.core.toUiText
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RollState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val vn: VnSummary? = null,
    val error: UiText? = null
)

class RandomVnViewModel(
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val rollState = MutableStateFlow(RollState())
    private var rollJob: Job? = null

    val uiState: StateFlow<RandomVnUiState> = combine(
        rollState,
        collectionRepository.observeAllVisibleEntriesMap(),
        settingsRepository.nsfwRevealedIdsFlow,
        settingsRepository.settingsFlow
    ) { roll, entries, revealedIds, settings ->
        RandomVnUiState(
            isVisible = roll.isVisible,
            isLoading = roll.isLoading,
            vn = roll.vn,
            error = roll.error,
            collectionEntries = entries,
            nsfwRevealedIds = revealedIds,
            showCoverCensor = settings.showCoverCensor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RandomVnUiState()
    )

    fun onAction(action: RandomVnAction) {
        when (action) {
            RandomVnAction.OpenAndRoll -> {
                rollState.update { it.copy(isVisible = true) }
                rollRandomVn()
            }
            RandomVnAction.RollAgain -> rollRandomVn()
            RandomVnAction.Dismiss -> {
                rollJob?.cancel()
                rollState.update { it.copy(isVisible = false, isLoading = false) }
            }
            is RandomVnAction.ToggleNsfwReveal -> {
                settingsRepository.toggleNsfwReveal(action.vnId)
            }
        }
    }

    private fun rollRandomVn() {
        rollJob?.cancel()
        rollState.update { it.copy(isLoading = true, error = null) }
        rollJob = viewModelScope.launch {
            when (val result = vnRepository.fetchRandomVn()) {
                is AppResult.Success -> {
                    rollState.update {
                        it.copy(isLoading = false, vn = result.data, error = null)
                    }
                }
                is AppResult.Failure -> {
                    rollState.update {
                        it.copy(isLoading = false, error = result.error.toUiText())
                    }
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RandomVnViewModel(
                    vnRepository = container.vnRepository,
                    collectionRepository = container.collectionRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
