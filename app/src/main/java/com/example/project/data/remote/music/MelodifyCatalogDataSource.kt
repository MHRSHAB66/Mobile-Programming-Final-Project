package com.example.project.data.remote.music

import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.toDomainArtist
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Melodify FastAPI catalogue. On any failure returns empty lists so
 * [com.example.project.data.repository.MusicRepositoryImpl] can fall back to mock data.
 */
class MelodifyCatalogDataSource(
    private val catalogApi: CatalogApi,
) : RemoteMusicDataSource {

    override suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        runCatching {
            catalogApi.getSongs(page = 1, limit = 200).items.map { it.toDomainSong() }
        }.getOrDefault(emptyList())
    }

    override suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        runCatching {
            catalogApi.getArtists(page = 1, limit = 200).items.map { it.toDomainArtist() }
        }.getOrDefault(emptyList())
    }

    override suspend fun getArtist(id: String): Artist? = withContext(Dispatchers.IO) {
        runCatching { catalogApi.getArtist(id).toDomainArtist() }.getOrNull()
    }
}
