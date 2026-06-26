package com.example.project.domain.usecase

import com.example.project.domain.model.Song
import com.example.project.domain.repository.DownloadRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/** Result of attempting a download, so the UI can show the right message. */
sealed interface DownloadResult {
    data object Started : DownloadResult
    data object NeedsPremium : DownloadResult
    data object AlreadyDownloaded : DownloadResult
}

/**
 * Enforces the premium business rule: only premium users may download for offline use.
 * Normal users get a "needs premium" result that the UI turns into an upgrade prompt.
 */
class DownloadSongUseCase(
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(song: Song): DownloadResult {
        if (downloadRepository.isDownloaded(song.id)) return DownloadResult.AlreadyDownloaded
        val isPremium = settingsRepository.settings.first().isPremium
        if (!isPremium) return DownloadResult.NeedsPremium
        downloadRepository.enqueueDownload(song)
        return DownloadResult.Started
    }
}
