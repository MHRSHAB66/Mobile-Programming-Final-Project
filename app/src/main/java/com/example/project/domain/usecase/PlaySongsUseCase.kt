package com.example.project.domain.usecase

import com.example.project.domain.model.Song
import com.example.project.domain.player.PlayerController
import com.example.project.domain.repository.DownloadRepository
import com.example.project.domain.repository.LibraryRepository

/**
 * Starts playback of a queue. For each song it resolves whether a downloaded local file
 * exists (play local) or it should stream the URL, then records the chosen track in the
 * recently-played history.
 */
class PlaySongsUseCase(
    private val player: PlayerController,
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(queue: List<Song>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val resolved = queue.map { song ->
            val localPath = downloadRepository.localPathFor(song.id)
            if (localPath != null) song.copy(localPath = localPath) else song
        }
        val safeIndex = startIndex.coerceIn(0, resolved.lastIndex)
        player.play(resolved, safeIndex)
        libraryRepository.addRecentlyPlayed(resolved[safeIndex])
    }
}
