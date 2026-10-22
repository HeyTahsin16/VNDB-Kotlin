package com.vndblite.dcj.ui.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RandomVnViewModel(
    private val vnRepository: VnRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RandomVnUiState())
    val uiState: StateFlow<RandomVnUiState> = _uiState.asStateFlow()

    private var rollJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        rollRandomVn()
    }

    fun onAction(action: RandomVnAction) {
        when (action) {
            RandomVnAction.RollAnother -> rollRandomVn()
            RandomVnAction.ToggleNsfwReveal -> {
                _uiState.update { it.copy(isNsfwRevealed = !it.isNsfwRevealed) }
            }
        }
    }

    private fun rollRandomVn() {
        rollJob?.cancel()
        rollJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isNsfwRevealed = false) }
            when (val res = vnRepository.fetchRandomHighRatedVn()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            vn = res.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = res.error
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            vnRepository: VnRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RandomVnViewModel(
                    vnRepository = vnRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
        }
    }
}
