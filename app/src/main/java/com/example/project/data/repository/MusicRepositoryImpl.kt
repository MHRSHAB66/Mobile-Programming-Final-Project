package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.project.data.local.db.DownloadDao
import com.example.project.data.local.db.LikedSongDao
import com.example.project.data.mock.MockData
import com.example.project.data.paging.RemoteSongPagingSource
import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.data.remote.music.RemoteMusicDataSource
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Catalogue repository. Pulls songs/artists from the injected [RemoteMusicDataSource]
 * and caches successful remote results in memory. If the remote source is empty or fails,
 * falls back to the bundled mock catalogue so the UI still has content — but that mock
 * fallback is **not** treated as a permanent cache, so the next call retries the backend
 * once connectivity returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MusicRepositoryImpl(
    private val dataSource: RemoteMusicDataSource,
    private val catalogApi: CatalogApi,
    private val likedDao: LikedSongDao,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository,
) : MusicRepository {

    @Volatile
    private var cachedSongs: List<Song>? = null

    /** True only when [cachedSongs] came from a successful remote fetch. */
    @Volatile
    private var songsFromRemote: Boolean = false

    @Volatile
    private var cachedArtists: List<Artist>? = null

    @Volatile
    private var artistsFromRemote: Boolean = false

    private suspend fun allSongs(): List<Song> {
        if (songsFromRemote) {
            cachedSongs?.let { return it }
        }

        val remote = runCatching { dataSource.getSongs() }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            cachedSongs = remote
            songsFromRemote = true
            return remote
        }

        // Keep a previous remote catalogue if we already had one (transient blip).
        if (songsFromRemote) {
            cachedSongs?.let { return it }
        }

        // Temporary offline fallback — next call will try the backend again.
        val fallback = cachedSongs ?: MockData.songs
        cachedSongs = fallback
        songsFromRemote = false
        return fallback
    }

    private suspend fun allArtists(): List<Artist> {
        if (artistsFromRemote) {
            cachedArtists?.let { return it }
        }

        val remote = runCatching { dataSource.getArtists() }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            cachedArtists = remote
            artistsFromRemote = true
            return remote
        }

        if (artistsFromRemote) {
            cachedArtists?.let { return it }
        }

        val fallback = cachedArtists ?: MockData.artists
        cachedArtists = fallback
        artistsFromRemote = false
        return fallback
    }

    private suspend fun decorate(songs: List<Song>): List<Song> {
        val userId = settingsRepository.settings.first().currentUserId
        val likedIds = likedDao.observeIds().first().toSet()
        val downloadedIds = downloadDao.observeCompletedIds(userId).first().toSet()
        return songs.map { song ->
            song.copy(
                isLiked = song.id in likedIds,
                localPath = if (song.id in downloadedIds) {
                    downloadDao.localPath(song.id, userId) ?: song.localPath
                } else song.localPath,
            )
        }
    }

    override suspend fun getTrendingSongs(): List<Song> = withContext(Dispatchers.IO) {
        decorate(allSongs().shuffled().take(8))
    }

    override suspend fun getMostPopular(): List<Song> = withContext(Dispatchers.IO) {
        decorate(allSongs().take(15))
    }

    override suspend fun getNewReleases(): List<Song> = withContext(Dispatchers.IO) {
        decorate(allSongs().takeLast(15).reversed())
    }

    override suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        decorate(allSongs())
    }

    override suspend fun getSong(id: String): Song? = withContext(Dispatchers.IO) {
        allSongs().firstOrNull { it.id == id }?.let { decorate(listOf(it)).first() }
    }

    override suspend fun getSongsByIds(ids: List<String>): List<Song> = withContext(Dispatchers.IO) {
        val byId = allSongs().associateBy { it.id }
        decorate(ids.mapNotNull { byId[it] })
    }

    override suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        allArtists().sortedByDescending { it.followers }
    }

    override suspend fun getArtist(id: String): Artist? = withContext(Dispatchers.IO) {
        val fresh = runCatching { dataSource.getArtist(id) }.getOrNull()
        if (fresh != null) {
            cachedArtists = when {
                cachedArtists == null -> listOf(fresh)
                cachedArtists!!.any { it.id == id } ->
                    cachedArtists!!.map { if (it.id == id) fresh else it }
                else -> cachedArtists!! + fresh
            }
            artistsFromRemote = true
            return@withContext fresh
        }
        allArtists().firstOrNull { it.id == id }
    }

    override suspend fun getArtistSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        val remote = runCatching {
            catalogApi.getArtistSongs(artistId, page = 1, limit = 200).items.map { it.toDomainSong() }
        }.getOrNull()
        if (!remote.isNullOrEmpty()) return@withContext decorate(remote)
        decorate(allSongs().filter { it.artistId == artistId })
    }

    override fun getArtistSongsPaged(artistId: String): Flow<PagingData<Song>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false, initialLoadSize = 20),
            pagingSourceFactory = {
                RemoteSongPagingSource { page, limit ->
                    catalogApi.getArtistSongs(artistId, page = page, limit = limit)
                }
            },
        ).flow

    override fun observeLibrarySignals(): Flow<Unit> =
        settingsRepository.settings.flatMapLatest { settings ->
            combine(likedDao.observeIds(), downloadDao.observeCompletedIds(settings.currentUserId)) { _, _ -> Unit }
        }.map { }
}
