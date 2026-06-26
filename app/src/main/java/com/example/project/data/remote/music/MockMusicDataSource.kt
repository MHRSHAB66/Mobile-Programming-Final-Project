package com.example.project.data.remote.music

import com.example.project.data.mock.MockData
import com.example.project.domain.model.Artist
import com.example.project.domain.model.Song

/**
 * Default data source: the in-memory mock catalogue. Audio comes from verified, stable,
 * royalty-free sample MP3s (see [MockData]), so playback works reliably out of the box with no
 * API key. Also used as the safe fallback when a remote source is unavailable.
 */
class MockMusicDataSource : RemoteMusicDataSource {
    override suspend fun getSongs(): List<Song> = MockData.songs
    override suspend fun getArtists(): List<Artist> = MockData.artists
}
