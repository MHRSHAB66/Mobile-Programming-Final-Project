package com.example.project.domain.usecase

import com.example.project.domain.model.HomeFeed
import com.example.project.domain.model.PlaylistType
import com.example.project.domain.repository.MusicRepository
import com.example.project.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** Aggregates everything the Home screen needs in one off-main-thread call. */
class GetHomeFeedUseCase(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(): HomeFeed = withContext(Dispatchers.IO) {
        coroutineScope {
            val carousel = async { musicRepository.getTrendingSongs() }
            val popular = async { musicRepository.getMostPopular() }
            val newReleases = async { musicRepository.getNewReleases() }
            val global = async { playlistRepository.getPlaylists(PlaylistType.GLOBAL) }
            val local = async { playlistRepository.getPlaylists(PlaylistType.LOCAL) }
            val artists = async { musicRepository.getArtists() }
            HomeFeed(
                carousel = carousel.await(),
                mostPopular = popular.await(),
                newReleases = newReleases.await(),
                globalPlaylists = global.await(),
                localPlaylists = local.await(),
                topArtists = artists.await(),
            )
        }
    }
}
