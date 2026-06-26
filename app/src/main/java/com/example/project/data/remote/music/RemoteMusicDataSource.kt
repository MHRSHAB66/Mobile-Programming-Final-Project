package com.example.project.data.remote.music

import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song

/**
 * Abstraction over where the catalogue comes from. Implementations return the same [Song]/
 * [Artist] domain models, so the repositories (and the whole app) are independent of the
 * backend. Today this is either [JamendoMusicDataSource] (real Creative-Commons music, when a
 * client id is configured) or [MockMusicDataSource] (in-memory catalogue with verified,
 * stable, royalty-free sample audio).
 */
interface RemoteMusicDataSource {
    suspend fun getSongs(): List<Song>
    suspend fun getArtists(): List<Artist>
}
