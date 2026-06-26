package com.example.project.domain.repository

import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Read access to the music catalogue (songs + artists). Backed by mock data today. */
interface MusicRepository {
    suspend fun getTrendingSongs(): List<Song>
    suspend fun getMostPopular(): List<Song>
    suspend fun getNewReleases(): List<Song>
    suspend fun getAllSongs(): List<Song>
    suspend fun getSong(id: String): Song?

    /**
     * Resolves a list of song ids to full [Song]s, applying liked state and any local
     * download path so the player can choose local vs. streamed source.
     */
    suspend fun getSongsByIds(ids: List<String>): List<Song>

    suspend fun getArtists(): List<Artist>
    suspend fun getArtist(id: String): Artist?
    suspend fun getArtistSongs(artistId: String): List<Song>

    /** Emits whenever liked/download state changes so lists can re-decorate songs. */
    fun observeLibrarySignals(): Flow<Unit>
}
