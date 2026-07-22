package com.example.project.data.repository

import com.example.project.data.local.db.LikedSongDao
import com.example.project.data.local.db.RecentlyPlayedDao
import com.example.project.data.local.db.toLikedEntity
import com.example.project.data.local.db.toRecentEntity
import com.example.project.data.local.db.toSong
import com.example.project.data.remote.api.LibraryApi
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryRepositoryImpl(
    private val likedDao: LikedSongDao,
    private val recentDao: RecentlyPlayedDao,
    private val libraryApi: LibraryApi,
    private val scope: CoroutineScope,
) : LibraryRepository {

    override fun observeLikedSongs(): Flow<List<Song>> =
        likedDao.observeAll().map { list -> list.map { it.toSong() } }

    override fun observeLikedIds(): Flow<Set<String>> =
        likedDao.observeIds().map { it.toSet() }

    override suspend fun isLiked(songId: String): Boolean = likedDao.isLiked(songId)

    override suspend fun toggleLike(song: Song): Boolean = withContext(Dispatchers.IO) {
        val currentlyLiked = likedDao.isLiked(song.id)
        if (currentlyLiked) {
            likedDao.delete(song.id)
            scope.launch(Dispatchers.IO) {
                runCatching { libraryApi.unlikeSong(song.id) }
                    .onFailure {
                        // Keep local unlike; next refreshLikes will reconcile if needed.
                    }
            }
            false
        } else {
            likedDao.insert(song.toLikedEntity(System.currentTimeMillis()))
            scope.launch(Dispatchers.IO) {
                runCatching { libraryApi.likeSong(song.id) }
                    .onFailure {
                        // Optimistic local like stays; refresh will drop if server rejected.
                    }
            }
            true
        }
    }

    override suspend fun refreshLikes() = withContext(Dispatchers.IO) {
        runCatching {
            val remote = libraryApi.getLikedSongs().map { dto ->
                dto.toDomainSong().copy(isLiked = true)
                    .toLikedEntity(System.currentTimeMillis())
            }
            likedDao.deleteAll()
            if (remote.isNotEmpty()) {
                likedDao.insertAll(remote)
            }
        }
        Unit
    }

    override suspend fun clearLikesCache() = withContext(Dispatchers.IO) {
        likedDao.deleteAll()
    }

    override fun observeRecentlyPlayed(): Flow<List<Song>> =
        recentDao.observeAll().map { list -> list.map { it.toSong() } }

    override suspend fun addRecentlyPlayed(song: Song) {
        recentDao.upsert(song.toRecentEntity(System.currentTimeMillis()))
    }
}
