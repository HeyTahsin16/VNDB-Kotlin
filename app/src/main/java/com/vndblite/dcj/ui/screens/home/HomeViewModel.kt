package com.vndblite.dcj.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.HomeSection
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            revealedNsfwIds = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED_NSFW)?.toSet().orEmpty()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val remoteSections = HomeSection.entries.filter { it != HomeSection.COLLECTION }

    init {
        observeSettings()
        observeCollection()
        observeRemoteSections()
        observeQuotesAndStats()
        refreshAll(force = false)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.RefreshAll -> refreshAll(force = true)
            is HomeAction.RetrySection -> refreshSingleSection(action.section, force = true)
            HomeAction.RefreshQuotes -> refreshQuotes()
            HomeAction.RetryStats -> refreshStats(force = true)
            is HomeAction.ToggleNsfwReveal -> toggleNsfwReveal(action.id)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeCollection() {
        viewModelScope.launch {
            collectionRepository.observeAllWithVn().collect { items ->
                val byId = items.associate { it.entry.vnId to it.entry }
                val summaries = items.map { it.vn }
                _uiState.update { state ->
                    val currentColState = state.sections[HomeSection.COLLECTION]
                        ?: HomeSectionState(HomeSection.COLLECTION)
                    state.copy(
                        collectionByVnId = byId,
                        sections = state.sections + (
                            HomeSection.COLLECTION to currentColState.copy(
                                items = summaries,
                                isLoading = false,
                                error = null
                            )
                            )
                    )
                }
            }
        }
        viewModelScope.launch {
            collectionRepository.observeStatusCounts().collect { counts ->
                _uiState.update { it.copy(statusCounts = counts) }
            }
        }
    }

    private fun observeRemoteSections() {
        remoteSections.forEach { section ->
            viewModelScope.launch {
                vnRepository.observeHomeSection(section).collect { cachedItems ->
                    _uiState.update { state ->
                        val existing = state.sections[section] ?: HomeSectionState(section)
                        state.copy(
                            sections = state.sections + (
                                section to existing.copy(
                                    items = cachedItems,
                                    error = if (cachedItems.isNotEmpty()) null else existing.error
                                )
                                )
                        )
                    }
                }
            }
        }
    }

    private fun observeQuotesAndStats() {
        viewModelScope.launch {
            vnRepository.observeQuotes().collect { quotes ->
                _uiState.update { it.copy(quotes = quotes) }
            }
        }
        viewModelScope.launch {
            vnRepository.observeSiteStats().collect { stats ->
                _uiState.update { it.copy(siteStats = stats) }
            }
        }
    }

    private fun refreshAll(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                val updatedSections = state.sections.toMutableMap()
                remoteSections.forEach { sec ->
                    val cur = updatedSections[sec] ?: HomeSectionState(sec)
                    updatedSections[sec] = cur.copy(isLoading = true, error = null)
                }
                state.copy(
                    isRefreshingAll = true,
                    sections = updatedSections,
                    isQuotesLoading = true,
                    quotesError = null,
                    isStatsLoading = true,
                    statsError = null
                )
            }

            val pageSize = _uiState.value.settings.maxPreviewItem.coerceIn(5, 50)
            val sectionJobs = remoteSections.map { section ->
                launch {
                    val result = vnRepository.refreshHomeSection(section, pageSize = pageSize, force = force)
                    _uiState.update { state ->
                        val cur = state.sections[section] ?: HomeSectionState(section)
                        val err = (result as? AppResult.Failure)?.error
                        state.copy(
                            sections = state.sections + (
                                section to cur.copy(isLoading = false, error = err)
                                )
                        )
                    }
                }
            }
            val quotesJob = launch {
                if (force || _uiState.value.quotes.isEmpty()) {
                    val res = vnRepository.refreshQuotes(count = 4)
                    _uiState.update {
                        it.copy(
                            isQuotesLoading = false,
                            quotesError = (res as? AppResult.Failure)?.error
                        )
                    }
                } else {
                    _uiState.update { it.copy(isQuotesLoading = false) }
                }
            }
            val statsJob = launch {
                val res = vnRepository.refreshSiteStats(force = force)
                _uiState.update {
                    it.copy(
                        isStatsLoading = false,
                        statsError = (res as? AppResult.Failure)?.error
                    )
                }
            }

            sectionJobs.forEach { it.join() }
            quotesJob.join()
            statsJob.join()
            _uiState.update { it.copy(isRefreshingAll = false) }
        }
    }

    private fun refreshSingleSection(section: HomeSection, force: Boolean) {
        if (section == HomeSection.COLLECTION) return
        viewModelScope.launch {
            _uiState.update { state ->
                val cur = state.sections[section] ?: HomeSectionState(section)
                state.copy(
                    sections = state.sections + (section to cur.copy(isLoading = true, error = null))
                )
            }
            val pageSize = _uiState.value.settings.maxPreviewItem.coerceIn(5, 50)
            val result = vnRepository.refreshHomeSection(section, pageSize = pageSize, force = force)
            _uiState.update { state ->
                val cur = state.sections[section] ?: HomeSectionState(section)
                state.copy(
                    sections = state.sections + (
                        section to cur.copy(
                            isLoading = false,
                            error = (result as? AppResult.Failure)?.error
                        )
                        )
                )
            }
        }
    }

    private fun refreshQuotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isQuotesLoading = true, quotesError = null) }
            val result = vnRepository.refreshQuotes(count = 4)
            _uiState.update {
                it.copy(
                    isQuotesLoading = false,
                    quotesError = (result as? AppResult.Failure)?.error
                )
            }
        }
    }

    private fun refreshStats(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStatsLoading = true, statsError = null) }
            val result = vnRepository.refreshSiteStats(force = force)
            _uiState.update {
                it.copy(
                    isStatsLoading = false,
                    statsError = (result as? AppResult.Failure)?.error
                )
            }
        }
    }

    private fun toggleNsfwReveal(id: String) {
        _uiState.update { state ->
            val updated = if (id in state.revealedNsfwIds) {
                state.revealedNsfwIds - id
            } else {
                state.revealedNsfwIds + id
            }
            savedStateHandle[KEY_REVEALED_NSFW] = ArrayList(updated)
            state.copy(revealedNsfwIds = updated)
        }
    }

    companion object {
        private const val KEY_REVEALED_NSFW = "home_revealed_nsfw_ids"

        fun factory(
            vnRepository: VnRepository,
            collectionRepository: CollectionRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return HomeViewModel(
                    vnRepository = vnRepository,
                    collectionRepository = collectionRepository,
                    settingsRepository = settingsRepository,
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }
}
