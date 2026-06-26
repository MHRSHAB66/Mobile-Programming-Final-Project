package com.example.project.data.repository

import com.example.project.data.local.db.SearchHistoryDao
import com.example.project.data.local.db.SearchHistoryEntity
import com.example.project.data.mock.MockData
import com.example.project.domain.model.SearchFilter
import com.example.project.domain.model.SearchResults
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SearchRepositoryImpl(
    private val historyDao: SearchHistoryDao,
    private val musicRepository: MusicRepository,
) : SearchRepository {

    override suspend fun search(query: String, filter: SearchFilter): SearchResults =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            when (filter) {
                SearchFilter.SONG -> SearchResults(
                    songs = musicRepository.getAllSongs().filter {
                        it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q)
                    }
                )
                SearchFilter.ARTIST -> SearchResults(
                    artists = MockData.artists.filter { it.name.lowercase().contains(q) }
                )
                SearchFilter.PLAYLIST -> SearchResults(
                    playlists = MockData.playlists.filter {
                        it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
                    }
                )
                SearchFilter.USER -> SearchResults(
                    users = MockData.users.filter {
                        it.displayName.lowercase().contains(q) || it.handle.lowercase().contains(q)
                    }
                )
            }
        }

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
