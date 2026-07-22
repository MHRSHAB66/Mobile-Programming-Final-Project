package com.example.project.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchResults
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.SearchRepository
import com.example.project.domain.usecase.SearchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.SONG,
    val isSearching: Boolean = false,
    val results: SearchResults = SearchResults(),
    val history: List<String> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchUseCase: SearchUseCase,
    private val searchRepository: SearchRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter.SONG)
    private val isSearching = MutableStateFlow(false)

    private val resultsFlow = combine(query, filter) { q, f -> q to f }
        .debounce(350)
        .distinctUntilChanged()
        .onEach { isSearching.value = it.first.isNotBlank() }
        .mapLatest { (q, f) ->
            val r = searchUseCase(q, f)
            isSearching.value = false
            r
        }

    private val baseState = combine(
        query, filter, resultsFlow, searchRepository.observeHistory(), isSearching
    ) { q, f, results, history, searching ->
        SearchUiState(
            query = q,
            filter = f,
            isSearching = searching,
            results = results,
            history = history,
        )
    }

    val uiState: StateFlow<SearchUiState> = combine(
        baseState,
        libraryRepository.observeLikedIds(),
    ) { state, likedIds ->
        state.copy(
            results = state.results.copy(
                songs = state.results.songs.map { it.copy(isLiked = it.id in likedIds) },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: SearchFilter) {
        filter.value = value
    }

    fun onSubmit() {
        val q = query.value.trim()
        if (q.isNotEmpty()) viewModelScope.launch { searchRepository.addToHistory(q) }
    }

    fun onHistoryClick(value: String) {
        query.value = value
    }

    fun onRemoveHistory(value: String) {
        viewModelScope.launch { searchRepository.removeFromHistory(value) }
    }

    fun onClearHistory() {
        viewModelScope.launch { searchRepository.clearHistory() }
    }
}
