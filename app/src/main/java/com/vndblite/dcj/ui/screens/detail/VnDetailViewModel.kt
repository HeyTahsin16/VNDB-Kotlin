package com.vndblite.dcj.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VnDetailViewModel(
    private val vnId: String,
    private val vnRepository: VnRepository,
    private val collectionRepository: CollectionRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val initialTabName = savedStateHandle.get<String>(KEY_SELECTED_TAB)
    private val initialTab = VnDetailTab.entries.firstOrNull { it.name == initialTabName }
        ?: VnDetailTab.OVERVIEW

    private val _uiState = MutableStateFlow(
        VnDetailUiState(
            vnId = vnId,
            selectedTab = initialTab,
            spoilerLevel = savedStateHandle.get<Int>(KEY_SPOILER_LEVEL) ?: 0,
            fullscreenScreenshotUrl = savedStateHandle.get<String>(KEY_FULLSCREEN_SCREENSHOT),
            revealedNsfwIds = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED_NSFW)?.toSet().orEmpty()
        )
    )
    val uiState: StateFlow<VnDetailUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeCachedVnAndCollection()
        loadDetailsAndExtras(force = false)
    }

    fun onAction(action: VnDetailAction) {
        when (action) {
            VnDetailAction.Refresh -> loadDetailsAndExtras(force = true)
            is VnDetailAction.SelectTab -> {
                savedStateHandle[KEY_SELECTED_TAB] = action.tab.name
                _uiState.update { it.copy(selectedTab = action.tab) }
            }
            is VnDetailAction.SelectSpoilerLevel -> {
                val coerced = action.level.coerceIn(0, 2)
                savedStateHandle[KEY_SPOILER_LEVEL] = coerced
                _uiState.update { it.copy(spoilerLevel = coerced) }
            }
            VnDetailAction.OpenCollectionDialog -> {
                _uiState.update { it.copy(isCollectionDialogOpen = true) }
            }
            VnDetailAction.DismissCollectionDialog -> {
                _uiState.update { it.copy(isCollectionDialogOpen = false) }
            }
            is VnDetailAction.SaveCollectionEntry -> {
                val summary = _uiState.value.summary ?: VnSummary(
                    id = vnId,
                    title = _uiState.value.collectionEntry?.title ?: vnId
                )
                viewModelScope.launch {
                    collectionRepository.upsertEntry(
                        vnSummary = summary,
                        status = action.draft.status,
                        vote = action.draft.vote,
                        notes = action.draft.notes,
                        startedDate = action.draft.startedDate,
                        finishedDate = action.draft.finishedDate
                    )
                    _uiState.update { it.copy(isCollectionDialogOpen = false) }
                }
            }
            VnDetailAction.RemoveCollectionEntry -> {
                viewModelScope.launch {
                    collectionRepository.removeEntry(vnId)
                    _uiState.update { it.copy(isCollectionDialogOpen = false) }
                }
            }
            is VnDetailAction.SelectCharacter -> {
                _uiState.update { it.copy(selectedCharacter = action.character) }
            }
            VnDetailAction.DismissCharacterBio -> {
                _uiState.update { it.copy(selectedCharacter = null) }
            }
            is VnDetailAction.OpenFullscreenScreenshot -> {
                savedStateHandle[KEY_FULLSCREEN_SCREENSHOT] = action.fullUrl
                _uiState.update { it.copy(fullscreenScreenshotUrl = action.fullUrl) }
            }
            VnDetailAction.CloseFullscreenScreenshot -> {
                savedStateHandle[KEY_FULLSCREEN_SCREENSHOT] = null
                _uiState.update { it.copy(fullscreenScreenshotUrl = null) }
            }
            is VnDetailAction.ExpandFullReview -> expandReview(action.reviewId)
            is VnDetailAction.ToggleNsfwReveal -> toggleNsfwReveal(action.id)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeCachedVnAndCollection() {
        viewModelScope.launch {
            vnRepository.observeVnSummary(vnId).collect { summary ->
                if (summary != null) {
                    _uiState.update { it.copy(summary = summary) }
                }
            }
        }
        viewModelScope.launch {
            vnRepository.observeVnDetails(vnId).collect { details ->
                if (details != null) {
                    _uiState.update { it.copy(details = details) }
                }
            }
        }
        viewModelScope.launch {
            collectionRepository.observeEntry(vnId).collect { entry ->
                _uiState.update { it.copy(collectionEntry = entry) }
            }
        }
    }

    private fun loadDetailsAndExtras(force: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDetails = true,
                    detailsError = null,
                    isLoadingExtras = true
                )
            }

            val detailJob = launch {
                when (val res = vnRepository.loadVnDetail(vnId, force = force)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                summary = res.data.first,
                                details = res.data.second,
                                isLoadingDetails = false,
                                detailsError = null
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoadingDetails = false,
                                detailsError = res.error
                            )
                        }
                    }
                }
            }

            val charsJob = launch {
                when (val res = vnRepository.fetchVnCharacters(vnId, pageSize = 50)) {
                    is AppResult.Success -> _uiState.update { it.copy(characters = res.data) }
                    is AppResult.Failure -> Unit
                }
            }

            val quotesJob = launch {
                when (val res = vnRepository.fetchVnQuotes(vnId, count = 8)) {
                    is AppResult.Success -> _uiState.update { it.copy(quotes = res.data) }
                    is AppResult.Failure -> Unit
                }
            }

            val reviewsJob = launch {
                when (val res = vnRepository.fetchVnReviews(vnId)) {
                    is AppResult.Success -> _uiState.update { it.copy(reviews = res.data) }
                    is AppResult.Failure -> Unit
                }
            }

            detailJob.join()
            charsJob.join()
            quotesJob.join()
            reviewsJob.join()
            _uiState.update { it.copy(isLoadingExtras = false) }
        }
    }

    private fun expandReview(reviewId: String) {
        if (reviewId in _uiState.value.loadingFullReviewIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingFullReviewIds = it.loadingFullReviewIds + reviewId) }
            when (val res = vnRepository.fetchFullReviewText(reviewId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            expandedReviewTexts = it.expandedReviewTexts + (reviewId to res.data),
                            loadingFullReviewIds = it.loadingFullReviewIds - reviewId
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(loadingFullReviewIds = it.loadingFullReviewIds - reviewId)
                    }
                }
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
        private const val KEY_SELECTED_TAB = "vn_detail_selected_tab"
        private const val KEY_SPOILER_LEVEL = "vn_detail_spoiler_level"
        private const val KEY_FULLSCREEN_SCREENSHOT = "vn_detail_fullscreen_screenshot"
        private const val KEY_REVEALED_NSFW = "vn_detail_revealed_nsfw_ids"

        fun factory(
            vnId: String,
            vnRepository: VnRepository,
            collectionRepository: CollectionRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return VnDetailViewModel(
                    vnId = vnId,
                    vnRepository = vnRepository,
                    collectionRepository = collectionRepository,
                    settingsRepository = settingsRepository,
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }
}
