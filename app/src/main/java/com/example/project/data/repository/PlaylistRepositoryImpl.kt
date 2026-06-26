package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.project.data.mock.MockData
import com.example.project.data.paging.ListPagingSource
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
) : PlaylistRepository {

    override suspend fun getPlaylists(type: PlaylistType): List<Playlist> =
        withContext(Dispatchers.IO) { MockData.playlists.filter { it.type == type } }

    override suspend fun getAllPlaylists(): List<Playlist> =
        withContext(Dispatchers.IO) { MockData.playlists }

    override suspend fun getPlaylist(id: String): Playlist? =
        withContext(Dispatchers.IO) { MockData.playlistById(id) }

    override suspend fun getPlaylistSongs(id: String): List<Song> = withContext(Dispatchers.IO) {
        val playlist = MockData.playlistById(id) ?: return@withContext emptyList()
        musicRepository.getSongsByIds(playlist.songIds)
    }

    override fun getPlaylistSongsPaged(id: String): Flow<PagingData<Song>> =
        Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            // Build the source lazily; resolution is cheap (in-memory mock).
            val playlist = MockData.playlistById(id)
            val byId = MockData.songs.associateBy { it.id }
            val songs = playlist?.songIds?.mapNotNull { byId[it] } ?: emptyList()
            ListPagingSource(songs)
        }.flow
}
