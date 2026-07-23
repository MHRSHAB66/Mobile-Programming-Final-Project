package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.project.data.local.db.CachedPlaylistSongEntity
import com.example.project.data.local.db.CatalogCacheDao
import com.example.project.data.local.db.toCachedEntity
import com.example.project.data.local.db.toPlaylist
import com.example.project.data.local.db.toSong
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.AddPlaylistSongRequestDto
import com.example.project.data.remote.api.dto.CreatePlaylistRequestDto
import com.example.project.data.remote.api.dto.toDomainPlaylist
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.domain.model.Playlist
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.model.Song
import com.example.project.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl(
    private val catalogApi: CatalogApi,
    private val catalogCache: CatalogCacheDao,
) : PlaylistRepository {

    private suspend fun allPlaylists(): List<Playlist> {
        val remote = runCatching { catalogApi.getPlaylists().map { it.toDomainPlaylist() } }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            val now = System.currentTimeMillis()
            catalogCache.upsertPlaylists(remote.map { it.toCachedEntity(now) })
            return remote
        }
        return catalogCache.getAllPlaylists().map { it.toPlaylist() }
    }

    override suspend fun getPlaylists(type: PlaylistType): List<Playlist> =
        withContext(Dispatchers.IO) { allPlaylists().filter { it.type == type } }

    override suspend fun getAllPlaylists(): List<Playlist> =
        withContext(Dispatchers.IO) { allPlaylists() }

    override suspend fun getMyPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val remote = runCatching { catalogApi.getMyPlaylists().map { it.toDomainPlaylist() } }
            .getOrNull()
        if (remote != null) {
            val now = System.currentTimeMillis()
            catalogCache.upsertPlaylists(remote.map { it.toCachedEntity(now) })
            return@withContext remote
        }
        catalogCache.getAllPlaylists()
            .map { it.toPlaylist() }
            .filter { it.type == PlaylistType.USER }
    }

    override suspend fun getPlaylist(id: String): Playlist? = withContext(Dispatchers.IO) {
        val remote = runCatching { catalogApi.getPlaylist(id).toDomainPlaylist() }.getOrNull()
        if (remote != null) {
            catalogCache.upsertPlaylists(listOf(remote.toCachedEntity(System.currentTimeMillis())))
            return@withContext remote
        }
        catalogCache.getPlaylist(id)?.toPlaylist()
            ?: allPlaylists().firstOrNull { it.id == id }
    }

    override suspend fun getPlaylistSongs(id: String): List<Song> = withContext(Dispatchers.IO) {
        val remote = runCatching {
            catalogApi.getPlaylistSongs(id, page = 1, limit = 200).items.map { it.toDomainSong() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            val now = System.currentTimeMillis()
            catalogCache.upsertSongs(remote.map { it.toCachedEntity(now) })
            catalogCache.clearPlaylistSongs(id)
            catalogCache.upsertPlaylistSongs(
                remote.mapIndexed { index, song ->
                    CachedPlaylistSongEntity(playlistId = id, songId = song.id, position = index)
                },
            )
            return@withContext remote
        }
        catalogCache.getPlaylistSongs(id).map { it.toSong() }
    }

    override suspend fun createPlaylist(title: String, isPublic: Boolean): Result<Playlist> =
        withContext(Dispatchers.IO) {
            runCatching {
                val created = catalogApi.createPlaylist(
                    CreatePlaylistRequestDto(title = title.trim(), isPublic = isPublic),
                ).toDomainPlaylist()
                catalogCache.upsertPlaylists(
                    listOf(created.toCachedEntity(System.currentTimeMillis())),
                )
                created
            }
        }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                catalogApi.addSongToPlaylist(
                    playlistId,
                    AddPlaylistSongRequestDto(songId = songId),
                )
                // Refresh tracks so Room paging / detail stay correct.
                getPlaylistSongs(playlistId)
                Unit
            }
        }

    override fun getPlaylistSongsPaged(id: String): Flow<PagingData<Song>> =
        Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { catalogCache.pagingPlaylistSongs(id) },
        ).flow.map { paging -> paging.map { it.toSong() } }
}
