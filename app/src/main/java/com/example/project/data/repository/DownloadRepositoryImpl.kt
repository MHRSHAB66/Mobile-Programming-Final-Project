package com.example.project.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.project.data.download.DownloadWorker
import com.example.project.data.local.db.DownloadDao
import com.example.project.data.local.db.toDownloadEntity
import com.example.project.data.local.db.toDownloadItem
import com.example.project.domain.model.DownloadItem
import com.example.project.domain.model.DownloadSort
import com.example.project.domain.model.DownloadState
import com.example.project.domain.model.Song
import com.example.project.domain.repository.DownloadRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository,
) : DownloadRepository {

    private val workManager get() = WorkManager.getInstance(context)

    override fun observeDownloads(sort: DownloadSort): Flow<List<DownloadItem>> {
        val source = when (sort) {
            DownloadSort.RECENT -> downloadDao.observeAllByRecent()
            DownloadSort.TITLE -> downloadDao.observeAllByTitle()
            DownloadSort.ARTIST -> downloadDao.observeAllByArtist()
        }
        return source.map { list -> list.map { it.toDownloadItem() } }
    }

    override fun observeDownloadedIds(): Flow<Set<String>> =
        downloadDao.observeCompletedIds().map { it.toSet() }

    override suspend fun enqueueDownload(song: Song): Boolean {
        // Enforce the premium business rule at the data boundary as well.
        if (!settingsRepository.settings.first().isPremium) return false

        downloadDao.upsert(
            song.toDownloadEntity(
                state = DownloadState.QUEUED,
                progress = 0,
                localPath = null,
                addedAt = System.currentTimeMillis(),
            )
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_SONG_ID to song.id,
                    DownloadWorker.KEY_AUDIO_URL to song.audioUrl,
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.uniqueName(song.id),
            ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }

    override suspend fun removeDownload(songId: String) {
        workManager.cancelUniqueWork(DownloadWorker.uniqueName(songId))
        downloadDao.localPath(songId)?.let { path -> runCatching { File(path).delete() } }
        downloadDao.delete(songId)
    }

    override suspend fun isDownloaded(songId: String): Boolean = downloadDao.exists(songId)

    override suspend fun localPathFor(songId: String): String? = downloadDao.localPath(songId)
}
