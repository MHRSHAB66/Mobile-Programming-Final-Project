package com.example.project.domain.model

/** Filter chips on the Search screen. */
enum class SearchFilter { SONG, ARTIST, PLAYLIST, USER }

/** Aggregated search results across all entity types. */
data class SearchResults(
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val users: List<User> = emptyList(),
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && artists.isEmpty() && playlists.isEmpty() && users.isEmpty()
}
