package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.project.data.mock.MockData
import com.example.project.data.paging.ListPagingSource
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.toDomainPlaylist
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.Song
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl(
    private val musicRepository: MusicRepository,
    private val catalogApi: CatalogApi,
) : PlaylistRepository {

    @Volatile
    private var cached: List<Playlist>? = null

    private suspend fun allPlaylists(): List<Playlist> {
        cached?.let { return it }
        val remote = runCatching { catalogApi.getPlaylists().map { it.toDomainPlaylist() } }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
        val loaded = remote ?: MockData.playlists
        cached = loaded
        return loaded
    }

    override suspend fun getPlaylists(type: PlaylistType): List<Playlist> =
        withContext(Dispatchers.IO) { allPlaylists().filter { it.type == type } }

    override suspend fun getAllPlaylists(): List<Playlist> =
        withContext(Dispatchers.IO) { allPlaylists() }

    override suspend fun getPlaylist(id: String): Playlist? = withContext(Dispatchers.IO) {
        runCatching { catalogApi.getPlaylist(id).toDomainPlaylist() }.getOrNull()
            ?: allPlaylists().firstOrNull { it.id == id }
            ?: MockData.playlistById(id)
    }

    override suspend fun getPlaylistSongs(id: String): List<Song> = withContext(Dispatchers.IO) {
        val remote = runCatching {
            catalogApi.getPlaylistSongs(id, page = 1, limit = 200).items.map { it.toDomainSong() }
        }.getOrNull()
        if (!remote.isNullOrEmpty()) return@withContext remote

        val playlist = MockData.playlistById(id) ?: return@withContext emptyList()
        musicRepository.getSongsByIds(playlist.songIds)
    }

    override fun getPlaylistSongsPaged(id: String): Flow<PagingData<Song>> =
        Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            val songs = runCatching {
                // Blocking resolve for the paging source factory; list is bounded.
                kotlinx.coroutines.runBlocking {
                    getPlaylistSongs(id)
                }
            }.getOrDefault(emptyList())
            ListPagingSource(songs)
        }.flow
}
