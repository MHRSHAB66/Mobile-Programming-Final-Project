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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)

class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository,
) : DownloadRepository {

    private val workManager get() = WorkManager.getInstance(context)

    private val userIdFlow get() = settingsRepository.settings.map { it.currentUserId }

    override fun observeDownloads(sort: DownloadSort): Flow<List<DownloadItem>> =
        userIdFlow.flatMapLatest { userId ->
            val source = when (sort) {
                DownloadSort.RECENT -> downloadDao.observeAllByRecent(userId)
                DownloadSort.TITLE -> downloadDao.observeAllByTitle(userId)
                DownloadSort.ARTIST -> downloadDao.observeAllByArtist(userId)
            }
            source.map { list -> list.map { it.toDownloadItem() } }
        }

    override fun observeDownloadedIds(): Flow<Set<String>> =
        userIdFlow.flatMapLatest { userId ->
            downloadDao.observeCompletedIds(userId).map { it.toSet() }
        }

    override suspend fun enqueueDownload(song: Song): Boolean {
        val settings = settingsRepository.settings.first()
        if (!settings.isPremium) return false
        val userId = settings.currentUserId

        downloadDao.upsert(
            song.toDownloadEntity(
                userId = userId,
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
                    DownloadWorker.KEY_USER_ID to userId,
                    DownloadWorker.KEY_AUDIO_URL to song.audioUrl,
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.uniqueName(userId, song.id),
            ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }

    override suspend fun removeDownload(songId: String) {
        val userId = settingsRepository.settings.first().currentUserId
        workManager.cancelUniqueWork(DownloadWorker.uniqueName(userId, songId))
        downloadDao.localPath(songId, userId)?.let { path -> runCatching { File(path).delete() } }
        downloadDao.delete(songId, userId)
    }

    override suspend fun isDownloaded(songId: String): Boolean {
        val userId = settingsRepository.settings.first().currentUserId
        return downloadDao.exists(songId, userId)
    }

    override suspend fun localPathFor(songId: String): String? {
        val userId = settingsRepository.settings.first().currentUserId
        return downloadDao.localPath(songId, userId)
    }
}
