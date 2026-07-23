package com.example.project.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.project.data.remote.api.dto.SongPageDto
import com.example.project.data.remote.api.dto.toDomainSong
import com.example.project.domain.model.Song

/**
 * Pages songs from a Melodify `SongPage` endpoint (playlist tracks, artist songs, etc.).
 */
class RemoteSongPagingSource(
    private val loader: suspend (page: Int, limit: Int) -> SongPageDto,
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 1
        val limit = params.loadSize.coerceAtMost(50)
        return try {
            val response = loader(page, limit)
            val items = response.items.map { it.toDomainSong() }
            val loaded = ((response.page - 1) * response.limit) + items.size
            LoadResult.Page(
                data = items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (loaded >= response.total || items.isEmpty()) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
