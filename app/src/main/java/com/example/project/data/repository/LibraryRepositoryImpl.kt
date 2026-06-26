package com.example.project.data.repository

import com.example.project.data.local.db.LikedSongDao
import com.example.project.data.local.db.RecentlyPlayedDao
import com.example.project.data.local.db.toLikedEntity
import com.example.project.data.local.db.toRecentEntity
import com.example.project.data.local.db.toSong
import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepositoryImpl(
    private val likedDao: LikedSongDao,
    private val recentDao: RecentlyPlayedDao,
) : LibraryRepository {

    override fun observeLikedSongs(): Flow<List<Song>> =
        likedDao.observeAll().map { list -> list.map { it.toSong() } }

    override fun observeLikedIds(): Flow<Set<String>> =
        likedDao.observeIds().map { it.toSet() }

    override suspend fun isLiked(songId: String): Boolean = likedDao.isLiked(songId)

    override suspend fun toggleLike(song: Song): Boolean {
        return if (likedDao.isLiked(song.id)) {
            likedDao.delete(song.id)
            false
        } else {
            likedDao.insert(song.toLikedEntity(System.currentTimeMillis()))
            true
        }
    }

    override fun observeRecentlyPlayed(): Flow<List<Song>> =
        recentDao.observeAll().map { list -> list.map { it.toSong() } }

    override suspend fun addRecentlyPlayed(song: Song) {
        recentDao.upsert(song.toRecentEntity(System.currentTimeMillis()))
    }
}
