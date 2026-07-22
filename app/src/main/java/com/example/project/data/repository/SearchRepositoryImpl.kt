package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.project.data.local.db.SearchHistoryDao
import com.example.project.data.local.db.SearchHistoryEntity
import com.example.project.data.mock.MockData
import com.example.project.data.paging.SearchPagingSource
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.toDomainArtist
import com.example.project.data.remote.api.dto.toDomainPlaylist
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.data.remote.api.dto.toDomainUser
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchHit
import com.example.project.domain.model.SearchResults
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SearchRepositoryImpl(
    private val historyDao: SearchHistoryDao,
    private val musicRepository: MusicRepository,
    private val catalogApi: CatalogApi,
) : SearchRepository {

    override suspend fun search(query: String, filter: SearchFilter): SearchResults =
        withContext(Dispatchers.IO) {
            val type = when (filter) {
                SearchFilter.SONG -> "song"
                SearchFilter.ARTIST -> "artist"
                SearchFilter.PLAYLIST -> "playlist"
                SearchFilter.USER -> "user"
            }
            val remote = runCatching {
                catalogApi.search(query = query.trim(), type = type, page = 1, limit = 40)
            }.getOrNull()

            if (remote != null) {
                return@withContext SearchResults(
                    songs = remote.songs.map { it.toDomainSong() },
                    artists = remote.artists.map { it.toDomainArtist() },
                    playlists = remote.playlists.map { it.toDomainPlaylist() },
                    users = remote.users.map { it.toDomainUser() },
                )
            }

            val q = query.trim().lowercase()
            when (filter) {
                SearchFilter.SONG -> SearchResults(
                    songs = musicRepository.getAllSongs().filter {
                        it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q)
                    },
                )
                SearchFilter.ARTIST -> SearchResults(
                    artists = musicRepository.getArtists().filter {
                        it.name.lowercase().contains(q)
                    },
                )
                SearchFilter.PLAYLIST -> SearchResults(
                    playlists = MockData.playlists.filter {
                        it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
                    },
                )
                SearchFilter.USER -> SearchResults(
                    users = MockData.users.filter {
                        it.displayName.lowercase().contains(q) || it.handle.lowercase().contains(q)
                    },
                )
            }
        }

    override fun searchPaged(query: String, filter: SearchFilter): Flow<PagingData<SearchHit>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false, initialLoadSize = 20),
            pagingSourceFactory = { SearchPagingSource(catalogApi, query, filter) },
        ).flow

    override fun observeHistory(): Flow<List<String>> = historyDao.observeRecent()

    override suspend fun addToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            historyDao.insert(SearchHistoryEntity(trimmed, System.currentTimeMillis()))
        }
    }

    override suspend fun removeFromHistory(query: String) = historyDao.delete(query)
    override suspend fun clearHistory() = historyDao.clear()
}
