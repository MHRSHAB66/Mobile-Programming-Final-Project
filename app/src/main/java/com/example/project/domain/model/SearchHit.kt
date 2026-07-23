package com.example.project.domain.model

/** One row in a paged search results list (Paging 3). */
sealed interface SearchHit {
    val id: String

    data class SongHit(val song: Song) : SearchHit {
        override val id: String get() = song.id
    }

    data class ArtistHit(val artist: Artist) : SearchHit {
        override val id: String get() = artist.id
    }

    data class PlaylistHit(val playlist: Playlist) : SearchHit {
        override val id: String get() = playlist.id
    }

    data class UserHit(val user: User) : SearchHit {
        override val id: String get() = user.id
    }
}
