package com.example.project.data.repository

import com.example.project.data.local.db.DownloadDao
import com.example.project.data.local.db.LikedSongDao
import com.example.project.data.mock.MockData
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
 * (Jamendo when configured, otherwise the mock source) and caches them in memory. If the
 * remote source is empty or fails, it falls back to the bundled mock catalogue so the app
 * always has content. Each song is decorated with liked/download state from Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MusicRepositoryImpl(
    private val dataSource: RemoteMusicDataSource,
    private val likedDao: LikedSongDao,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository,
) : MusicRepository {

    @Volatile
    private var cachedSongs: List<Song>? = null

    @Volatile
    private var cachedArtists: List<Artist>? = null

    private suspend fun allSongs(): List<Song> {
        cachedSongs?.let { return it }
        val loaded = runCatching { dataSource.getSongs() }.getOrNull()
            ?.takeIf { it.isNotEmpty() } ?: MockData.songs
        cachedSongs = loaded
        return loaded
    }

    private suspend fun allArtists(): List<Artist> {
        cachedArtists?.let { return it }
        val loaded = runCatching { dataSource.getArtists() }.getOrNull()
            ?.takeIf { it.isNotEmpty() } ?: MockData.artists
        cachedArtists = loaded
        return loaded
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
        allArtists().firstOrNull { it.id == id }
    }

    override suspend fun getArtistSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        decorate(allSongs().filter { it.artistId == artistId })
    }

    override fun observeLibrarySignals(): Flow<Unit> =
        settingsRepository.settings.flatMapLatest { settings ->
            combine(likedDao.observeIds(), downloadDao.observeCompletedIds(settings.currentUserId)) { _, _ -> Unit }
        }.map { }
}
