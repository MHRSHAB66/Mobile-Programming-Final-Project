package com.example.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.domain.model.HomeFeed
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.usecase.GetHomeFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val feed: HomeFeed? = null,
    val error: UiText? = null,
)

class HomeViewModel(
    private val getHomeFeed: GetHomeFeedUseCase,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val baseFeed = MutableStateFlow<HomeFeed?>(null)
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        baseFeed, loading, error, libraryRepository.observeLikedIds()
    ) { feed, isLoading, err, likedIds ->
        HomeUiState(
            isLoading = isLoading,
            feed = feed?.decorate(likedIds),
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        load(showLoading = true)
    }

    /** Full load (shimmer if nothing shown yet). */
    fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && baseFeed.value == null) loading.value = true
            error.value = null
            runCatching { getHomeFeed() }
                .onSuccess { baseFeed.value = it }
                .onFailure { error.value = UiText.from(R.string.error_generic) }
            loading.value = false
        }
    }

    /** Silent refresh when returning to Home (e.g. after following an artist). */
    fun refresh() = load(showLoading = false)

    private fun HomeFeed.decorate(liked: Set<String>): HomeFeed = copy(
        carousel = carousel.applyLiked(liked),
        mostPopular = mostPopular.applyLiked(liked),
        newReleases = newReleases.applyLiked(liked),
    )

    private fun List<Song>.applyLiked(liked: Set<String>): List<Song> =
        map { it.copy(isLiked = it.id in liked) }
}
