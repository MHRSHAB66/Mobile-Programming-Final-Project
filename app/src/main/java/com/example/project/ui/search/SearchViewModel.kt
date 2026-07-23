package com.example.project.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchHit
import com.example.project.domain.repository.LibraryRepository
import com.example.project.domain.repository.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.SONG,
    val history: List<String> = emptyList(),
    val likedIds: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchRepository: SearchRepository,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter.SONG)

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        filter,
        searchRepository.observeHistory(),
        libraryRepository.observeLikedIds(),
    ) { q, f, history, likedIds ->
        SearchUiState(query = q, filter = f, history = history, likedIds = likedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    val pagedHits: Flow<PagingData<SearchHit>> = combine(query, filter) { q, f -> q to f }
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { (q, f) ->
            if (q.isBlank()) flowOf(PagingData.empty())
            else searchRepository.searchPaged(q, f)
        }
        .cachedIn(viewModelScope)

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
