package com.example.project.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Pages an in-memory list. Used so playlist/long lists go through Paging 3 the same way a
 * network- or Room-backed source would, keeping the UI Paging-ready for a real backend.
 */
class ListPagingSource<T : Any>(
    private val items: List<T>,
    private val pageSize: Int = 20,
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 0
        val start = page * pageSize
        if (start >= items.size) {
            return LoadResult.Page(emptyList(), prevKey = if (page == 0) null else page - 1, nextKey = null)
        }
        val end = minOf(start + pageSize, items.size)
        val sublist = items.subList(start, end)
        return LoadResult.Page(
            data = sublist,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (end >= items.size) null else page + 1,
        )
    }
}
