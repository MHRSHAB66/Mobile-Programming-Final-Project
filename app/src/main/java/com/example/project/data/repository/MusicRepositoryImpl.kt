package com.example.project.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.project.data.local.db.CatalogCacheDao
import com.example.project.data.local.db.DownloadDao
import com.example.project.data.local.db.LikedSongDao
import com.example.project.data.local.db.toArtist
import com.example.project.data.local.db.toCachedEntity
import com.example.project.data.local.db.toSong
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
 * Catalogue repository. Fetches from the Melodify API, writes successes into Room, and
 * serves that local cache when the network is unavailable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MusicRepositoryImpl(
    private val dataSource: RemoteMusicDataSource,
    private val catalogApi: CatalogApi,
    private val catalogCache: CatalogCacheDao,
    private val likedDao: LikedSongDao,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository,
) : MusicRepository {

    private suspend fun allSongs(): List<Song> {
        val remote = runCatching { dataSource.getSongs() }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            val now = System.currentTimeMillis()
            catalogCache.upsertSongs(
                remote.mapIndexed { index, song ->
                    song.toCachedEntity(cachedAt = now, popularity = remote.size - index)
                },
            )
            return remote
        }
        return catalogCache.getAllSongs().map { it.toSong() }
    }

    private suspend fun allArtists(): List<Artist> {
        val remote = runCatching { dataSource.getArtists() }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            val now = System.currentTimeMillis()
            catalogCache.upsertArtists(remote.map { it.toCachedEntity(now) })
            return remote
        }
        return catalogCache.getAllArtists().map { it.toArtist() }
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
        val remote = runCatching { catalogApi.getSong(id).toDomainSong() }.getOrNull()
        if (remote != null) {
            catalogCache.upsertSongs(listOf(remote.toCachedEntity(System.currentTimeMillis())))
            return@withContext decorate(listOf(remote)).first()
        }
        catalogCache.getSong(id)?.toSong()?.let { decorate(listOf(it)).first() }
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
            catalogCache.upsertArtists(listOf(fresh.toCachedEntity(System.currentTimeMillis())))
            return@withContext fresh
        }
        catalogCache.getArtist(id)?.toArtist()
            ?: allArtists().firstOrNull { it.id == id }
    }

    override suspend fun getArtistSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        val remote = runCatching {
            catalogApi.getArtistSongs(artistId, page = 1, limit = 200).items.map { it.toDomainSong() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            val now = System.currentTimeMillis()
            catalogCache.upsertSongs(
                remote.mapIndexed { index, song ->
                    song.toCachedEntity(cachedAt = now, popularity = remote.size - index)
                },
            )
            return@withContext decorate(remote)
        }
        decorate(catalogCache.getSongsByArtist(artistId).map { it.toSong() })
    }

    override fun getArtistSongsPaged(artistId: String): Flow<PagingData<Song>> =
        Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { catalogCache.pagingSongsByArtist(artistId) },
        ).flow.map { paging -> paging.map { it.toSong() } }

    override suspend fun updateCachedArtistFollowers(artistId: String, followers: Int) =
        withContext(Dispatchers.IO) {
            catalogCache.updateArtistFollowers(artistId, followers.coerceAtLeast(0))
        }

    override fun observeLibrarySignals(): Flow<Unit> =
        settingsRepository.settings.flatMapLatest { settings ->
            combine(likedDao.observeIds(), downloadDao.observeCompletedIds(settings.currentUserId)) { _, _ -> Unit }
        }.map { }
}
