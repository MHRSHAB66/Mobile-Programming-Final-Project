package com.example.project.domain.model

enum class DownloadState { QUEUED, DOWNLOADING, COMPLETED, FAILED }

/** A song plus its offline download status, shown on the Downloads screen. */
data class DownloadItem(
    val song: Song,
    val state: DownloadState,
    val progress: Int = 0,
    val addedAt: Long = 0L,
)

/** Sort options for the Downloads list. */
enum class DownloadSort { RECENT, TITLE, ARTIST }
