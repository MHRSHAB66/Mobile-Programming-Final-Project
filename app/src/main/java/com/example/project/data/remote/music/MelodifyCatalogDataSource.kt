package com.example.project.data.remote.music

import com.example.project.data.remote.api.CatalogApi
import com.example.project.data.remote.api.dto.toDomainArtist
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Melodify FastAPI catalogue. On any failure returns empty lists so the UI can show
 * empty states until connectivity returns.
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
