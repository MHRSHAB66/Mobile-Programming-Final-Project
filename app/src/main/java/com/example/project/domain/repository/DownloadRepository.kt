package com.example.project.domain.repository

import com.example.project.domain.model.DownloadItem
import com.example.project.domain.model.DownloadSort
import com.example.project.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(sort: DownloadSort): Flow<List<DownloadItem>>
    fun observeDownloadedIds(): Flow<Set<String>>

    /** Enqueues a background download via WorkManager. Returns false if not permitted (non-premium). */
    suspend fun enqueueDownload(song: Song): Boolean
    suspend fun removeDownload(songId: String)
    suspend fun isDownloaded(songId: String): Boolean
    suspend fun localPathFor(songId: String): String?
}
