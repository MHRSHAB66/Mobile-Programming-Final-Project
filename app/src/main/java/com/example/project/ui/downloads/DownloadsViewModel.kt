package com.example.project.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.domain.model.DownloadItem
import com.example.project.domain.model.DownloadSort
import com.example.project.domain.repository.DownloadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val items: List<DownloadItem> = emptyList(),
    val sort: DownloadSort = DownloadSort.RECENT,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val sort = MutableStateFlow(DownloadSort.RECENT)

    val uiState: StateFlow<DownloadsUiState> = sort
        .flatMapLatest { s ->
            downloadRepository.observeDownloads(s).map { items -> DownloadsUiState(items, s) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun setSort(value: DownloadSort) {
        sort.value = value
    }

    fun remove(songId: String) {
        viewModelScope.launch { downloadRepository.removeDownload(songId) }
    }
}
