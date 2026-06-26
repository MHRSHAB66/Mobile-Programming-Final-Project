package com.example.project.domain.model

/** Aggregated content for the Home screen, produced by GetHomeFeedUseCase. */
data class HomeFeed(
    val carousel: List<Song>,
    val mostPopular: List<Song>,
    val newReleases: List<Song>,
    val globalPlaylists: List<Playlist>,
    val localPlaylists: List<Playlist>,
    val topArtists: List<Artist>,
)
