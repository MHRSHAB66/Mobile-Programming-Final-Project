package com.example.project.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.project.data.local.db.DownloadDao
import com.example.project.domain.model.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Performs the actual offline download in the background. Streams the audio URL to local
 * storage, updating progress/status in Room as it goes. Runs entirely off the main thread.
 *
 * If the network is unavailable the work is marked FAILED and the song remains streamable —
 * the repository's smart playback still works via the stream URL.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val downloadDao: DownloadDao by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val url = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()

        downloadDao.updateStatus(songId, DownloadState.DOWNLOADING.name, 0, null)

        val dir = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val outFile = File(dir, "$songId.mp3")

        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                downloadDao.updateStatus(songId, DownloadState.FAILED.name, 0, null)
                return@withContext Result.failure()
            }
            val total = connection.contentLength.toLong()
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var lastReported = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = ((downloaded * 100) / total).toInt()
                            if (pct >= lastReported + 5) {
                                lastReported = pct
                                downloadDao.updateStatus(
                                    songId, DownloadState.DOWNLOADING.name, pct, null
                                )
                            }
                        }
                    }
                }
            }
            downloadDao.updateStatus(songId, DownloadState.COMPLETED.name, 100, outFile.absolutePath)
            Result.success()
        } catch (e: Exception) {
            outFile.delete()
            downloadDao.updateStatus(songId, DownloadState.FAILED.name, 0, null)
            Result.failure()
        }
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_AUDIO_URL = "audio_url"
        fun uniqueName(songId: String) = "download_$songId"
    }
}
