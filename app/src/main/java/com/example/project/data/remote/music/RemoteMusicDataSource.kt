package com.example.project.data.remote.music

import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song

/**
 * Abstraction over the Melodify catalogue API. Implementations return [Song] / [Artist]
 * domain models so repositories stay independent of Retrofit DTOs.
 */
interface RemoteMusicDataSource {
    suspend fun getSongs(): List<Song>
    suspend fun getArtists(): List<Artist>
    suspend fun getArtist(id: String): Artist? = getArtists().firstOrNull { it.id == id }
}
