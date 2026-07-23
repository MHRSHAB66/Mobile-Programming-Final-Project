package com.example.project.domain.repository

import androidx.paging.PagingData
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** Read access to the music catalogue (songs + artists). */
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

    /** Spec §3 — long artist track lists use Paging 3. */
    fun getArtistSongsPaged(artistId: String): Flow<PagingData<Song>>

    /** Keeps Room / home lists in sync after a local follow/unfollow. */
    suspend fun updateCachedArtistFollowers(artistId: String, followers: Int)

    /** Emits whenever liked/download state changes so lists can re-decorate songs. */
    fun observeLibrarySignals(): Flow<Unit>
}
