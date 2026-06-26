package com.example.project.domain.usecase

import com.example.project.domain.model.Song
import com.example.project.domain.repository.LibraryRepository

/** Toggles the liked state of a song. Returns true if the song is now liked. */
class ToggleLikeUseCase(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(song: Song): Boolean = libraryRepository.toggleLike(song)
}
